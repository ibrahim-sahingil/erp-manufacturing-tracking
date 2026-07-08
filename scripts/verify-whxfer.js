// whXferConfirm yeniden yazımının sanity testi: loose / tam / kısmi aktarım
// yolları in-memory db shim'iyle koşulur (sunucu gerekmez).
const fs = require('fs');
const path = require('path');
const html = fs.readFileSync(path.join('src','main','resources','static','index.html'),'utf8');
function grab(name){
  const marker='function '+name; const start=html.indexOf(marker);
  if(start<0) throw new Error(name+' bulunamadı');
  let i=html.indexOf('(',start),pd=1;i++;
  while(pd>0){const c=html[i];if(c==='(')pd++;if(c===')')pd--;i++;}
  i=html.indexOf('{',i);
  let d=1;i++;
  while(d>0){const c=html[i];if(c==='{')d++;if(c==='}')d--;i++;}
  const asyncPrefix = html.slice(Math.max(0,start-6),start)==='async ' ? 'async ' : '';
  return asyncPrefix + html.slice(start,i);
}

let fail=0;
const chk=(n,c)=>{ console.log((c?'  ✅':'  ❌ FAIL')+' '+n); if(!c)fail++; };

// ── shim ──
let seq=1;
const db = { purchase_items:[], warehouse_movements:[] };
global.dbInsert = async (t,body)=>{ const row={id:'row'+(seq++),...body}; db[t].push(row); return [row]; };
global.dbUpdate = async (t,id,body)=>{ const r=db[t].find(x=>x.id===id); if(!r) return false; Object.assign(r,body); return true; };
global.dbDelete = async (t,id)=>{ const i=db[t].findIndex(x=>x.id===id); if(i>-1) db[t].splice(i,1); return true; };
global.currentUser={display_name:'Test'};
global.toast=(m)=>{ global._lastToast=m; };
global.whName=id=>({w1:'Depo1',w2:'Depo2'}[id]||null);
global.purchaseOrders=[];
global.whMovements=[];
global.renderWarehouseList=()=>{};
const vals={};
global.document={ getElementById:id=>({ value: vals[id], remove(){}, }), };

eval(grab('mvInsert'));
eval(grab('splitPurchaseItem'));
eval(grab('whXferConfirm'));

async function main(){
  // 1) LOOSE aktarım: OUT + IN yazılır, whMovements'a eklenir
  vals['whx-target']='w2'; vals['whx-qty']='3';
  await whXferConfirm({kind:'loose', src:'w1', name:'Sac', code:'S1', maxQ:10, unit:'kg'});
  console.log('loose aktarım:');
  chk('OUT+IN hareket çifti yazıldı', db.warehouse_movements.length===2
    && db.warehouse_movements[0].movement_type==='OUT' && db.warehouse_movements[1].movement_type==='IN');
  chk('whMovements yerelde güncellendi (IN başta, OUT ikinci)', whMovements.length===2
    && whMovements[0].movement_type==='IN' && whMovements[1].movement_type==='OUT');
  chk('kaynak/hedef depolar doğru', db.warehouse_movements[0].warehouse_id==='w1'
    && db.warehouse_movements[1].warehouse_id==='w2');

  // 2) TAM aktarım: kalem hedef depoya taşınır
  db.warehouse_movements.length=0; whMovements.length=0;
  global.purchaseItems=[{id:'pi1', project_name:'P', name:'Mal', code:'M1', quantity:5,
    unit:'adet', status:'IN_WAREHOUSE', warehouse_id:'w1', received_qty:5}];
  db.purchase_items=[...purchaseItems];
  vals['whx-qty']='5';
  await whXferConfirm({kind:'item', id:'pi1', src:'w1', maxQ:5, unit:'adet'});
  console.log('tam aktarım:');
  chk('kalem hedef depoya taşındı', purchaseItems[0].warehouse_id==='w2');
  chk('bölme olmadı (tek kalem)', purchaseItems.length===1 && db.purchase_items.length===1);
  chk('OUT+IN yazıldı ve whMovements güncellendi', db.warehouse_movements.length===2 && whMovements.length===2);

  // 3) KISMİ aktarım: kalem bölünür, yeni kalem hedef depoda
  db.warehouse_movements.length=0; whMovements.length=0;
  global.purchaseItems=[{id:'pi2', project_name:'P', name:'Mal2', code:'M2', quantity:10,
    unit:'adet', status:'IN_WAREHOUSE', warehouse_id:'w1', received_qty:10,
    received_by:'X', received_at:'2026-01-01T10:00:00', ordered_at:null}];
  db.purchase_items=[...purchaseItems];
  vals['whx-qty']='4';
  await whXferConfirm({kind:'item', id:'pi2', src:'w1', maxQ:10, unit:'adet'});
  console.log('kısmi aktarım:');
  const orig=purchaseItems.find(p=>p.id==='pi2'), ni=purchaseItems.find(p=>p.id!=='pi2');
  chk('orijinal adet + kabul sayacı düştü (6)', orig && Number(orig.quantity)===6 && Number(orig.received_qty)===6);
  chk('yeni kalem hedef depoda IN_WAREHOUSE (4)', ni && ni.warehouse_id==='w2'
    && ni.status==='IN_WAREHOUSE' && Number(ni.quantity)===4 && Number(ni.received_qty)===4);
  chk('received_at orijinalden kopyalandı (B4)', ni && ni.received_at==='2026-01-01T10:00:00');
  chk('OUT orijinal kaleme, IN yeni kaleme bağlı', db.warehouse_movements[0].purchase_item_id==='pi2'
    && db.warehouse_movements[1].purchase_item_id===ni.id);

  // 4) KISMİ aktarımda bağlama hatası → tam rollback
  db.warehouse_movements.length=0; whMovements.length=0;
  global.purchaseItems=[{id:'pi3', project_name:'P', name:'Mal3', code:'M3', quantity:10,
    unit:'adet', status:'IN_WAREHOUSE', warehouse_id:'w1', received_qty:10}];
  db.purchase_items=[...purchaseItems];
  const realUpdate = global.dbUpdate;
  global.dbUpdate = async (t,id,body)=>{ if(body.warehouse_id==='w2' && id!=='pi3') return false; return realUpdate(t,id,body); };
  vals['whx-qty']='4';
  await whXferConfirm({kind:'item', id:'pi3', src:'w1', maxQ:10, unit:'adet'});
  global.dbUpdate = realUpdate;
  console.log('kısmi aktarım bağlama hatası (B6 rollback):');
  const orig3=purchaseItems.find(p=>p.id==='pi3');
  chk('orijinal adet geri yüklendi (10)', Number(orig3.quantity)===10 && Number(orig3.received_qty)===10);
  chk('yeni kalem DB\'den silindi', db.purchase_items.length===1);
  chk('hareket yazılmadı', db.warehouse_movements.length===0);
  chk('kullanıcıya hata gösterildi', /başarısız/.test(global._lastToast));

  console.log(fail?`\n${fail} HATA ❌`:'\nWHXFER SANITY GEÇTİ ✅');
  process.exit(fail?1:0);
}
main();
