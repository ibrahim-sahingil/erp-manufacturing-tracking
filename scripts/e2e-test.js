// ═══ E2E TESTİ — 3. tur özellikleri, GERÇEK frontend fonksiyonlarıyla ═══
// index.html'den pbomPublishParts / woWaitingChildren vb. çıkarılır ve canlı
// backend'e karşı izole bir test projesinde koşulur; sonunda tam temizlik.
// Kullanım: node scripts/e2e-test.js <token>  (sunucu localhost:8080 çalışırken)
const fs = require('fs');
const TOK = process.argv[2];
const BASE = 'http://localhost:8080/api/';
const html = fs.readFileSync(require('path').join(__dirname,'..','src','main','resources','static','index.html'),'utf8');

let failures = 0;
function check(name, cond, detail){
  console.log((cond?'  ✅':'  ❌ FAIL')+' '+name+(detail!==undefined?` (${detail})`:''));
  if(!cond) failures++;
}

// ── Gerçek fonksiyonları çıkar ──
function grab(name){
  const marker = (name.startsWith('const')?'':'function ')+name;
  const start = html.indexOf(marker);
  if(start<0) throw new Error(name+' bulunamadı');
  if(name.startsWith('const')) return html.slice(start, html.indexOf(';', start)+1);
  let i = html.indexOf('{', start), depth = 1; i++;
  while(depth>0){ const c=html[i]; if(c==='{')depth++; if(c==='}')depth--; i++; }
  const asyncPrefix = html.slice(Math.max(0,start-6), start)==='async ' ? 'async ' : '';
  return asyncPrefix + html.slice(start, i);
}
// Sabitler + fonksiyonlar. DİKKAT: indirect eval'de const global olmaz —
// globalThis atamasına çevrilir; function bildirimleri sloppy'de global olur.
(0,eval)(grab('const MATERIAL_KINDS').replace('const MATERIAL_KINDS =','globalThis.MATERIAL_KINDS ='));
(0,eval)(grab('const PURCHASE_KINDS').replace('const PURCHASE_KINDS =','globalThis.PURCHASE_KINDS ='));
(0,eval)(grab('pbomPublishParts'));
(0,eval)(grab('pbomPublishMsg'));
(0,eval)(grab('partWaitingChildren'));
(0,eval)(grab('woWaitingChildren'));
(0,eval)(grab('woStartBlockMsg'));

// ── Shims (frontend adapter'ın minimal karşılığı) ──
globalThis.currentUser = {display_name:'E2E Test'};
globalThis._lastApiError = null;
globalThis.parts = [];
globalThis.purchaseItems = [];
globalThis.workOrderParts = [];
let orderId = null, PROJ = 'E2E-TEST-'+Date.now().toString(36);

async function api(method, path, body){
  const r = await fetch(BASE+path.replace(/^\//,''), {
    method, headers:{Authorization:'Bearer '+TOK, 'Content-Type':'application/json'},
    body: body?JSON.stringify(body):undefined
  });
  const j = await r.json().catch(()=>null);
  if(!j || j.success===false){ globalThis._lastApiError = j?j.message:'HTTP hata'; return null; }
  return j.data;
}
function partFromApi(r){ return {...r, project: r.order_id===orderId?PROJ:r._proj, qty:r.total_qty, drawing:r.drawing_no}; }
function partToApi(b){
  const o = {...b};
  if('drawing' in o){ o.drawing_no=o.drawing; delete o.drawing; }
  if('qty' in o){ o.total_qty=o.qty; delete o.qty; }
  if(o.project){ o.order_id=orderId; }
  if(o.notes && !o.description) o.description=o.notes;
  delete o.project; delete o.department; delete o.notes;
  return o;
}
const EP = {parts:'parts', purchase_items:'purchase-items', project_bom:'project-bom',
  project_bom_parts:'project-bom-parts', bom_products:'bom-products', bom_parts:'bom-parts',
  work_orders:'work-orders', work_order_parts:'work-order-parts', orders:'orders',
  warehouses:'warehouses', warehouse_movements:'warehouse-movements'};
globalThis.dbGet = async (t,q)=>{
  let d = await api('GET','/'+EP[t]);
  if(!Array.isArray(d)) return [];
  if(t==='parts') d = d.map(partFromApi);
  return d;
};
globalThis.dbInsert = async (t,b)=>{
  const body = t==='parts'?partToApi(b):b;
  const d = await api('POST','/'+EP[t], body);
  if(!d) return [];
  return [t==='parts'?partFromApi(d):d];
};
globalThis.dbUpdate = async (t,id,b)=>{
  const body = t==='parts'?partToApi(b):b;
  return !!(await api('PUT','/'+EP[t]+'/'+id, body));
};
globalThis.dbDelete = async (t,id)=> !!(await api('DELETE','/'+EP[t]+'/'+id));
globalThis.genId = ()=>Date.now().toString(36)+Math.random().toString(36).slice(2,6);

(async()=>{
  const created = {parts:[], pi:[], wo:[], wop:[], mv:[], wh:[]};
  try {
    console.log('═══ KURULUM ═══ proje:', PROJ);
    const order = await api('POST','/orders',{project_name:PROJ, customer_name:'E2E Müşteri'});
    check('sipariş oluştu', !!order); orderId = order.id;

    const prod = await api('POST','/bom-products',{name:'E2E Ürün', code:'E2E-PRD-'+Date.now().toString(36)});
    check('bom ürünü oluştu', !!prod);

    // Ağaç: GVD(MAMUL) → PLT(türsüz), CVT(TEDARIK); PLT → SAC(HAMMADDE)
    const mkBp = (name,code,kind,parent,extra)=>api('POST','/bom-parts',{
      product_id:prod.id, parent_id:parent||null, name, code,
      quantity:1, material_kind:kind, ...(extra||{})});
    const gvd = await mkBp('E2E Gövde','E2E-GVD','MAMUL',null);
    const plt = await mkBp('E2E Platin','E2E-PLT',null,gvd.id);
    const sac = await mkBp('E2E Sac','E2E-SAC','HAMMADDE',plt.id,{width_mm:300,height_mm:500,thickness_mm:3});
    const cvt = await mkBp('E2E Civata','E2E-CVT','TEDARIK',gvd.id,{quantity:8});
    check('4 bom parçası oluştu', gvd&&plt&&sac&&cvt);

    const pbm = await api('POST','/project-bom',{project_name:PROJ, bom_product_id:prod.id, status:'draft', created_by:'E2E'});
    check('project_bom bağlantısı oluştu', !!pbm);

    // pbp'ler: project_bom CREATE sirasinda backend sablondan OTOMATIK kopyalar
    // (autoPopulateBomParts) — elle kopya YAPILMAZ (UI'daki hasCopied kontrolü gibi)
    const codeOf = p => p.custom_code || p.resolved_code;
    const pbps = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm.id);
    check('4 pbp otomatik kopyalandı', pbps.length===4,
      pbps.length + ' → ' + pbps.map(codeOf).join(','));
    check('pbp resolved_material_kind şablondan geliyor',
      pbps.find(p=>codeOf(p)==='E2E-SAC')?.resolved_material_kind==='HAMMADDE');

    console.log('═══ F+H: YAYINLA (gerçek pbomPublishParts) ═══');
    globalThis.parts = await dbGet('parts');
    const tpl = (await dbGet('bom_parts')).filter(b=>b.product_id===prod.id);
    const r1 = await pbomPublishParts({project_name:PROJ}, pbps, tpl);
    console.log('  sonuç:', JSON.stringify(r1), '→', pbomPublishMsg(r1));
    check('2 parça üretime (GVD, PLT)', r1.added===2, r1.added);
    check('2 kalem satın almaya (SAC, CVT)', r1.purAdded===2, r1.purAdded);
    check('1 hiyerarşi bağı (PLT→GVD)', r1.linked===1, r1.linked);

    const partGvd = parts.find(p=>p.project===PROJ && p.code==='E2E-GVD');
    const partPlt = parts.find(p=>p.project===PROJ && p.code==='E2E-PLT');
    created.parts.push(partGvd?.id, partPlt?.id);
    check('PLT.parent_part_id === GVD', partPlt?.parent_part_id===partGvd?.id);
    const piSac = purchaseItems.find(i=>i.project_name===PROJ && i.code==='E2E-SAC');
    const piCvt = purchaseItems.find(i=>i.project_name===PROJ && i.code==='E2E-CVT');
    created.pi.push(piSac?.id, piCvt?.id);
    check('SAC kalemi pbp bağlı + adet 1', !!piSac?.project_bom_part_id && Number(piSac?.quantity)===1);
    check('CVT kalemi adet 8', Number(piCvt?.quantity)===8, piCvt?.quantity);
    check('SAC/CVT parts\'a GİRMEDİ', !parts.some(p=>p.project===PROJ&&['E2E-SAC','E2E-CVT'].includes(p.code)));

    console.log('═══ İKİNCİ YAYINLAMA (idempotens) ═══');
    const r2 = await pbomPublishParts({project_name:PROJ}, pbps, tpl);
    check('hiçbir şey mükerrer oluşmadı',
      r2.added===0 && r2.purAdded===0 && r2.linked===0 && r2.skipped===2 && r2.purSkipped===2,
      JSON.stringify(r2));

    console.log('═══ H: İŞ EMRİ BAŞLATMA ENGELİ (gerçek woWaitingChildren) ═══');
    const wo = await api('POST','/work-orders',{order_id:orderId, status:'planned', notes:'E2E',
      start_datetime:new Date().toISOString().slice(0,19)});
    check('iş emri oluştu', !!wo, _lastApiError||'');
    created.wo.push(wo.id);
    const wop = await api('POST','/work-order-parts',{work_order_id:wo.id, part_id:partGvd.id, qty:1});
    created.wop.push(wop.id);
    globalThis.workOrderParts = [wop];
    let waiting = woWaitingChildren(wo.id);
    check('GVD emri engelli (PLT bitmedi)', waiting.length===1, woStartBlockMsg(waiting));
    // PLT'yi bitir → engel kalkmalı
    await dbUpdate('parts', partPlt.id, {status:'done', qty_done:1});
    globalThis.parts = await dbGet('parts');
    waiting = woWaitingChildren(wo.id);
    check('PLT bitince engel kalktı', waiting.length===0, waiting.length);

    console.log('═══ D: MAL KABUL AKIŞI (CVT kalemi) ═══');
    let whs = await dbGet('warehouses');
    let wh = whs.find(w=>w.is_active!==false);
    if(!wh){ wh = (await dbInsert('warehouses',{name:'E2E Depo'}))[0]; created.wh.push(wh.id); }
    check('sipariş ver (ORDERED)', await dbUpdate('purchase_items', piCvt.id, {status:'ORDERED'}));
    check('depoya al (IN_WAREHOUSE)', await dbUpdate('purchase_items', piCvt.id, {status:'IN_WAREHOUSE', warehouse_id:wh.id}));
    const mv = (await dbInsert('warehouse_movements',{warehouse_id:wh.id, purchase_item_id:piCvt.id,
      item_name:piCvt.name, item_code:piCvt.code, movement_type:'IN', quantity:8, unit:'adet',
      source_type:'PURCHASE_TRANSFER', performed_by:'E2E', notes:'E2E test'}))[0];
    created.mv.push(mv?.id);
    check('depo hareketi yazıldı', !!mv);
    const cvtFresh = (await dbGet('purchase_items')).find(i=>i.id===piCvt.id);
    check('damga: received_at dolu', !!cvtFresh.received_at);

    console.log('═══ G: MRP BAĞI (SAC kalemi) ═══');
    check('havuza at', await dbUpdate('purchase_items', piSac.id, {needs_planning:true}));
    const plan = (await dbInsert('purchase_items',{project_name:PROJ, name:'SAC 1350×5000×3 (E2E)',
      quantity:1, unit:'adet', notes:'📐 MRP planı — E2E', created_by:'E2E'}))[0];
    created.pi.push(plan?.id);
    check('plan kalemi oluştu', !!plan);
    check('kaynak plana bağlandı', await dbUpdate('purchase_items', piSac.id, {stock_plan_id:plan.id, needs_planning:false}));
    const sacFresh = (await dbGet('purchase_items')).find(i=>i.id===piSac.id);
    check('bağ + havuzdan düştü', sacFresh.stock_plan_id===plan.id && !sacFresh.needs_planning);

    console.log('═══ TEMİZLİK ═══');
  } catch(e){
    failures++;
    console.error('❌ BEKLENMEDİK HATA:', e.message);
  } finally {
    // temizlik: ters sırayla, hatalar yutulur
    const del = async (t,id)=>{ if(id) await dbDelete(t,id).catch(()=>{}); };
    for(const id of created.mv)  await del('warehouse_movements', id);
    for(const id of created.wop) await del('work_order_parts', id);
    for(const id of created.wo)  await del('work_orders', id);
    // stock_plan bağı olan önce (FK SET NULL zaten var ama sıralı gidelim)
    const piAll = await dbGet('purchase_items');
    for(const i of piAll.filter(x=>x.project_name===PROJ && x.stock_plan_id)) await del('purchase_items', i.id);
    for(const i of (await dbGet('purchase_items')).filter(x=>x.project_name===PROJ)) await del('purchase_items', i.id);
    for(const p of (await dbGet('parts')).filter(x=>x.project===PROJ)) await del('parts', p.id);
    for(const pb of (await dbGet('project_bom')).filter(x=>x.project_name===PROJ)) await del('project_bom', pb.id);
    for(const bp of (await dbGet('bom_products')).filter(x=>(x.name||'')==='E2E Ürün')) await del('bom_products', bp.id);
    for(const id of created.wh) await del('warehouses', id);
    for(const o of (await dbGet('orders')).filter(x=>x.project_name===PROJ)) await del('orders', o.id);
    // temizlik doğrulaması
    const leftovers =
      (await dbGet('purchase_items')).filter(x=>x.project_name===PROJ).length +
      (await dbGet('parts')).filter(x=>x.project===PROJ).length +
      (await dbGet('project_bom')).filter(x=>x.project_name===PROJ).length +
      (await dbGet('orders')).filter(x=>x.project_name===PROJ).length;
    check('temizlik tamam (0 artık kayıt)', leftovers===0, leftovers);
    console.log('\n═══ SONUÇ: ' + (failures? failures+' HATA ❌' : 'TÜM TESTLER GEÇTİ ✅') + ' ═══');
    process.exit(failures?1:0);
  }
})();
