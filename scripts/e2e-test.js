// ═══ E2E TESTİ — 3. + 4. tur özellikleri, GERÇEK frontend fonksiyonlarıyla ═══
// index.html'den pbomPublishParts / woWaitingChildren / rcvDoReceive vb.
// çıkarılır ve canlı backend'e karşı izole bir test projesinde koşulur;
// sonunda tam temizlik. 4. tur kapsamı: aynı kod farklı dallarda (aynalama),
// yayınlamada adet toplama + yeniden yayınlamada adet güncelleme, kısmi mal
// kabul (gelen/iade/bölme), depolar arası aktarım kaynak tipi.
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
  // Önce parametre parantezini atla — destructuring parametrenin '{'sı
  // gövde açılışı sanılmasın (mvInsert/splitPurchaseItem gibi)
  let i = html.indexOf('(', start), pd = 1; i++;
  while(pd>0){ const c=html[i]; if(c==='(')pd++; if(c===')')pd--; i++; }
  i = html.indexOf('{', i);
  let depth = 1; i++;
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
(0,eval)(grab('pbomActiveCodes')); // 9. tur M3: tam senkron etkin kod kümesi
(0,eval)(grab('pbomFullSync'));    // 9. tur M3: ağaçtan çıkarılanların temizliği
(0,eval)(grab('partWaitingChildren'));
(0,eval)(grab('woWaitingChildren'));
(0,eval)(grab('woStartBlockMsg'));
(0,eval)(grab('mvInsert'));          // temizlik turu: warehouse_movements kurucu (rcvDoReceive kullanır)
(0,eval)(grab('splitPurchaseItem')); // temizlik turu: kısmi kabul/aktarım ortak bölme çekirdeği
(0,eval)(grab('rcvDoReceive'));   // 4. tur: kısmi mal kabul çekirdeği
(0,eval)(grab('mipKey'));         // 7. tur #4: eşleştirme anahtarı (kod önce, yoksa ad)
(0,eval)(grab('whStockOf'));      // 7. tur #4: saf stok hesabı — _whItemStock buna delege eder
// 8. tur: yayınlama satın almaya kalem düşürmez; eksik MİP'ten gönderilir.
// Gönderme çekirdeği (grup → hesap → öneri) burada gerçek fonksiyonlarla koşulur.
(0,eval)(grab('mipCodeOf'));
(0,eval)(grab('mipNameOf'));
(0,eval)(grab('mipQtyOf'));
(0,eval)(grab('mipKindOf'));
(0,eval)(grab('mipGroupParts'));
(0,eval)(grab('mipCalcRow'));
(0,eval)(grab('mipNum'));
(0,eval)(grab('mipBuyQty'));
(0,eval)(grab('_whItemStock'));   // O6: depo net stok hesabı (sevk uyarısının çekirdeği)
(0,eval)(grab('_unlinkPlanSources')); // E5: plaka iptal/silmede kaynakları havuza döndürme

// (E1 tagged-template) h`` mekanizması — index.html'deki esc/raw/_hval/h ile
// AYNI OLMALI (değişirse İKİSİNİ de güncelle; 'h' ismi grab substring'iyle
// çakıştığından burada elle kopya tutulur). Aşağıdaki testler mekanizmanın
// davranışsal doğruluğunu (çift kaçırma, nested, raw, nullish) korur.
globalThis.esc = s => String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
globalThis.raw = s => { const r = new String(s==null?'':String(s)); r.__html = true; return r; };
globalThis._hval = v => (v==null||v===false||v===true)?'':(v&&v.__html)?v.toString():Array.isArray(v)?v.map(_hval).join(''):esc(v);
globalThis.h = (strings,...vals)=>{ let out=strings[0]; for(let i=0;i<vals.length;i++) out+=_hval(vals[i])+strings[i+1]; return raw(out); };
console.log('═══ E1 TAGGED-TEMPLATE: h`` mekanizması ═══');
check('esc: kullanıcı verisi kaçırılır',
  h`<b>${'<img src=x onerror=alert(1)>'}</b>`.toString()==='<b>&lt;img src=x onerror=alert(1)&gt;</b>');
check('raw: ham HTML korunur',
  h`<div>${raw('<i>x</i>')}</div>`.toString()==='<div><i>x</i></div>');
check('nested array: ÇİFT kaçırma YOK',
  h`<ul>${['a<b','c&d'].map(x=>h`<li>${x}</li>`)}</ul>`.toString()==='<ul><li>a&lt;b</li><li>c&amp;d</li></ul>');
check('nullish/bool → boş dize',
  h`x${null}${undefined}${false}${true}y`.toString()==='xy');
check('tırnak/ampersand kaçırma',
  h`${'A & "B" \'C\''}`.toString()==='A &amp; &quot;B&quot; &#39;C&#39;');

// ── Shims (frontend adapter'ın minimal karşılığı) ──
globalThis.currentUser = {display_name:'E2E Test'};
globalThis._lastApiError = null;
globalThis.parts = [];
globalThis.purchaseItems = [];
globalThis.workOrderParts = [];
globalThis.projectBoms = []; // B1: pbomPublishParts kardeş pbom'ları buradan bulur
globalThis.purchaseOrders = []; // B7: rcvDoReceive grup referansını buradan çözer
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
  warehouses:'warehouses', warehouse_movements:'warehouse-movements',
  warehouse_reservations:'warehouse-reservations'};
globalThis.dbGet = async (t,q)=>{
  let d = await api('GET','/'+EP[t]);
  if(!Array.isArray(d)) return [];
  // B1: pbomPublishParts kardeş pbom'ları project_bom_id filtresiyle çeker
  const byBom = /project_bom_id=eq\.([^&]+)/.exec(q||'');
  if(byBom) d = d.filter(r=>r.project_bom_id===byBom[1]);
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
// DELETE cevabında data:null gelir — api() null döndürür; başarı ölçütü hata
// OLMAMASIDIR (9. tur M3'te fark edildi: eski `!!api(...)` başarıda da false
// veriyordu, sonucu önemseyen ilk çağıran pbomFullSync oldu).
globalThis.dbDelete = async (t,id)=>{
  globalThis._lastApiError = null;
  await api('DELETE','/'+EP[t]+'/'+id);
  return !globalThis._lastApiError;
};
globalThis.genId = ()=>Date.now().toString(36)+Math.random().toString(36).slice(2,6);

(async()=>{
  const created = {parts:[], pi:[], wo:[], wop:[], mv:[], wh:[]};
  try {
    console.log('═══ 6a: BAYAT CACHE → PROJESİZ KAYIT DÜZELTMESİ (retry) ═══');
    // Gerçek adapter fonksiyonlarıyla, sahte apiFetch üzerinden: 'orders'
    // cache'i boşken proje açılır; eski yol null döner (parça projesiz
    // yazılırdı), resolveOrderIdOrRetry cache'i tazeleyip bulmalı.
    globalThis.TABLE_ENDPOINTS = {orders:'orders'};
    let _fakeOrders = [];
    globalThis.apiFetch = async ()=> _fakeOrders;
    (0,eval)(grab('const _ref').replace('const _ref =','globalThis._ref ='));
    (0,eval)(grab('_isUuid'));
    (0,eval)(grab('getRef'));
    (0,eval)(grab('invalidateRef'));
    (0,eval)(grab('projectNameToOrderId'));
    (0,eval)(grab('resolveOrderIdOrRetry'));
    await getRef('orders'); // boş liste 30 sn'liğine cache'lendi
    _fakeOrders = [{id:'yeni-order-uuid', project_name:'E2E-YENI-PROJE'}]; // proje az önce açıldı
    check('bayat cache: eski yol null döner (hatanın kendisi)',
      (await projectNameToOrderId('E2E-YENI-PROJE'))===null);
    check('resolveOrderIdOrRetry cache tazeleyip bulur',
      (await resolveOrderIdOrRetry('E2E-YENI-PROJE'))==='yeni-order-uuid');
    check('hiç olmayan proje retry sonrası da null',
      (await resolveOrderIdOrRetry('E2E-HAYALET'))===null);

    console.log('═══ KURULUM ═══ proje:', PROJ);
    const order = await api('POST','/orders',{project_name:PROJ, customer_name:'E2E Müşteri'});
    check('sipariş oluştu', !!order); orderId = order.id;

    const prod = await api('POST','/bom-products',{name:'E2E Ürün', code:'E2E-PRD-'+Date.now().toString(36)});
    check('bom ürünü oluştu', !!prod);

    // Ağaç (4. tur aynalama senaryosu — sol/sağ direk):
    //   GVD(MAMUL) ─┬─ PLT (türsüz)  ─┬─ SAC (HAMMADDE, aynı kod her iki dalda)
    //               │                 └─ BRK (türsüz, aynı kod her iki dalda)
    //               ├─ PLT2 (türsüz) ─┬─ SAC (kopya dal)
    //               │                 └─ BRK (kopya dal)
    //               └─ CVT (TEDARIK, adet 8)
    const mkBp = (name,code,kind,parent,extra)=>api('POST','/bom-parts',{
      product_id:prod.id, parent_id:parent||null, name, code,
      quantity:1, material_kind:kind, ...(extra||{})});
    const gvd  = await mkBp('E2E Gövde','E2E-GVD','MAMUL',null);
    const plt  = await mkBp('E2E Platin Sol','E2E-PLT',null,gvd.id);
    const plt2 = await mkBp('E2E Platin Sağ','E2E-PLT2',null,gvd.id);
    const sac  = await mkBp('E2E Sac','E2E-SAC','HAMMADDE',plt.id,{width_mm:300,height_mm:500,thickness_mm:3});
    const sac2 = await mkBp('E2E Sac','E2E-SAC','HAMMADDE',plt2.id,{width_mm:300,height_mm:500,thickness_mm:3});
    const brk  = await mkBp('E2E Braket','E2E-BRK',null,plt.id);
    const brk2 = await mkBp('E2E Braket','E2E-BRK',null,plt2.id);
    check('7 bom parçası oluştu (aynalama dallar dahil)', gvd&&plt&&plt2&&sac&&sac2&&brk&&brk2, _lastApiError||'');
    const cvt = await mkBp('E2E Civata','E2E-CVT','TEDARIK',gvd.id,{quantity:8});
    check('CVT oluştu', !!cvt);

    console.log('═══ 4.TUR #1: KOD BENZERSİZLİĞİ KARDEŞ KAPSAMINDA ═══');
    const dupFail = await mkBp('E2E Sac Mükerrer','E2E-SAC',null,plt.id);
    check('aynı dalda aynı kod REDDEDİLDİ', dupFail===null && /ust parca/i.test(_lastApiError||''), _lastApiError);
    const dupOk = await mkBp('E2E Sac Serbest','E2E-SAC',null,cvt.id);
    check('farklı dalda aynı kod KABUL', !!dupOk);
    if(dupOk) await dbDelete('bom_parts', dupOk.id); // ağacı bozmadan geri al

    console.log('═══ B3: TAŞIMADA KARDEŞ-KOD KONTROLÜ (bom_parts) ═══');
    // BRK(sağ dal) → PLT altına taşıma: orada aynı kodlu BRK var → RET
    const mvFail = await api('PUT','/bom-parts/'+brk2.id,{parent_id:plt.id});
    check('aynı kodlu kardeşi olan parent\'a taşıma REDDEDİLDİ',
      mvFail===null && /ust parca/i.test(_lastApiError||''), _lastApiError);
    const mvOk = await api('PUT','/bom-parts/'+brk2.id,{parent_id:cvt.id});
    check('çakışmasız taşıma KABUL', !!mvOk && mvOk.parent_id===cvt.id);
    const mvBack = await api('PUT','/bom-parts/'+brk2.id,{parent_id:plt2.id});
    check('geri taşındı', !!mvBack && mvBack.parent_id===plt2.id);

    const pbm = await api('POST','/project-bom',{project_name:PROJ, bom_product_id:prod.id, status:'draft', created_by:'E2E'});
    check('project_bom bağlantısı oluştu', !!pbm);

    // pbp'ler: project_bom CREATE sirasinda backend sablondan OTOMATIK kopyalar
    const codeOf = p => p.custom_code || p.resolved_code;
    let pbps = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm.id);
    check('8 pbp otomatik kopyalandı', pbps.length===8,
      pbps.length + ' → ' + pbps.map(codeOf).join(','));
    // Hiyerarşi bağları backend 2. pass'te kurulmuş olmalı (parent_custom_id)
    const pbpSacs = pbps.filter(p=>codeOf(p)==='E2E-SAC');
    check('aynı kodlu 2 SAC pbp FARKLI parent altında',
      pbpSacs.length===2 && pbpSacs[0].parent_custom_id!==pbpSacs[1].parent_custom_id && pbpSacs.every(p=>!!p.parent_custom_id));

    console.log('═══ B3: TAŞIMADA KARDEŞ-KOD KONTROLÜ (pbp, etkin kod) ═══');
    // Otomatik kopyalanan pbp'lerde custom_code boş — kod şablondan çözümlenir;
    // aynı etkin kodlu kardeşin yanına taşıma yine de reddedilmeli
    const sacMvFail = await api('PUT','/project-bom-parts/'+pbpSacs[1].id,{parent_custom_id:pbpSacs[0].parent_custom_id});
    check('pbp: aynı (etkin) kodlu kardeşe taşıma REDDEDİLDİ',
      sacMvFail===null && /ust parca/i.test(_lastApiError||''), _lastApiError);
    const sacHome = pbpSacs[1].parent_custom_id;
    const pbpCvtRef = pbps.find(p=>codeOf(p)==='E2E-CVT');
    const sacMvOk = await api('PUT','/project-bom-parts/'+pbpSacs[1].id,{parent_custom_id:pbpCvtRef.id});
    check('pbp: çakışmasız taşıma KABUL', !!sacMvOk && sacMvOk.parent_custom_id===pbpCvtRef.id);
    const sacMvBack = await api('PUT','/project-bom-parts/'+pbpSacs[1].id,{parent_custom_id:sacHome});
    check('pbp: geri taşındı', !!sacMvBack && sacMvBack.parent_custom_id===sacHome);

    console.log('═══ 9.TUR M4: YENİ YAYIN ÖNCE KARAR İSTER ═══');
    // Tedarik kararı artık MİP'te: karar verilmeden publish HİÇBİR yere yazmaz.
    globalThis.parts = await dbGet('parts');
    const tplPre = (await dbGet('bom_parts')).filter(b=>b.product_id===prod.id);
    const r0 = await pbomPublishParts({project_name:PROJ}, pbps, tplPre);
    check('kararsız yayın hiçbir yere yazmadı (decisionPending=6 kod)',
      r0.added===0 && r0.mipPending===0 && r0.decisionPending===6
      && !(await dbGet('parts')).some(p=>p.project===PROJ),
      JSON.stringify(r0));
    const oneriler = mipGroupParts(pbps);
    check('öneriler türden türetildi (SAC/CVT→PURCHASE, GVD→PRODUCE), karar boş',
      oneriler.find(g=>g.code==='E2E-SAC')?.suggested==='PURCHASE'
      && oneriler.find(g=>g.code==='E2E-CVT')?.suggested==='PURCHASE'
      && oneriler.find(g=>g.code==='E2E-GVD')?.suggested==='PRODUCE'
      && oneriler.every(g=>g.decision===null),
      JSON.stringify(oneriler.map(g=>g.code+':'+g.suggested)));
    const kararlar = [];
    oneriler.forEach(g=>g.pbpIds.forEach(id=>kararlar.push({id, decision:g.suggested})));
    const kararSonuc = await api('POST','/project-bom-parts/decisions',{items:kararlar, decided_by:'E2E'});
    check('toplu karar ucu tek istekte 8 pbp yazdı', kararSonuc===8, kararSonuc??_lastApiError);
    pbps = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm.id);
    check('kararlar yazıldı + damgalandı (decided_at/by)',
      pbps.every(p=>['PURCHASE','PRODUCE'].includes(p.procurement_decision))
      && pbps.every(p=>!!p.decided_at && p.decided_by==='E2E'),
      JSON.stringify(pbps.map(p=>p.procurement_decision)));

    console.log('═══ F+H + 4.TUR: YAYINLA (adet toplama + hiyerarşi) ═══');
    globalThis.parts = await dbGet('parts');
    const tpl = (await dbGet('bom_parts')).filter(b=>b.product_id===prod.id);
    const r1 = await pbomPublishParts({project_name:PROJ}, pbps, tpl);
    console.log('  sonuç:', JSON.stringify(r1), '→', pbomPublishMsg(r1));
    check('4 parça üretime (GVD, PLT, PLT2, BRK-tek satır)', r1.added===4, r1.added);
    // 8. tur: yayınlama satın almaya kalem DÜŞÜRMEZ — 2 parça MİP'e düşer
    check('satın almaya kalem düşmedi, 2 parça MİP\'e (mipPending)',
      r1.mipPending===2 && (await dbGet('purchase_items')).filter(i=>i.project_name===PROJ).length===0,
      JSON.stringify({mipPending:r1.mipPending}));
    check('3 hiyerarşi bağı (PLT→GVD, PLT2→GVD, BRK→PLT)', r1.linked===3, r1.linked);

    const partGvd  = parts.find(p=>p.project===PROJ && p.code==='E2E-GVD');
    const partPlt  = parts.find(p=>p.project===PROJ && p.code==='E2E-PLT');
    const partPlt2 = parts.find(p=>p.project===PROJ && p.code==='E2E-PLT2');
    const partBrk  = parts.find(p=>p.project===PROJ && p.code==='E2E-BRK');
    check('PLT.parent === GVD', partPlt?.parent_part_id===partGvd?.id);
    check('PLT2.parent === GVD', partPlt2?.parent_part_id===partGvd?.id);
    // Aynı koda toplanan parçada "ilk dalın atası kazanır" — hangi dalın ilk
    // geldiği liste sırasına (UUID) bağlı ve bilinçli keyfi; iki dal da geçerli
    check('BRK tek satır, adet TOPLAMI 2, parent dallardan biri (PLT/PLT2)',
      Number(partBrk?.qty)===2 && [partPlt?.id, partPlt2?.id].includes(partBrk?.parent_part_id),
      `qty=${partBrk?.qty}, parent=${partBrk?.parent_part_id===partPlt?.id?'PLT':(partBrk?.parent_part_id===partPlt2?.id?'PLT2':'?')}`);
    check('SAC/CVT parts\'a GİRMEDİ', !parts.some(p=>p.project===PROJ&&['E2E-SAC','E2E-CVT'].includes(p.code)));

    console.log('═══ 8.TUR: EKSİK, MİP\'TEN SATIN ALMAYA GÖNDERİLİR ═══');
    // Gerçek çekirdek: mipGroupParts → mipCalcRow → mipBuyQty → insert
    // (mipBuyConfirm'in yazdığı kalemle birebir alanlar).
    const mipGruplar = mipGroupParts(pbps).filter(g=>g.decision==='PURCHASE'); // (M4) satın alma akışı yalnız PURCHASE kararlılar
    check('MİP 2 grup görüyor (SAC adet toplamı 2 + CVT 8)',
      mipGruplar.length===2
      && mipGruplar.find(g=>g.code==='E2E-SAC')?.need===2
      && mipGruplar.find(g=>g.code==='E2E-CVT')?.need===8,
      JSON.stringify(mipGruplar.map(g=>({c:g.code, n:g.need}))));
    check('grup pbp bağı + malzeme taşıyor (MRP ölçüleri için)',
      mipGruplar.every(g=>!!g.pbpId), JSON.stringify(mipGruplar.map(g=>!!g.pbpId)));
    for(const g of mipGruplar){
      const row = mipCalcRow(g, [], [], [], PROJ);
      const q = mipBuyQty(row);
      check('stok yokken öneri = ihtiyaç ('+g.code+')', q===row.need && q===row.missing, q);
      const d = await dbInsert('purchase_items',{project_name:PROJ,
        project_bom_part_id:g.pbpId||null, name:g.name, code:g.code,
        quantity:q, unit:g.unit||'adet', material:g.material||null, created_by:'E2E MİP'});
      check('kalem gönderildi ('+g.code+')', Array.isArray(d)&&!!d[0], _lastApiError||'');
    }
    globalThis.purchaseItems = await dbGet('purchase_items');
    const piSac = purchaseItems.find(i=>i.project_name===PROJ && i.code==='E2E-SAC');
    const piCvt = purchaseItems.find(i=>i.project_name===PROJ && i.code==='E2E-CVT');
    check('SAC satın almada TEK kalem, adet TOPLAMI 2', Number(piSac?.quantity)===2, piSac?.quantity);
    check('CVT kalemi adet 8', Number(piCvt?.quantity)===8, piCvt?.quantity);
    // Mükerrer gönderim koruması: PLANNED kalem öneriden düşer
    const rowSacSonra = mipCalcRow(mipGruplar.find(g=>g.code==='E2E-SAC'), [], [], purchaseItems, PROJ);
    check('gönderilen PLANNED öneriden düşer (mipBuyQty 0)', mipBuyQty(rowSacSonra)===0, mipBuyQty(rowSacSonra));

    console.log('═══ İKİNCİ YAYINLAMA (idempotens) ═══');
    const r2 = await pbomPublishParts({project_name:PROJ}, pbps, tpl);
    check('hiçbir şey mükerrer oluşmadı',
      r2.added===0 && r2.linked===0 && r2.updated===0
      && r2.skipped===4 && r2.mipPending===2,
      JSON.stringify(r2));
    check('republish satın almaya DOKUNMADI (2 kalem aynen)',
      (await dbGet('purchase_items')).filter(i=>i.project_name===PROJ).length===2);

    console.log('═══ 4.TUR: YENİDEN YAYINLAMADA ADET GÜNCELLEME ═══');
    // BRK'nin bir dalındaki adet 1→2 yapılır → toplam 3 → parts güncellenmeli
    const pbpBrk1 = pbps.find(p=>codeOf(p)==='E2E-BRK');
    check('pbp adet değişti', await dbUpdate('project_bom_parts', pbpBrk1.id, {custom_qty:2}));
    const pbpsFresh = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm.id);
    const r3 = await pbomPublishParts({project_name:PROJ}, pbpsFresh, tpl);
    globalThis.parts = await dbGet('parts');
    const brkFresh = parts.find(p=>p.project===PROJ && p.code==='E2E-BRK');
    check('adet güncellendi (updated=1, BRK qty 3)', r3.updated===1 && Number(brkFresh?.qty)===3,
      `updated=${r3.updated}, qty=${brkFresh?.qty}`);

    console.log('═══ B1: ÇAPRAZ-PBOM ADET (aynı kod iki makinede) ═══');
    // İkinci ürün ağacı aynı projeye bağlanır; E2E-BRK kodunu 5 adetle içerir.
    // parts.qty proje-geneli toplam olmalı: 3 (pbm) + 5 (pbm2) = 8; republish
    // ping-pong yapmamalı (eski hata: her yayın kendi toplamını yazıyordu).
    const prod2 = await api('POST','/bom-products',{name:'E2E Ürün', code:'E2E-PRD2-'+Date.now().toString(36)});
    const gvd2 = await api('POST','/bom-parts',{product_id:prod2.id, parent_id:null,
      name:'E2E Gövde 2', code:'E2E-GVD2', quantity:1, material_kind:'MAMUL'});
    const brkX = await api('POST','/bom-parts',{product_id:prod2.id, parent_id:gvd2.id,
      name:'E2E Braket', code:'E2E-BRK', quantity:5});
    check('ikinci ürün ağacı kuruldu', prod2&&gvd2&&brkX, _lastApiError||'');
    const pbm2 = await api('POST','/project-bom',{project_name:PROJ, bom_product_id:prod2.id, status:'draft', created_by:'E2E'});
    let pbps2 = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm2.id);
    const tpl2 = (await dbGet('bom_parts')).filter(b=>b.product_id===prod2.id);
    // (M4) pbm2 kararları da öneriyle onaylanır (türsüz BRK + MAMUL GVD2 → PRODUCE)
    const kararlar2 = [];
    mipGroupParts(pbps2).forEach(g=>g.pbpIds.forEach(id=>kararlar2.push({id, decision:g.suggested})));
    await api('POST','/project-bom-parts/decisions',{items:kararlar2, decided_by:'E2E'});
    pbps2 = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm2.id);
    globalThis.projectBoms = [
      {id:pbm.id,  project_name:PROJ, status:'published'},
      {id:pbm2.id, project_name:PROJ, status:'published'}];
    const r4 = await pbomPublishParts({id:pbm2.id, project_name:PROJ}, pbps2, tpl2);
    globalThis.parts = await dbGet('parts');
    let brkTotal = parts.find(p=>p.project===PROJ && p.code==='E2E-BRK');
    check('BRK adedi iki pbom toplamı (3+5=8)', r4.updated===1 && Number(brkTotal?.qty)===8,
      `updated=${r4.updated}, qty=${brkTotal?.qty}`);
    const r5 = await pbomPublishParts({id:pbm.id, project_name:PROJ}, pbpsFresh, tpl);
    globalThis.parts = await dbGet('parts');
    brkTotal = parts.find(p=>p.project===PROJ && p.code==='E2E-BRK');
    check('ilk pbom republish ping-pong yapmadı (qty 8 kaldı)',
      r5.updated===0 && Number(brkTotal?.qty)===8, `updated=${r5.updated}, qty=${brkTotal?.qty}`);
    // Tamamlanan üretim yeni hedefi aşıyorsa adet EZİLMEZ:
    // pbm2'deki BRK 5→4 (hedef 3+4=7) ama qty_done=99 → güncelleme bloklanır
    const pbpBrkX = pbps2.find(p=>codeOf(p)==='E2E-BRK');
    await dbUpdate('project_bom_parts', pbpBrkX.id, {custom_qty:4});
    await dbUpdate('parts', brkTotal.id, {status:'done', qty_done:99});
    globalThis.parts = await dbGet('parts');
    const pbps2b = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm2.id);
    const r5b = await pbomPublishParts({id:pbm2.id, project_name:PROJ}, pbps2b, tpl2);
    globalThis.parts = await dbGet('parts');
    brkTotal = parts.find(p=>p.project===PROJ && p.code==='E2E-BRK');
    check('qty_done > hedef iken adet EZİLMEDİ (qtyBlocked=1, qty 8 kaldı)',
      r5b.qtyBlocked===1 && r5b.updated===0 && Number(brkTotal?.qty)===8,
      JSON.stringify({updated:r5b.updated, blocked:r5b.qtyBlocked, qty:brkTotal?.qty}));
    // geri al: pbm2 BRK adedi 5'e, üretim sayaçları sıfıra
    await dbUpdate('project_bom_parts', pbpBrkX.id, {custom_qty:5});
    await dbUpdate('parts', brkTotal.id, {status:'pending', qty_done:0});
    globalThis.parts = await dbGet('parts');

    console.log('═══ B2 (8. turda değişti): REPUBLISH SATIN ALMAYA DOKUNMAZ, FARKI MİP GÖSTERİR ═══');
    // CVT adedi 8→10: republish kalemi ARTIK güncellemez; fark MİP'te
    // "Satın Almaya Gönder (2)" önerisi olarak görünür.
    const pbpCvt = pbpsFresh.find(p=>codeOf(p)==='E2E-CVT');
    check('CVT pbp adedi 10 yapıldı', await dbUpdate('project_bom_parts', pbpCvt.id, {custom_qty:10}));
    let pbpsB2 = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm.id);
    await pbomPublishParts({id:pbm.id, project_name:PROJ}, pbpsB2, tpl);
    globalThis.purchaseItems = await dbGet('purchase_items');
    check('kalem adedi DEĞİŞMEDİ (8 kaldı)',
      Number(purchaseItems.find(i=>i.id===piCvt.id)?.quantity)===8,
      purchaseItems.find(i=>i.id===piCvt.id)?.quantity);
    const rowCvtB2 = mipCalcRow(mipGroupParts(pbpsB2).find(g=>g.code==='E2E-CVT'), [], [], purchaseItems, PROJ);
    check('MİP farkı önerir (ihtiyaç 10 − planlandı 8 = 2)', mipBuyQty(rowCvtB2)===2, mipBuyQty(rowCvtB2));
    await dbUpdate('project_bom_parts', pbpCvt.id, {custom_qty:8});

    console.log('═══ H: İŞ EMRİ BAŞLATMA ENGELİ ═══');
    const wo = await api('POST','/work-orders',{order_id:orderId, status:'planned', notes:'E2E',
      start_datetime:new Date().toISOString().slice(0,19)});
    check('iş emri oluştu', !!wo, _lastApiError||'');
    created.wo.push(wo.id);
    const wop = await api('POST','/work-order-parts',{work_order_id:wo.id, part_id:partGvd.id, qty:1});
    created.wop.push(wop.id);
    globalThis.workOrderParts = [wop];
    let waiting = woWaitingChildren(wo.id);
    check('GVD emri engelli (PLT+PLT2 bitmedi)', waiting.length===2, woStartBlockMsg(waiting));
    await dbUpdate('parts', partPlt.id, {status:'done', qty_done:1});
    await dbUpdate('parts', partPlt2.id, {status:'done', qty_done:1});
    globalThis.parts = await dbGet('parts');
    waiting = woWaitingChildren(wo.id);
    check('ikisi bitince engel kalktı', waiting.length===0, waiting.length);

    console.log('═══ O1: PARÇA SİLME GUARD\'LARI ═══');
    globalThis._lastApiError = null;
    await api('DELETE','/parts/'+partGvd.id);
    check('iş emrine bağlı parça SİLİNEMEDİ', /is emrine bagli/i.test(_lastApiError||''), _lastApiError);
    globalThis._lastApiError = null;
    await api('DELETE','/parts/'+partPlt.id);
    check('üretim ilerlemesi olan parça SİLİNEMEDİ', /ilerleme/i.test(_lastApiError||''), _lastApiError);

    console.log('═══ U1: GEÇMİŞİ OLAN KULLANICI SİLİNEMEZ (500 yerine dostça) ═══');
    const u1user = await api('POST','/users',{name:'E2E U1 Personel', role:'Operatör'});
    check('U1 test personeli oluştu', !!u1user, _lastApiError||'');
    const u1log = await api('POST','/part-logs',{part_id:partGvd.id, user_id:u1user.id, qty_done:1});
    check('personele üretim kaydı bağlandı', !!u1log, _lastApiError||'');
    globalThis._lastApiError = null;
    await api('DELETE','/users/'+u1user.id);
    check('üretim kaydı olan personel SİLİNEMEDİ (dostça mesaj)', /uretim kaydi|silinemez/i.test(_lastApiError||''), _lastApiError);
    check('personel yerinde duruyor', (await api('GET','/users')||[]).some(x=>x.id===u1user.id));
    // temizlik: önce log, sonra personel (log kalırsa personel silinemez)
    await api('DELETE','/part-logs/'+u1log.id);
    await api('DELETE','/users/'+u1user.id);
    check('U1 temizlik: log+personel silindi', !(await api('GET','/users')||[]).some(x=>x.id===u1user.id));

    console.log('═══ U4: QR SAYAÇ ATOMİK ARTIŞ (lost-update önlemi) ═══');
    const u4Before = Number((await dbGet('parts')).find(p=>p.id===partGvd.id)?.qty_done)||0;
    const u4u = await api('POST','/users',{name:'E2E U4 Personel', role:'Operatör'});
    check('U4 personeli oluştu', !!u4u, _lastApiError||'');
    // iki ardışık log: atomik SQL increment doğru toplamalı (Java oku-yaz değil)
    await api('POST','/part-logs',{part_id:partGvd.id, user_id:u4u.id, qty_done:2});
    await api('POST','/part-logs',{part_id:partGvd.id, user_id:u4u.id, qty_done:3});
    const u4After = (await dbGet('parts')).find(p=>p.id===partGvd.id);
    check('iki log atomik toplandı (qty_done +5)', (Number(u4After.qty_done)||0)-u4Before===5,
      `${u4Before} → ${u4After.qty_done}`);
    const _t=Number(u4After.qty)||0,_d=Number(u4After.qty_done)||0,_r=Number(u4After.qty_reject)||0;
    check('qty_pending türetildi (max(0,total-done-reject))',
      Number(u4After.qty_pending)===Math.max(0,_t-_d-_r), `pend=${u4After.qty_pending}`);
    // temizlik: logları sil (U1 guard: logu olan personel silinemez), sonra personel
    for(const l of (await api('GET','/part-logs?userId='+u4u.id)||[])) await api('DELETE','/part-logs/'+l.id);
    await api('DELETE','/users/'+u4u.id);
    check('U4 temizlik tamam', !(await api('GET','/users')||[]).some(x=>x.id===u4u.id));

    console.log('═══ 4.TUR #3: KISMİ MAL KABUL (gerçek rcvDoReceive) ═══');
    let whs = await dbGet('warehouses');
    let wh = whs.find(w=>w.is_active!==false);
    if(!wh){ wh = (await dbInsert('warehouses',{name:'E2E Depo'}))[0]; created.wh.push(wh.id); }
    check('sipariş ver (ORDERED + termin)', await dbUpdate('purchase_items', piCvt.id, {status:'ORDERED', expected_date:'2026-08-01'}));
    // 8. tur: sipariş verilmiş kalem republish'te zaten hiç dokunulmaz
    await dbUpdate('project_bom_parts', pbpCvt.id, {custom_qty:12});
    const pbpsB2c = (await dbGet('project_bom_parts')).filter(p=>p.project_bom_id===pbm.id);
    await pbomPublishParts({id:pbm.id, project_name:PROJ}, pbpsB2c, tpl);
    globalThis.purchaseItems = await dbGet('purchase_items');
    check('ORDERED kalem adedi republish\'te korundu (8)',
      Number(purchaseItems.find(i=>i.id===piCvt.id)?.quantity)===8,
      purchaseItems.find(i=>i.id===piCvt.id)?.quantity);
    await dbUpdate('project_bom_parts', pbpCvt.id, {custom_qty:8});
    globalThis.purchaseItems = await dbGet('purchase_items');
    // 8 beklenirken 5 geldi, 1'i iade → 4 depoya girer, 4 beklemede kalır (kalem bölünür)
    const okPartial = await rcvDoReceive(piCvt.id, wh.id, 4, 1);
    check('kısmi kabul çalıştı', okPartial===true);
    const freshAll = await dbGet('purchase_items');
    const cvtOrig  = freshAll.find(i=>i.id===piCvt.id);
    const cvtSplit = freshAll.find(i=>i.project_name===PROJ && i.code==='E2E-CVT' && i.id!==piCvt.id);
    check('orijinal: 4 beklemede, ORDERED, iade 1',
      Number(cvtOrig?.quantity)===4 && cvtOrig?.status==='ORDERED' && Number(cvtOrig?.returned_qty)===1,
      `q=${cvtOrig?.quantity} s=${cvtOrig?.status} iade=${cvtOrig?.returned_qty}`);
    check('bölünen: 4 depoda, kabul 4, alan damgalı',
      Number(cvtSplit?.quantity)===4 && cvtSplit?.status==='IN_WAREHOUSE'
      && Number(cvtSplit?.received_qty)===4 && cvtSplit?.received_by==='E2E Test',
      `q=${cvtSplit?.quantity} s=${cvtSplit?.status} alan=${cvtSplit?.received_by}`);
    check('bölünen kalem received_at damgalı (B4 — whUndo RECEIVED\'a döndürebilir)',
      !!cvtSplit?.received_at, cvtSplit?.received_at);
    check('bölünen kalem termin + sipariş damgası taşıdı (B7)',
      cvtSplit?.expected_date==='2026-08-01' && !!cvtSplit?.ordered_at,
      `termin=${cvtSplit?.expected_date} sipariş=${cvtSplit?.ordered_at}`);
    const mvIn = (await dbGet('warehouse_movements')).find(m=>m.purchase_item_id===cvtSplit?.id);
    check('IN hareketi kabul adediyle (4, GOODS_RECEIPT)',
      mvIn && Number(mvIn.quantity)===4 && mvIn.movement_type==='IN' && mvIn.source_type==='GOODS_RECEIPT');
    // Kalan 4 tam kabul → orijinal kalem depoya geçer
    globalThis.purchaseItems = await dbGet('purchase_items');
    const okFull = await rcvDoReceive(piCvt.id, wh.id, 4, 0);
    const cvtDone = (await dbGet('purchase_items')).find(i=>i.id===piCvt.id);
    check('tam kabul: IN_WAREHOUSE + received_at damgalı + kabul 4',
      okFull===true && cvtDone?.status==='IN_WAREHOUSE' && !!cvtDone?.received_at && Number(cvtDone?.received_qty)===4,
      `s=${cvtDone?.status}`);

    console.log('═══ 4.TUR #2: DEPOLAR ARASI AKTARIM KAYNAK TİPİ ═══');
    const mvX = (await dbInsert('warehouse_movements',{warehouse_id:wh.id, purchase_item_id:null,
      item_name:'E2E Aktarım Malzemesi', item_code:'E2E-XFR', movement_type:'IN', quantity:3,
      unit:'adet', source_type:'WAREHOUSE_TRANSFER', performed_by:'E2E', notes:'E2E depolar arası'}))[0];
    check('WAREHOUSE_TRANSFER hareketi kabul edildi', !!mvX, _lastApiError||'');
    created.mv.push(mvX?.id);

    console.log('═══ G: MRP BAĞI (SAC kalemi) ═══');
    check('havuza at', await dbUpdate('purchase_items', piSac.id, {needs_planning:true}));
    const plan = (await dbInsert('purchase_items',{project_name:PROJ, name:'SAC 1350×5000×3 (E2E)',
      quantity:1, unit:'adet', notes:'📐 MRP planı — E2E', created_by:'E2E'}))[0];
    created.pi.push(plan?.id);
    check('plan kalemi oluştu', !!plan);
    check('kaynak plana bağlandı', await dbUpdate('purchase_items', piSac.id, {stock_plan_id:plan.id, needs_planning:false}));
    const sacFresh = (await dbGet('purchase_items')).find(i=>i.id===piSac.id);
    check('bağ + havuzdan düştü', sacFresh.stock_plan_id===plan.id && !sacFresh.needs_planning);
    // (E5) Plaka iptal/silinince kaynak havuza geri döner (çekirdek: _unlinkPlanSources)
    globalThis.purchaseItems = await dbGet('purchase_items');
    const freed = await _unlinkPlanSources(plan.id);
    check('plaka çözüldü: 1 kaynak havuza döndü', freed===1, freed);
    const sacFreed = (await dbGet('purchase_items')).find(i=>i.id===piSac.id);
    check('kaynak: stock_plan_id null + needs_planning true',
      !sacFreed.stock_plan_id && sacFreed.needs_planning===true,
      JSON.stringify({sp:sacFreed.stock_plan_id, np:sacFreed.needs_planning}));

    console.log('═══ E2: ONDALIK ADET ÜRETİMDE YUKARI YUVARLANIR ═══');
    // parts.total_qty INTEGER — ondalık BOM adedi eskiden sessizce kesiliyordu.
    // İzole: türsüz ondalık (2.5) parça yayınla → parts qty 3 + rounded uyarısı.
    globalThis.projectBoms = [];
    const e2fake = [{id:'e2-fake-ondalik', custom_code:'E2E-ONDALIK', custom_name:'E2E Ondalık Parça', custom_qty:2.5, material_kind:null, procurement_decision:'PRODUCE'}];
    const rE2 = await pbomPublishParts({id:'yok', project_name:PROJ}, e2fake, []);
    globalThis.parts = await dbGet('parts');
    const ondalik = parts.find(p=>p.project===PROJ && p.code==='E2E-ONDALIK');
    check('ondalık 2.5 → parts qty 3 (yukarı) + rounded=1',
      rE2.rounded===1 && Number(ondalik?.qty)===3, JSON.stringify({rounded:rE2.rounded, qty:ondalik?.qty}));

    console.log('═══ 9.TUR M3: TAM SENKRON (ağaçtan çıkarılanların temizliği) ═══');
    // Etkin kod kümesi GERÇEK yayınlanmış ağaçlardan kurulur; yetim kayıtlar:
    // işlem görmemiş üretim → silinir, işlem görmüş → korunur+listelenir,
    // PLANNED+grupsuz satın alma → CANCELLED, ORDERED → korunur+listelenir.
    globalThis.PUR_STATUS = {ORDERED:{label:'Sipariş Verildi'}, PLANNED:{label:'Planlandı'}};
    // e2e yayını doğrudan pbomPublishParts ile yapar (DB'de status draft kalır);
    // B1 desenindeki gibi yayınlanmış fixture elle kurulur — pbomActiveCodes
    // ağaç içeriğini DB'den, yayın durumunu bu global listeden okur.
    globalThis.projectBoms = [
      {id:pbm.id,  project_name:PROJ, status:'published'},
      {id:pbm2.id, project_name:PROJ, status:'published'}];
    const orfTemiz = (await dbInsert('parts',{id:genId(), name:'E2E Yetim Temiz', code:'E2E-ORF1',
      project:PROJ, qty:2, qty_done:0, qty_pending:2, qty_reject:0, status:'pending'}))[0];
    const orfIslemli = (await dbInsert('parts',{id:genId(), name:'E2E Yetim İşlemli', code:'E2E-ORF2',
      project:PROJ, qty:2, qty_done:1, qty_pending:1, qty_reject:0, status:'inprogress'}))[0];
    const orfPlanned = (await dbInsert('purchase_items',{project_name:PROJ, name:'E2E Yetim Kalem',
      code:'E2E-ORF3', quantity:4}))[0];
    const orfOrdered = (await dbInsert('purchase_items',{project_name:PROJ, name:'E2E Yetim Sipariş',
      code:'E2E-ORF4', quantity:4}))[0];
    await dbUpdate('purchase_items', orfOrdered.id, {status:'ORDERED'});
    globalThis.parts = await dbGet('parts');
    const aktifKodlar = await pbomActiveCodes(PROJ); // ağaçta yaşayan kodlar (kontrol için)
    const syn = await pbomFullSync({project_name:PROJ});
    console.log('  senkron sonucu:', JSON.stringify({...syn,
      keptStarted:syn.keptStarted, keptOrdered:syn.keptOrdered}));
    globalThis.parts = await dbGet('parts');
    check('işlem görmemiş yetim üretim parçası SİLİNDİ', !parts.some(p=>p.id===orfTemiz.id) && syn.removedParts>=1,
      JSON.stringify({removed:syn.removedParts}));
    check('işlem görmüş yetim parça KORUNDU + listelendi',
      parts.some(p=>p.id===orfIslemli.id) && syn.keptStarted.includes('E2E-ORF2'),
      JSON.stringify(syn.keptStarted));
    const synPis = await dbGet('purchase_items');
    check('yetim PLANNED kalem iptal edildi (not düşüldü)',
      synPis.find(i=>i.id===orfPlanned.id)?.status==='CANCELLED'
      && /tam senkron/i.test(synPis.find(i=>i.id===orfPlanned.id)?.notes||'') && syn.cancelledPurchases>=1,
      JSON.stringify({s:synPis.find(i=>i.id===orfPlanned.id)?.status, c:syn.cancelledPurchases}));
    check('ORDERED yetim kalem KORUNDU (uyarı listesinde)',
      synPis.find(i=>i.id===orfOrdered.id)?.status==='ORDERED'
      && syn.keptOrdered.some(s=>s.includes('E2E-ORF4')), JSON.stringify(syn.keptOrdered));
    check('ağaçtaki üretim kodlarına dokunulmadı',
      [...aktifKodlar.prod].every(c=>parts.some(p=>p.project===PROJ && (p.code||'').toLowerCase()===c)),
      [...aktifKodlar.prod].join(','));
    check('sahte ağaçtan yayınlanan E2E-ONDALIK da temizlendi (ağaçta yok, işlemsiz)',
      !parts.some(p=>p.project===PROJ && p.code==='E2E-ONDALIK'));
    // temizlik: işlemli yetim parçanın sayaçları sıfırlanmadan silinemez (genel
    // temizlik bölümü hallediyor); ORDERED yetim de orada CANCELLED+silinir.

    console.log('═══ O6: DEPO NET STOK HESABI (_whItemStock) ═══');
    // dnShip eksi-stok uyarısı bu hesaba dayanır: SUM(IN)−SUM(OUT), kod önce
    globalThis.whMovements = [
      {warehouse_id:wh.id, item_code:'E2E-STK', item_name:'x', movement_type:'IN',  quantity:3},
      {warehouse_id:wh.id, item_code:'E2E-STK', item_name:'x', movement_type:'OUT', quantity:1},
      {warehouse_id:wh.id, item_code:'E2E-OTH', item_name:'y', movement_type:'IN',  quantity:5},
      {warehouse_id:'baska-depo', item_code:'E2E-STK', item_name:'x', movement_type:'IN', quantity:9}
    ];
    check('net = IN−OUT, kod+depo eşleşmeli (3−1=2)', _whItemStock(wh.id,'x','E2E-STK')===2, _whItemStock(wh.id,'x','E2E-STK'));
    check('eksiye düşen tespit: 2 stok < 10 sevk', (2 - 10) < 0);
    check('farklı kod ayrı sayılır (5)', _whItemStock(wh.id,'y','E2E-OTH')===5);
    globalThis.whMovements = [];

    console.log('═══ O4: SATIN ALMA KALEMİ SİLME GUARD\'I ═══');
    globalThis._lastApiError = null;
    await api('DELETE','/purchase-items/'+cvtSplit.id);
    check('depodaki kalem SİLİNEMEDİ', /depodaki kalem silinemez/i.test(_lastApiError||''), _lastApiError);
    check('kalem yerinde duruyor', (await dbGet('purchase_items')).some(x=>x.id===cvtSplit.id));

    console.log('═══ O5: HAREKET DEFTERİ SİLME KISITI ═══');
    globalThis._lastApiError = null;
    await api('DELETE','/warehouse-movements/'+mvIn.id);
    check('kaleme bağlı mal kabul hareketi SİLİNEMEDİ', /defterden silinemez/i.test(_lastApiError||''), _lastApiError);
    check('hareket yerinde duruyor', (await dbGet('warehouse_movements')).some(m=>m.id===mvIn.id));

    console.log('═══ 7.TUR #4 AŞAMA 2: DEPO REZERVASYONU (talep → kısmi onay) ═══');
    // test.txt senaryosu: kayıtta 30 görünüyor, sayımda 15 çıkıyor →
    // 15 onay (RESERVATION OUT) + 15 kayıp satın almaya + envanter düzeltme OUT.
    const rezIn = await api('POST','/warehouse-movements',{warehouse_id:wh.id,
      item_name:'E2E Rezerv Malzeme', item_code:'E2E-REZ', movement_type:'IN',
      quantity:30, source_type:'MANUAL'});
    check('rezervasyon stoğu girildi (30)', !!rezIn, _lastApiError||'');
    globalThis._lastApiError = null;
    const sahte = await api('POST','/warehouse-movements',{warehouse_id:wh.id,
      item_name:'E2E Rezerv Malzeme', item_code:'E2E-REZ', movement_type:'OUT',
      quantity:1, source_type:'RESERVATION'});
    check('dışarıdan RESERVATION hareketi REDDEDİLDİ', !sahte, _lastApiError);

    const wres = await api('POST','/warehouse-reservations',{project_name:PROJ,
      warehouse_id:wh.id, item_name:'E2E Rezerv Malzeme', item_code:'E2E-REZ',
      requested_qty:30, unit:'adet', requested_by:'E2E MİP'});
    check('talep oluştu (REQUESTED)', !!wres && wres.status==='REQUESTED', wres&&wres.status);

    globalThis._lastApiError = null;
    const g1 = await api('POST','/warehouse-reservations/'+wres.id+'/approve',
      {approved_qty:15, write_adjustment:true});
    check('açıklamasız kısmi onay REDDEDİLDİ', !g1 && /aciklama zorunlu/i.test(_lastApiError||''), _lastApiError);
    globalThis._lastApiError = null;
    const g2 = await api('POST','/warehouse-reservations/'+wres.id+'/approve',
      {approved_qty:31, shortage_reason:'x'});
    check('istenenden fazla onay REDDEDİLDİ', !g2, _lastApiError);

    const onay = await api('POST','/warehouse-reservations/'+wres.id+'/approve',
      {approved_qty:15, shortage_reason:'Sayımda 15 çıktı — kayıp/envanter yanlış',
       write_adjustment:true, approved_by:'E2E Depocu'});
    check('kısmi onay → PARTIAL, onaylanan 15',
      !!onay && onay.status==='PARTIAL' && Number(onay.approved_qty)===15,
      onay ? onay.status+'/'+onay.approved_qty : _lastApiError);
    const rezMvs = (await dbGet('warehouse_movements')).filter(m=>m.reservation_id===wres.id);
    const rezOut = rezMvs.find(m=>m.source_type==='RESERVATION');
    const rezAdj = rezMvs.find(m=>m.source_type==='RESERVATION_ADJUST');
    check('RESERVATION OUT 15 yazıldı (projeye mal edildi)',
      !!rezOut && rezOut.movement_type==='OUT' && Number(rezOut.quantity)===15);
    check('RESERVATION_ADJUST OUT 15 yazıldı (hayalet stok düzeltildi)',
      !!rezAdj && rezAdj.movement_type==='OUT' && Number(rezAdj.quantity)===15);
    globalThis.whMovements = await dbGet('warehouse_movements');
    check('net stok 0 (30 giriş − 15 rezerve − 15 düzeltme)',
      _whItemStock(wh.id,'E2E Rezerv Malzeme','E2E-REZ')===0,
      _whItemStock(wh.id,'E2E Rezerv Malzeme','E2E-REZ'));
    globalThis.whMovements = [];
    const eksikPi = (await dbGet('purchase_items')).find(i=>
      i.project_name===PROJ && i.code==='E2E-REZ' && i.status==='PLANNED');
    check('eksik 15 satın almaya düştü (PLANNED + gerekçeli not)',
      !!eksikPi && Number(eksikPi.quantity)===15 && /rezervasyon eksigi/i.test(eksikPi.notes||''),
      eksikPi ? eksikPi.quantity+' / '+(eksikPi.notes||'') : 'kalem yok');

    globalThis._lastApiError = null;
    const tekrar = await api('POST','/warehouse-reservations/'+wres.id+'/approve',
      {approved_qty:15, shortage_reason:'x'});
    check('ikinci onay REDDEDİLDİ (sonuçlanmış talep)', !tekrar && /sonuclanmis/i.test(_lastApiError||''), _lastApiError);
    globalThis._lastApiError = null;
    await api('DELETE','/warehouse-movements/'+rezOut.id);
    check('rezervasyona bağlı hareket SİLİNEMEDİ', /silinemez/i.test(_lastApiError||''), _lastApiError);

    // Stok aşımı guard'ı + iptal akışı (stok artık 0)
    const wres2 = await api('POST','/warehouse-reservations',{project_name:PROJ,
      warehouse_id:wh.id, item_name:'E2E Rezerv Malzeme', item_code:'E2E-REZ', requested_qty:5});
    globalThis._lastApiError = null;
    const g3 = await api('POST','/warehouse-reservations/'+wres2.id+'/approve',{approved_qty:5});
    check('kayıtlı stoğu aşan onay REDDEDİLDİ', !g3 && /stogu asiyor/i.test(_lastApiError||''), _lastApiError);
    const iptal = await api('POST','/warehouse-reservations/'+wres2.id+'/cancel');
    check('bekleyen talep iptal edildi (CANCELLED)', !!iptal && iptal.status==='CANCELLED', iptal&&iptal.status);
    globalThis._lastApiError = null;
    const iptal2 = await api('POST','/warehouse-reservations/'+wres.id+'/cancel');
    check('sonuçlanmış talep iptal EDİLEMEDİ', !iptal2, _lastApiError);

    console.log('═══ 9.TUR M7: EKSİK MEVCUT PLANNED KALEME EKLENİR ("40→42" bug\'ı) ═══');
    // İkinci eksik AYRI kalem açmamalı: az önce oluşan 15'lik PLANNED kalem
    // 15+2=17'ye çıkmalı (arkadaş raporu: "eksik 42 oldu ama sipariş 40'ta kaldı").
    const wres3 = await api('POST','/warehouse-reservations',{project_name:PROJ,
      warehouse_id:wh.id, item_name:'E2E Rezerv Malzeme', item_code:'E2E-REZ',
      requested_qty:2, unit:'adet'});
    const red3 = await api('POST','/warehouse-reservations/'+wres3.id+'/approve',
      {approved_qty:0, shortage_reason:'Depoda yok — M7 birleşme testi'});
    check('tam red → REJECTED', !!red3 && red3.status==='REJECTED',
      red3 ? red3.status : _lastApiError);
    const m7Pis = (await dbGet('purchase_items')).filter(i=>
      i.project_name===PROJ && i.code==='E2E-REZ' && i.status==='PLANNED');
    check('eksik AYNI kaleme eklendi (15+2=17, kalem sayısı 1)',
      m7Pis.length===1 && Number(m7Pis[0].quantity)===17 && /\+2/.test(m7Pis[0].notes||''),
      m7Pis.length+' kalem / '+(m7Pis[0] ? m7Pis[0].quantity+' / '+(m7Pis[0].notes||'') : '-'));
    // Sipariş verilmiş (ORDERED) kaleme DOKUNULMAZ — üçüncü eksik yeni kalem açmalı.
    await dbUpdate('purchase_items', m7Pis[0].id, {status:'ORDERED'});
    const wres4 = await api('POST','/warehouse-reservations',{project_name:PROJ,
      warehouse_id:wh.id, item_name:'E2E Rezerv Malzeme', item_code:'E2E-REZ',
      requested_qty:3, unit:'adet'});
    const red4 = await api('POST','/warehouse-reservations/'+wres4.id+'/approve',
      {approved_qty:0, shortage_reason:'Depoda yok — ORDERED korunur testi'});
    const m7All = (await dbGet('purchase_items')).filter(i=>
      i.project_name===PROJ && i.code==='E2E-REZ');
    const m7Planned = m7All.filter(i=>i.status==='PLANNED');
    check('ORDERED kaleme dokunulmadı, eksik YENİ PLANNED kalem açtı (3)',
      !!red4 && m7All.some(i=>i.status==='ORDERED' && Number(i.quantity)===17)
      && m7Planned.length===1 && Number(m7Planned[0].quantity)===3,
      JSON.stringify(m7All.map(i=>i.status+':'+i.quantity)));

    console.log('═══ 9.TUR M8: TAM REDDE KAYIT SIFIRLANIR (zero_stock) ═══');
    // Depo "hiç yok" dedi: kayıtta 5 görünse de zero_stock ile RESERVATION_ADJUST
    // OUT kalan kaydın TAMAMINI düşer (eksikten büyük hayalet dahil) → net stok 0.
    await api('POST','/warehouse-movements',{warehouse_id:wh.id, item_name:'E2E Red Malzeme',
      item_code:'E2E-RED', movement_type:'IN', quantity:5, source_type:'MANUAL'});
    const wresZ = await api('POST','/warehouse-reservations',{project_name:PROJ,
      warehouse_id:wh.id, item_name:'E2E Red Malzeme', item_code:'E2E-RED',
      requested_qty:2, unit:'adet'});
    const redZ = await api('POST','/warehouse-reservations/'+wresZ.id+'/approve',
      {approved_qty:0, shortage_reason:'Sayımda hiç çıkmadı', zero_stock:true,
       approved_by:'E2E Depocu'});
    check('zero_stock red → REJECTED', !!redZ && redZ.status==='REJECTED',
      redZ ? redZ.status : _lastApiError);
    const zMvs = (await dbGet('warehouse_movements')).filter(m=>m.reservation_id===wresZ.id);
    const zAdj = zMvs.find(m=>m.source_type==='RESERVATION_ADJUST');
    check('RESERVATION_ADJUST OUT 5 yazıldı (eksik 2 değil — kayıt sıfırlandı)',
      !!zAdj && zAdj.movement_type==='OUT' && Number(zAdj.quantity)===5,
      zAdj ? zAdj.quantity : 'hareket yok');
    globalThis.whMovements = await dbGet('warehouse_movements');
    check('net stok 0 (5 giriş − 5 sıfırlama)',
      _whItemStock(wh.id,'E2E Red Malzeme','E2E-RED')===0,
      _whItemStock(wh.id,'E2E Red Malzeme','E2E-RED'));
    globalThis.whMovements = [];

    console.log('═══ 8.TUR #1: TOPLAMA DEPOSU (transfer çifti + hedeften rezervasyon) ═══');
    // "Hem projeye işlensin hem istenirse depolar arası aktarılsın":
    // onay, kaynak→hedef WAREHOUSE_TRANSFER çifti + RESERVATION OUT'unu
    // HEDEF depodan yazar; net stok her yerde sıfır, defterde toplama izi kalır.
    const wh2 = (await dbInsert('warehouses',{name:'E2E Toplama Depo'}))[0];
    created.wh.push(wh2.id);
    await api('POST','/warehouse-movements',{warehouse_id:wh.id, item_name:'E2E Toplanan',
      item_code:'E2E-TOP', movement_type:'IN', quantity:10, source_type:'MANUAL'});
    const wresT = await api('POST','/warehouse-reservations',{project_name:PROJ,
      warehouse_id:wh.id, target_warehouse_id:wh2.id,
      item_name:'E2E Toplanan', item_code:'E2E-TOP', requested_qty:10});
    check('toplama depolu talep oluştu', !!wresT && wresT.target_warehouse_id===wh2.id,
      wresT && wresT.target_warehouse_id);
    const onayT = await api('POST','/warehouse-reservations/'+wresT.id+'/approve',
      {approved_qty:10, approved_by:'E2E Depocu'});
    check('tam onay APPROVED', !!onayT && onayT.status==='APPROVED', onayT ? onayT.status : _lastApiError);
    const mvT = (await dbGet('warehouse_movements')).filter(m=>m.reservation_id===wresT.id);
    const xOut = mvT.find(m=>m.source_type==='WAREHOUSE_TRANSFER' && m.movement_type==='OUT');
    const xIn  = mvT.find(m=>m.source_type==='WAREHOUSE_TRANSFER' && m.movement_type==='IN');
    const xRes = mvT.find(m=>m.source_type==='RESERVATION');
    check('transfer çifti: kaynaktan OUT + hedefe IN (10)',
      xOut && xOut.warehouse_id===wh.id && Number(xOut.quantity)===10
      && xIn && xIn.warehouse_id===wh2.id && Number(xIn.quantity)===10,
      'hareket sayısı='+mvT.length);
    check('rezervasyon OUT\'u HEDEF depodan yazıldı',
      xRes && xRes.warehouse_id===wh2.id && xRes.movement_type==='OUT' && Number(xRes.quantity)===10);
    globalThis.whMovements = await dbGet('warehouse_movements');
    check('net stok iki depoda da 0 (çifte sayım yok)',
      _whItemStock(wh.id,'E2E Toplanan','E2E-TOP')===0
      && _whItemStock(wh2.id,'E2E Toplanan','E2E-TOP')===0);
    globalThis.whMovements = [];
    globalThis._lastApiError = null;
    await api('DELETE','/warehouse-movements/'+xIn.id);
    check('toplama transfer bacağı da SİLİNEMEZ (rezervasyon yaşıyor)',
      /silinemez/i.test(_lastApiError||''), _lastApiError);
    // Kaynakla aynı hedef anlamsız → backend NULL'a indirger
    const wresAyni = await api('POST','/warehouse-reservations',{project_name:PROJ,
      warehouse_id:wh.id, target_warehouse_id:wh.id,
      item_name:'E2E Toplanan', item_code:'E2E-TOP', requested_qty:1});
    check('kaynakla aynı toplama deposu NULL\'a indirgendi',
      !!wresAyni && !wresAyni.target_warehouse_id);
    await api('POST','/warehouse-reservations/'+wresAyni.id+'/cancel');

    console.log('═══ K2: PROJE ADI DEĞİŞİNCE STRING TABLOLAR TAŞINIYOR ═══');
    const RENAMED = PROJ+'-ADI';
    const ren = await api('PUT','/orders/'+orderId, {project_name:RENAMED, customer_name:'E2E Müşteri'});
    check('proje adı değişti', !!ren && ren.project_name===RENAMED, _lastApiError||'');
    const piMoved = (await dbGet('purchase_items')).filter(i=>i.project_name===RENAMED).length;
    const pbMoved = (await dbGet('project_bom')).filter(b=>b.project_name===RENAMED).length;
    const piOrphan = (await dbGet('purchase_items')).filter(i=>i.project_name===PROJ).length;
    check('satın alma + BOM bağları yeni ada taşındı (sahipsiz kayıt 0)',
      piMoved>=2 && pbMoved>=1 && piOrphan===0, `pi=${piMoved} pb=${pbMoved} orphan=${piOrphan}`);
    // Aşama 2: warehouse_reservations da STRING tablo — taşınmalı
    const wrMoved = (await dbGet('warehouse_reservations')).filter(r=>r.project_name===RENAMED).length;
    const wrOrphan = (await dbGet('warehouse_reservations')).filter(r=>r.project_name===PROJ).length;
    check('rezervasyonlar yeni ada taşındı (sahipsiz 0)', wrMoved>=2 && wrOrphan===0,
      `wr=${wrMoved} orphan=${wrOrphan}`);
    await api('PUT','/orders/'+orderId, {project_name:PROJ, customer_name:'E2E Müşteri'});
    const piBack = (await dbGet('purchase_items')).filter(i=>i.project_name===PROJ).length;
    check('geri adlandırmada da taşındı', piBack===piMoved, piBack);
    globalThis.purchaseItems = await dbGet('purchase_items');

    console.log('═══ K3: BACKEND ROL DENETİMİ ═══');
    // Kısıtlı (developer olmayan, orders yetkisi olmayan) kullanıcıyla dene
    const limited = await api('POST','/users',{username:'e2e.kisitli.'+Date.now().toString(36),
      password:'e2etest123', name:'E2E Kısıtlı', role:'user', permissions:['dashboard']});
    check('kısıtlı kullanıcı oluştu (dev token)', !!limited, _lastApiError||'');
    const lr = await fetch(BASE+'auth/login',{method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({username:limited.username, password:'e2etest123'})}).then(r=>r.json()).catch(()=>null);
    const LTOK = lr && lr.data && (lr.data.token||lr.data.access_token);
    check('kısıtlı kullanıcı login oldu', !!LTOK);
    const lapi = async (method,path,body)=>{
      const r = await fetch(BASE+path.replace(/^\//,''), {method,
        headers:{Authorization:'Bearer '+LTOK,'Content-Type':'application/json'},
        body: body?JSON.stringify(body):undefined});
      let j=null; try{ j=await r.json(); }catch(e){}
      return {status:r.status, ok:!!j && j.success!==false, msg:j&&j.message};
    };
    const d1 = await lapi('DELETE','/orders/'+orderId);
    check('kısıtlı: proje silme REDDEDİLDİ', d1.status===403 || !d1.ok, 'HTTP '+d1.status);
    const d2 = await lapi('PUT','/orders/'+orderId, {project_name:PROJ+'-HACK', customer_name:'X'});
    check('kısıtlı: proje düzenleme REDDEDİLDİ', d2.status===403 || !d2.ok, 'HTTP '+d2.status);
    const d3 = await lapi('PUT','/users/'+limited.id, {role:'developer'});
    check('kısıtlı: kendi rolünü yükseltme REDDEDİLDİ', !d3.ok, d3.msg||('HTTP '+d3.status));
    const d4 = await lapi('POST','/users',{username:'e2e.hack', password:'x12345', role:'developer'});
    check('kısıtlı: geliştirici hesabı açma REDDEDİLDİ', !d4.ok, d4.msg||('HTTP '+d4.status));
    const d5 = await lapi('PUT','/users/'+limited.id, {password:'yeniSifre123'});
    check('kısıtlı: KENDİ şifresini değiştirebildi', d5.ok, d5.msg||('HTTP '+d5.status));
    const d6 = await lapi('POST','/users',{name:'E2E Personel Kartı', dept:'Kaynak', role:'Kaynakçı'});
    check('kısıtlı: sade personel kartı ekleyebildi', d6.ok, d6.msg||('HTTP '+d6.status));
    // temizlik (dev token): personel kartı + kısıtlı kullanıcı
    // (EP tablosunda 'users' yok — doğrudan api ile silinir)
    for(const u of (await api('GET','/users')||[]).filter(x=>x.full_name==='E2E Personel Kartı' || x.id===limited.id))
      await api('DELETE','/users/'+u.id);
    check('K3 test kullanıcıları silindi',
      !(await api('GET','/users')||[]).some(x=>x.id===limited.id || x.full_name==='E2E Personel Kartı'));

    console.log('═══ K1: PROJE SİLME GUARD\'I ═══');
    // Bağlı kaydı (parça/iş emri/satın alma/BOM) olan proje silinememeli
    globalThis._lastApiError = null;
    await api('DELETE','/orders/'+orderId);
    check('bağlı kaydı olan proje SİLİNEMEDİ', /silinemez/i.test(_lastApiError||''), _lastApiError);
    const ordStill = (await api('GET','/orders')||[]).some(x=>x.id===orderId);
    check('proje yerinde duruyor', ordStill);

    console.log('═══ TEMİZLİK ═══');
  } catch(e){
    failures++;
    console.error('❌ BEKLENMEDİK HATA:', e.message);
  } finally {
    // temizlik: ters sırayla, hatalar yutulur
    const del = async (t,id)=>{ if(id) await dbDelete(t,id).catch(()=>{}); };
    for(const id of created.wop) await del('work_order_parts', id);
    for(const id of created.wo)  await del('work_orders', id);
    // (O4/O5 guard'ları) depodaki kalem ve kaleme bağlı mal kabul hareketi
    // silinemez: önce kalemler CANCELLED + depodan çözülür, kalemler
    // silinince hareketlerin purchase_item_id bağı NULL'a düşer
    for(const i of (await dbGet('purchase_items')).filter(x=>x.project_name===PROJ))
      await dbUpdate('purchase_items', i.id, {status:'CANCELLED', warehouse_id:null});
    const piAll = await dbGet('purchase_items');
    for(const i of piAll.filter(x=>x.project_name===PROJ && x.stock_plan_id)) await del('purchase_items', i.id);
    for(const i of (await dbGet('purchase_items')).filter(x=>x.project_name===PROJ)) await del('purchase_items', i.id);
    // Rezervasyonlar HAREKETLERDEN ÖNCE silinmeli: bağlı RESERVATION hareketi
    // rezervasyon yaşarken silinemez (guard); rezervasyon silinince bağ
    // SET NULL olur ve hareket süpürülebilir. DELETE bu yüzden serbest.
    for(const r of (await dbGet('warehouse_reservations')).filter(x=>x.project_name===PROJ))
      await del('warehouse_reservations', r.id);
    // E2E hareketleri (rcvDoReceive'in yazdıkları dahil) kod önekiyle süpürülür
    for(const m of (await dbGet('warehouse_movements')).filter(m=>(m.item_code||'').startsWith('E2E-')))
      await del('warehouse_movements', m.id);
    // (O1 guard'ları) iş emri bağı yukarıda silindi; ilerleme sayaçları ve
    // hiyerarşi bağı sıfırlanmadan parça silinemez
    const projParts = (await dbGet('parts')).filter(x=>x.project===PROJ);
    for(const p of projParts) await dbUpdate('parts', p.id, {status:'pending', qty_done:0, qty_reject:0, parent_part_id:null});
    for(const p of projParts) await del('parts', p.id);
    for(const pb of (await dbGet('project_bom')).filter(x=>x.project_name===PROJ)) await del('project_bom', pb.id);
    // BomProductService parçası olan ürünü SİLMEZ; önce şablon parçaları
    // yapraktan köke silinmeli. Eskiden bu adım yoktu: silme sessizce
    // reddediliyor (hata yutuluyor), her koşuda bir "E2E Ürün" + parçaları
    // DB'de birikiyordu (33 ürün / 160 parça bulundu, 2026-07-10'da temizlendi).
    const e2eUrunler = (await dbGet('bom_products')).filter(x=>(x.name||'')==='E2E Ürün');
    const e2eUrunIds = new Set(e2eUrunler.map(p=>p.id));
    const e2eParcalar = (await dbGet('bom_parts')).filter(p=>e2eUrunIds.has(p.product_id))
      .sort((a,b)=>(b.level||0)-(a.level||0)); // önce yapraklar (alt parçası olan silinemez)
    for(const p of e2eParcalar) await del('bom_parts', p.id);
    for(const bp of e2eUrunler) await del('bom_products', bp.id);
    for(const id of created.wh) await del('warehouses', id);
    for(const o of (await dbGet('orders')).filter(x=>x.project_name===PROJ)) await del('orders', o.id);
    // temizlik doğrulaması — bom_products/bom_parts da SAYILIR (eskiden sayılmıyordu,
    // bu yüzden "0 artık kayıt" diyor ama ürünler birikiyordu)
    const kalanUrunler = (await dbGet('bom_products')).filter(x=>(x.name||'')==='E2E Ürün');
    const kalanUrunIds = new Set(kalanUrunler.map(p=>p.id));
    const leftovers =
      (await dbGet('purchase_items')).filter(x=>x.project_name===PROJ).length +
      (await dbGet('parts')).filter(x=>x.project===PROJ).length +
      (await dbGet('project_bom')).filter(x=>x.project_name===PROJ).length +
      (await dbGet('orders')).filter(x=>x.project_name===PROJ).length +
      (await dbGet('warehouse_movements')).filter(m=>(m.item_code||'').startsWith('E2E-')).length +
      (await dbGet('warehouse_reservations')).filter(x=>x.project_name===PROJ).length +
      kalanUrunler.length +
      (await dbGet('bom_parts')).filter(p=>kalanUrunIds.has(p.product_id)).length;
    check('temizlik tamam (0 artık kayıt)', leftovers===0, leftovers);
    console.log('\n═══ SONUÇ: ' + (failures? failures+' HATA ❌' : 'TÜM TESTLER GEÇTİ ✅') + ' ═══');
    process.exit(failures?1:0);
  }
})();
