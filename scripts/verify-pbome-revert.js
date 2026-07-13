// (9. tur M2) pbomeSnapshotDiff + pbomeRevert sanity testi (sunucusuz).
// "Kaydetmeden Kapat" diff bazlı geri alma: eklenen silinir, silinen idMap'le
// geri eklenir (parent remap), değişen eski değere döner (temizleme semantiği:
// material/tür/form→'', ölçü→0, dept/parent→explicit null). Sıra kritik:
// kod ön-geçişi → eklenen sil (yaşayan çocuk köke) → silinen ekle → patch.
// Kullanım: node scripts/verify-pbome-revert.js
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
const chk=(n,c,info)=>{ console.log((c?'  ✅':'  ❌ FAIL')+' '+n+(!c&&info!==undefined?'  — '+JSON.stringify(info):'')); if(!c)fail++; };

global.DIM_FIELDS = ['width_mm','height_mm','thickness_mm','length_mm','diameter_mm'];
eval(grab('pbomeSnapshotDiff'));
eval(grab('pbomeRevert'));

// ── op-kaydedici db shim ──
let ops = [];        // {op, table, id, body} — sıra doğrulaması için
let db = [];         // canlı satırlar
let seq = 1;
let failOn = ()=>false; // hata enjeksiyonu
global.dbUpdate = async (t,id,body)=>{ ops.push({op:'update', id, body});
  if(failOn('update',id)) return false;
  const r=db.find(x=>x.id===id); if(r) Object.assign(r,body); return true; };
global.dbDelete = async (t,id)=>{ ops.push({op:'delete', id});
  if(failOn('delete',id)) return false;
  db = db.filter(x=>x.id!==id); global._pbomeParts = db; return true; };
global.dbInsert = async (t,body)=>{ ops.push({op:'insert', body});
  if(failOn('insert')) return null;
  const row={id:'new'+(seq++), ...body}; db.push(row); return [row]; };

const row = (id, over={}) => ({
  id, project_bom_id:'pb1', bom_part_id:null, is_excluded:false,
  custom_name:'Parça '+id, custom_code:'K-'+id, custom_qty:1, custom_unit:'adet',
  custom_weight:null, custom_material:null, material_kind:null, material_form:null,
  custom_width_mm:null, custom_height_mm:null, custom_thickness_mm:null,
  custom_length_mm:null, custom_diameter_mm:null,
  operations:[], dept_id:null, parent_custom_id:null, level:0, sort_order:0, ...over });

async function main(){

console.log('═══ pbomeSnapshotDiff ═══');
const s1 = [row('a'), row('b',{parent_custom_id:'a', level:1})];
chk('değişiklik yokken diff boş', (()=>{ const d=pbomeSnapshotDiff(s1, structuredClone(s1));
  return !d.added.length && !d.deleted.length && !d.changed.length; })());
chk('null≈undefined≈"" normalize edilir (yalancı fark yok)', (()=>{
  const a=[row('a',{custom_material:null, dept_id:undefined})];
  const b=[row('a',{custom_material:'', dept_id:null})];
  return pbomeSnapshotDiff(a,b).changed.length===0; })());
chk('sayısal alan string/number farkı fark sayılmaz ("2" vs 2)', (()=>{
  const a=[row('a',{custom_qty:2})], b=[row('a',{custom_qty:'2'})];
  return pbomeSnapshotDiff(a,b).changed.length===0; })());
chk('qty + dept değişimi yakalanır', (()=>{
  const d = pbomeSnapshotDiff([row('a')],[row('a',{custom_qty:5, dept_id:'d9'})]);
  return d.changed.length===1 && d.changed[0].fields.includes('custom_qty') && d.changed[0].fields.includes('dept_id'); })());
chk('operations değişimi yakalanır (dizi vs JSON string eşdeğer)', (()=>{
  const opA=[{code:'WLD',name:'Kaynak'}];
  const same = pbomeSnapshotDiff([row('a',{operations:opA})],[row('a',{operations:JSON.stringify(opA)})]);
  const diff = pbomeSnapshotDiff([row('a',{operations:opA})],[row('a',{operations:[]})]);
  return same.changed.length===0 && diff.changed.length===1; })());
chk('ekleme + silme yakalanır', (()=>{
  const d = pbomeSnapshotDiff([row('a')],[row('b')]);
  return d.deleted.length===1 && d.deleted[0].id==='a' && d.added.length===1 && d.added[0].id==='b'; })());

console.log('\n═══ pbomeRevert: eklenenler silinir ═══');
{
  ops=[]; db=[row('a'), row('x1',{level:0}), row('x2',{parent_custom_id:'x1', level:1})];
  // 'a' yaşıyor ama oturumda x1'in altına taşınmış olsun → önce köke alınmalı
  db[0].parent_custom_id='x1'; db[0].level=1;
  global._pbomeParts = db;
  const snap = [row('a')]; // x1/x2 snapshot'ta yok = eklendi; a'nın parent'ı değişti
  const diff = pbomeSnapshotDiff(snap, db);
  const f = await pbomeRevert(diff);
  chk('hata yok', f===0, f);
  chk('eklenenler silindi, yaşayan kaldı', db.length===1 && db[0].id==='a', db.map(x=>x.id));
  const delOps = ops.filter(o=>o.op==='delete').map(o=>o.id);
  chk('yapraktan köke silindi (x2 önce)', delOps.join(',')==='x2,x1', delOps);
  const reparent = ops.find(o=>o.op==='update' && o.id==='a' && o.body.parent_custom_id===null);
  const silmeIdx = ops.findIndex(o=>o.op==='delete');
  chk('yaşayan çocuk silmeden ÖNCE köke alındı', !!reparent && ops.indexOf(reparent) < silmeIdx);
  chk('a en sonunda eski parent\'ına (null/kök) döndü', db[0].parent_custom_id===null && db[0].level===0);
}

console.log('\n═══ pbomeRevert: silinenler idMap ile geri eklenir ═══');
{
  ops=[]; db=[row('a')]; global._pbomeParts = db;
  const snap = [row('a'), row('p',{level:0}), row('c',{parent_custom_id:'p', level:1})];
  const diff = pbomeSnapshotDiff(snap, db);
  const f = await pbomeRevert(diff);
  chk('hata yok', f===0, f);
  chk('iki satır geri eklendi', db.length===3, db.length);
  const yeniP = db.find(x=>x.custom_code==='K-p'), yeniC = db.find(x=>x.custom_code==='K-c');
  chk('kökten yaprağa eklendi (p önce)', ops.filter(o=>o.op==='insert')[0].body.custom_code==='K-p');
  chk('çocuğun parent\'ı YENİ id\'ye remap edildi', yeniC && yeniP && yeniC.parent_custom_id===yeniP.id,
    yeniC && yeniC.parent_custom_id);
}

console.log('\n═══ pbomeRevert: değişenler eski değere döner (temizleme semantiği) ═══');
{
  ops=[]; db=[row('a',{custom_qty:9, custom_material:'Yeni Malzeme', material_kind:'TEDARIK',
    custom_width_mm:250, dept_id:'d9', custom_name:'Yeni Ad', custom_code:'K-YENI'})];
  global._pbomeParts = db;
  const snap = [row('a')]; // eskiden hepsi boş/1 idi
  const diff = pbomeSnapshotDiff(snap, db);
  const f = await pbomeRevert(diff);
  chk('hata yok', f===0, f);
  const patch = ops.filter(o=>o.op==='update' && o.id==='a').pop().body;
  chk('qty/ad/kod eski değere döndü', patch.custom_qty===1 && patch.custom_name==='Parça a' && patch.custom_code==='K-a');
  chk('malzeme/tür boş string ile TEMİZLENDİ (null "dokunma" demek)',
    patch.custom_material==='' && patch.material_kind==='');
  chk('ölçü 0 ile temizlendi', patch.custom_width_mm===0);
  chk('dept_id explicit null', 'dept_id' in patch && patch.dept_id===null);
  chk('weight patch\'e GİRMEDİ (null\'a döndürülemez)', !('custom_weight' in patch));
  // kod ön-geçişi: değişen kod önce eski koda döner (yeniden eklenecek satırın
  // kod çakışması guard'ına takılmaması için) — ilk update SADECE kod içermeli
  const ilk = ops.find(o=>o.op==='update');
  chk('kod ön-geçişi ilk işlem', ilk && ilk.body.custom_code==='K-a' && Object.keys(ilk.body).length===1);
}

console.log('\n═══ pbomeRevert: hata enjeksiyonu (best-effort) ═══');
{
  ops=[]; db=[row('a',{custom_qty:9}), row('x1')]; global._pbomeParts = db;
  failOn = (op)=>op==='delete'; // silme hep başarısız
  const diff = pbomeSnapshotDiff([row('a')], db);
  const f = await pbomeRevert(diff);
  failOn = ()=>false;
  chk('başarısız işlem sayıldı ama devam edildi', f===1 && db.find(x=>x.id==='a').custom_qty===1, f);
}

console.log(fail?`\n${fail} HATA ❌`:'\nPBOME REVERT SANITY GEÇTİ ✅');
process.exit(fail?1:0);
}
main();
