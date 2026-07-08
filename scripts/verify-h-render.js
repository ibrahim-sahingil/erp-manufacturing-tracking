// h`` tagged-template'e dönüştürülen render fonksiyonlarını bir DOM shim ile
// çalıştırıp kötü veriyi gerçekten kaçırdıklarını doğrular. E2E bu DOM
// fonksiyonlarını çağırmadığından (confirm/innerHTML), dönüşüm regresyonlarını
// yakalayan tek otomatik güvence budur. Yeni fonksiyon dönüştükçe buraya
// senaryo eklenir. Kullanım: node scripts/verify-h-render.js
const fs = require('fs');
const path = require('path');
const html = fs.readFileSync(path.join(__dirname,'..','src','main','resources','static','index.html'),'utf8');
function grab(name){
  const marker='function '+name; const start=html.indexOf(marker);
  if(start<0) throw new Error(name+' bulunamadı');
  // Parametre parantezini atla — destructuring parametrenin '{'sı gövde sanılmasın
  let i=html.indexOf('(',start),pd=1;i++;
  while(pd>0){const c=html[i];if(c==='(')pd++;if(c===')')pd--;i++;}
  i=html.indexOf('{',i);
  let d=1;i++;
  while(d>0){const c=html[i];if(c==='{')d++;if(c==='}')d--;i++;}
  const asyncPrefix = html.slice(Math.max(0,start-6),start)==='async ' ? 'async ' : '';
  return asyncPrefix + html.slice(start,i);
}
// h`` mekanizması — index.html'deki esc/raw/_hval/h ile AYNI olmalı
global.ea = s => (s==null?'':String(s)).replace(/\\/g,'\\\\').replace(/'/g,"\\'").replace(/"/g,'&quot;');
global.esc = s => String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
global.raw = s => { const r=new String(s==null?'':String(s)); r.__html=true; return r; };
global._hval = v => (v==null||v===false||v===true)?'':(v&&v.__html)?v.toString():Array.isArray(v)?v.map(_hval).join(''):esc(v);
global.h = (strings,...vals)=>{ let out=strings[0]; for(let i=0;i<vals.length;i++) out+=_hval(vals[i])+strings[i+1]; return raw(out); };
// Temizlik turu helper'ları — render fonksiyonları içinden çağrılıyor
eval(grab('INP'));

const store={};
// Elementler id başına önbelleklenir ki test .value atayabilsin (stat-start vb.)
const els={};
global.window = global;
global.document={ getElementById:id=> els[id] || (els[id]={ value:'', addEventListener(){}, style:{}, textContent:'', scrollIntoView(){}, classList:{add(){},remove(){}}, set innerHTML(v){store[id]=String(v);}, get innerHTML(){return store[id]||'';} }) };
global.parts=[];
const EVIL='<img src=x onerror=alert(1)>';

let fail=0;
const chk=(n,c)=>{ console.log((c?'  ✅':'  ❌ FAIL')+' '+n); if(!c)fail++; };

async function main(){

// ── renderSuppliersModalList + renderDeptList ──
global.suppliers=[{id:'s1', name:'Ac<b>me "X" & Co', contact_person:EVIL, phone:'5\'55', email:'a@b', notes:'not<script>', is_active:true},
                  {id:'s2', name:'Pasif', is_active:false}];
global.depts=[{id:'d1', project:'Proj<X>', name:'Kaynak<img>'}];
global.STANDARD_DEPTS=[];
eval(grab('renderSuppliersModalList'));
eval(grab('renderDeptList'));
renderSuppliersModalList();
renderDeptList();
const sup=store['sup-modal-list']||'', dep=store['dept-list']||'';
console.log('renderSuppliersModalList / renderDeptList:');
chk('suppliers: onerror atağı kaçırıldı (ham <img yok)', !sup.includes(EVIL) && sup.includes('&lt;img src=x onerror'));
chk('suppliers: ad < > & " kaçırıldı', sup.includes('Ac&lt;b&gt;me &quot;X&quot; &amp; Co'));
chk('suppliers: pasif rozeti (raw) korundu', sup.includes('(pasif)'));
chk('suppliers: notes <script> kaçırıldı', sup.includes('not&lt;script&gt;') && !sup.includes('not<script>'));
chk('depts: proje/ad kaçırıldı', dep.includes('Proj&lt;X&gt;') && dep.includes('Kaynak&lt;img&gt;'));
chk('depts: kart yapısı (raw HTML) korundu', dep.includes('<div style="background:var(--surface2)') && dep.includes('deleteDept('));

// ── renderScan (QR tarama — herkese açık, en yüksek risk) ──
global.dbGet = async (t)=> t==='parts'
  ? [{id:'p1', name:'Parça'+EVIL, code:'C&<D>', drawing:'"Resim"', department:'Böl<b>', material:'Mat<i>', project:'Prj<script>alert(1)</script>', status:'pending', qty:5, created_at:'2026-01-01T10:00:00', description:'Açıklama '+EVIL}]
  : t==='users' ? [{name:'Kişi<b>', dept:'D<i>'}] : [];
global.loadLogs = async ()=> [{created_at:'2026-01-01T09:00:00', username:'Kayıtçı'+EVIL, qty_done:2, qty_reject:1, note:'not<script>x</script>'}];
global.autoSelectScanUser = ()=>{};
eval(grab('renderScan'));
await renderScan('p1');
const scan=store['scan-content']||'';
console.log('\nrenderScan (QR ekranı):');
chk('scan: parça adı onerror atağı kaçırıldı', !scan.includes('Parça'+EVIL) && scan.includes('Parça&lt;img'));
chk('scan: proje <script> kaçırıldı', scan.includes('Prj&lt;script&gt;') && !scan.includes('Prj<script>'));
chk('scan: açıklama onerror kaçırıldı', scan.includes('Açıklama &lt;img'));
chk('scan: log kullanıcı adı atağı kaçırıldı', scan.includes('Kayıtçı&lt;img') && !scan.includes('Kayıtçı'+EVIL));
chk('scan: log notu <script> kaçırıldı', scan.includes('not&lt;script&gt;'));
chk('scan: kod & < > kaçırıldı', scan.includes('C&amp;&lt;D&gt;'));
chk('scan: user option value kaçırıldı', scan.includes('<option value="Kişi&lt;b&gt;">'));
chk('scan: kart/buton yapısı (raw) korundu', scan.includes('class="scan-card"') && scan.includes("submitScan('p1')"));
chk('scan: log badge (raw) korundu', scan.includes('log-qty-badge log-qty-done') && scan.includes('✅ 2'));

// ── renderReceive (QR mal kabul) ──
global.PUR_STATUS={PLANNED:{icon:'📝',label:'Planlandı'}, IN_WAREHOUSE:{icon:'🏭',label:'Depoda'}, CANCELLED:{icon:'✖',label:'İptal'}};
global.dbGet = async (t)=> t==='purchase_items'
  ? [{id:'i1', name:'Kalem'+EVIL, project_name:'Prj<b>', code:'K&<D>', quantity:3, unit:'ad<i>', material:'Mat<script>', supplier:'Ted'+EVIL, status:'PLANNED'}]
  : t==='warehouses' ? [{id:'w1', name:'Depo<b>', location:'Kat<i>', is_active:true}] : [];
eval(grab('whOptionsHTML')); // temizlik turu: depo option helper'ı (renderReceive kullanır)
eval(grab('renderReceive'));
await renderReceive('i1');
const rcv=store['scan-content']||'';
console.log('\nrenderReceive (QR mal kabul):');
chk('receive: kalem adı onerror kaçırıldı', !rcv.includes('Kalem'+EVIL) && rcv.includes('Kalem&lt;img'));
chk('receive: tedarikçi onerror kaçırıldı', rcv.includes('Ted&lt;img'));
chk('receive: malzeme <script> kaçırıldı', rcv.includes('Mat&lt;script&gt;'));
chk('receive: depo option adı kaçırıldı', rcv.includes('<option value="w1">Depo&lt;b&gt;'));
chk('receive: yapı/buton (raw) korundu', rcv.includes('📦 QR Mal Kabul') && rcv.includes("receiveConfirm('i1')"));

// ── renderOrders (sipariş kartları) ──
global.currentUser={role:'developer'};
global.users=[{name:'Onay<b>', dept:'D<i>'}];
global.isPinned=()=>false;
global.purFmtMoney=n=>String(n);
global.orders=[];
global.purchaseItems=[];
const _fakeOrder={id:'o1', project_name:'Prj'+EVIL, customer_name:'Müş<b>',
  customer_email:'a@b<i>', customer_phone:'555<script>', location:'Loc<img>', approved_by:'Ap<b>',
  notes:'Not'+EVIL, price:100, currency:'TRY', status:'active',
  items:[{name:'İt'+EVIL, desc:'Ds<script>x', qty:2}], created_at:'2026-01-01'};
global.dbGet=async(t)=> t==='orders'?[_fakeOrder] : [];
eval(grab('loadOrders'));   // gerçek: orders = dbGet('orders') → sahte order
eval(grab('renderOrders'));
await renderOrders();
const ord=store['orders-list']||'', appr=store['o-approved']||'';
console.log('\nrenderOrders (sipariş kartları):');
chk('orders: proje adı onerror kaçırıldı', ord.includes('Prj&lt;img') && !ord.includes('Prj'+EVIL));
chk('orders: müşteri e-posta/telefon kaçırıldı', ord.includes('a@b&lt;i&gt;') && ord.includes('555&lt;script&gt;'));
chk('orders: kalem adı/açıklama <script> kaçırıldı', ord.includes('İt&lt;img') && ord.includes('Ds&lt;script&gt;x'));
chk('orders: not kaçırıldı', ord.includes('Not&lt;img'));
chk('orders: onaylayan option kaçırıldı', appr.includes('Onay&lt;b&gt;'));
chk('orders: kart yapısı/buton (raw) korundu', ord.includes('class="order-card') && ord.includes("editOrder('o1')") && ord.includes("togglePin('order','o1'"));

// ── renderReceiving (mal kabul) ──
global._rcvActiveProj='PROJ'; // grup açık → satırlar + bulkBar render olsun
global.whName=()=>'Depo<i>';
global.purchaseItems=[];
global.dbGet=async(t)=> t==='purchase_items'?[{id:'r1', project_name:'PROJ', status:'ORDERED',
  code:'K'+EVIL, name:'İsim'+EVIL, unit:'ad<i>', supplier:'Ted<script>', quantity:2, returned_qty:0}] : [];
eval(grab('renderReceiving'));
await renderReceiving();
const rcvv=store['rcv-list']||'';
console.log('\nrenderReceiving (mal kabul):');
chk('receiving: kod/isim onerror kaçırıldı', rcvv.includes('K&lt;img') && rcvv.includes('İsim&lt;img') && !rcvv.includes('İsim'+EVIL));
chk('receiving: tedarikçi/birim <script> kaçırıldı', rcvv.includes('Ted&lt;script&gt;') && rcvv.includes('ad&lt;i&gt;'));
chk('receiving: bulkBar JS onclick (raw) bozulmadı', rcvv.includes("querySelectorAll('.rcv-chk').forEach"));
chk('receiving: satır butonları/checkbox (raw) korundu', rcvv.includes("rcvReceiveModal('r1')") && rcvv.includes('class="rcv-chk"'));

// ── whRow (depo satır helper — renderWarehouseList tarafından kullanılır) ──
global.PUR_STATUS={IN_WAREHOUSE:{icon:'🏭',label:'Depoda',color:'#2ecc71'}, PLANNED:{icon:'📝',label:'Pl',color:'#f5a623'}};
global.whName=()=>'Depo<i>';
eval(grab('whRow'));
const wr=String(whRow({code:'K'+EVIL, name:'Ad'+EVIL, status:'IN_WAREHOUSE', warehouse_id:'w1',
  project_name:'Prj<b>', material:'Mat<script>', supplier:'Ted'+EVIL, quantity:2, unit:'ad<i>', returned_qty:0},
  '<button onclick="whUndo(1)">Geri</button>'));
console.log('\nwhRow (depo satırı):');
chk('whRow: kod/ad onerror kaçırıldı', wr.includes('K&lt;img') && wr.includes('Ad&lt;img') && !wr.includes('Ad'+EVIL));
chk('whRow: malzeme/tedarikçi kaçırıldı', wr.includes('Mat&lt;script&gt;') && wr.includes('Ted&lt;img'));
chk('whRow: proje/depo adı kaçırıldı', wr.includes('Prj&lt;b&gt;') && wr.includes('Depo&lt;i&gt;'));
chk('whRow: actionsHTML (raw) korundu', wr.includes('<button onclick="whUndo(1)">Geri</button>'));

// ── renderPurchaseList (satın alma kalemleri) ──
global.PUR_ACTIONS={PLANNED:[['ORDERED','📦 Sipariş Ver']]};
global.purSelected=new Set();
global.purchaseOrders=[];
global.updatePurSelectBar=()=>{};
global.purchaseItems=[{id:'p1', status:'PLANNED', code:'K'+EVIL, name:'Ad'+EVIL, project_name:'Prj<b>',
  unit:'ad<i>', material:'Mat<script>', supplier:'Ted'+EVIL, notes:'Not'+EVIL, quantity:2,
  unit_price:null, expected_date:null, needs_planning:false}];
eval(grab('renderPurchaseList'));
renderPurchaseList();
const pl=store['pur-list']||'';
console.log('\nrenderPurchaseList (satın alma):');
chk('purchase: kod/ad onerror kaçırıldı', pl.includes('K&lt;img') && pl.includes('Ad&lt;img') && !pl.includes('Ad'+EVIL));
chk('purchase: proje/malzeme/tedarikçi kaçırıldı', pl.includes('Prj&lt;b&gt;') && pl.includes('Mat&lt;script&gt;') && pl.includes('Ted&lt;img'));
chk('purchase: not kaçırıldı', pl.includes('Not&lt;img'));
chk('purchase: actions/butonlar (raw) korundu', pl.includes("purSetStatus('p1','ORDERED')") && pl.includes("deletePurchaseItem('p1')") && pl.includes("purToggleSel('p1'"));

// ── renderPurchaseOrders (toplu sipariş grupları) ──
global.PO_STATUS={DRAFT:{icon:'📝',label:'Taslak',color:'#f5a623'}, ORDERED:{icon:'📦',label:'Sipariş',color:'#2980b9'}};
global.poQuotes=[{id:'q1', purchase_order_id:'po1', supplier_name:'Firma'+EVIL, contact_info:'İlet<b>',
  notes:'QNot<script>', rejection_reason:'Red<img>', total_price:null, currency:'TRY', delivery_date:null}];
global.purchaseItems=[{id:'i1', purchase_order_id:'po1', code:'K'+EVIL, name:'Ad'+EVIL, project_name:'Prj<b>',
  unit:'ad<i>', quantity:2, unit_price:null, currency:'TRY', status:'PLANNED'}];
global.purchaseOrders=[{id:'po1', name:'Grup'+EVIL, status:'DRAFT', selected_quote_id:null}];
eval(grab('renderPurchaseOrders'));
renderPurchaseOrders();
const po=store['po-list']||'';
console.log('\nrenderPurchaseOrders (sipariş grupları):');
chk('po: grup adı onerror kaçırıldı', po.includes('Grup&lt;img') && !po.includes('Grup'+EVIL));
chk('po: üye kalem kod/ad/proje kaçırıldı', po.includes('K&lt;img') && po.includes('Ad&lt;img') && po.includes('Prj&lt;b&gt;'));
chk('po: teklif firma/iletişim kaçırıldı', po.includes('Firma&lt;img') && po.includes('İlet&lt;b&gt;'));
chk('po: teklif not/red gerekçesi <script> kaçırıldı', po.includes('QNot&lt;script&gt;') && po.includes('Red&lt;img'));
chk('po: butonlar (raw) korundu', po.includes("poDeleteQuote('q1')") && po.includes("poRemoveItem('i1')") && po.includes("poAddQuote('po1')"));

// ── renderWorkOrders (iş emirleri) ──
global.workOrders=[{id:'w1', project_name:'Prj<b>', status:'planned', assigned_user:'Kişi'+EVIL,
  workspace_name:'WS<script>', notes:'Not'+EVIL, start_datetime:null, department_name:'Dep<i>'}];
global.orders=[];
global.workOrderParts=[{work_order_id:'w1', part_id:'p1', part_name:'PAd'+EVIL, part_code:'PK<b>', qty:2}];
global.parts=[{id:'p1', status:'pending'}];
global.WO_STATUS={planned:{css:'wos-planned',lbl:'Planlı'}};
global.statusBadge=()=>'<span class="badge">x</span>';
global._woPrintSelected=new Set();
global.dbGet=async(t)=> t==='work_order_revisions'?[{id:'rv1', work_order_id:'w1', field_changed:'Alan<b>',
  new_value:'Yeni<script>', revised_by:'Rev<img>', reason:'Sebep'+EVIL, created_at:'2026-01-01'}] : [];
eval(grab('renderWorkOrders'));
await renderWorkOrders();
const wo=store['wo-list']||'';
console.log('\nrenderWorkOrders (iş emirleri):');
chk('wo: parça adı/kod onerror kaçırıldı', wo.includes('PAd&lt;img') && wo.includes('PK&lt;b&gt;') && !wo.includes('PAd'+EVIL));
chk('wo: personel/çalışma alanı/not kaçırıldı', wo.includes('Kişi&lt;img') && wo.includes('WS&lt;script&gt;') && wo.includes('Not&lt;img'));
chk('wo: revize alanları <script> kaçırıldı', wo.includes('Alan&lt;b&gt;') && wo.includes('Yeni&lt;script&gt;') && wo.includes('Sebep&lt;img'));
chk('wo: statusBadge/butonlar (raw) korundu', wo.includes('<span class="badge">x</span>') && wo.includes("deleteWorkOrder('w1')"));
chk('wo: openWoReviseModal ea onclick (tırnak kaçışlı) bozulmadı', wo.includes('openWoReviseModal(') && wo.includes("'w1'"));

// ── renderDnList + renderDnDetail (irsaliye) ──
global.DN_STATUS={DRAFT:{icon:'📝',label:'Taslak',color:'#f5a623'}, SHIPPED:{icon:'🚚',label:'Sevk',color:'#2ecc71'}};
global.deliveryNotes=[{id:'d1', status:'DRAFT', note_no:'IRS-1', recipient_name:'Alıcı'+EVIL, order_id:null,
  city:'İst<b>', district:'Kad<i>', created_by:'Yap<script>', created_at:'2026-01-01', ship_date:null}];
global.deliveryItems=[{id:'di1', delivery_note_id:'d1', item_code:'K'+EVIL, item_name:'Ad'+EVIL, unit:'ad<i>',
  quantity:2, warehouse_id:null, notes:'Not<img>'}];
global.orders=[];
global._dnActive='d1';
eval(grab('renderDnList'));
renderDnList();
const dl=store['dn-list']||'';
console.log('\nrenderDnList / renderDnDetail (irsaliye):');
chk('dn-list: alıcı adı onerror kaçırıldı', dl.includes('Alıcı&lt;img') && !dl.includes('Alıcı'+EVIL));
chk('dn-list: şehir/oluşturan <script> kaçırıldı', dl.includes('İst&lt;b&gt;') && dl.includes('Yap&lt;script&gt;'));
chk('dn-list: actions (raw) korundu', dl.includes("dnShip('d1')") && dl.includes("dnDelete('d1')"));
eval(grab('renderDnDetail'));
renderDnDetail();
const dd=store['dn-detail']||'';
chk('dn-detail: kalem kod/ad/not kaçırıldı', dd.includes('K&lt;img') && dd.includes('Ad&lt;img') && dd.includes('Not&lt;img'));
chk('dn-detail: kalem sil butonu (raw) korundu', dd.includes("dnDeleteItem('di1')"));

// ── deptWoStripHTML (bölüm dashboard'ı iş emri şeridi) ──
global.WO_COLORS={planned:'#f5a623', inprogress:'#2980b9'};
global.WO_STATUS={planned:{lbl:'Planlı'}, inprogress:{lbl:'Devam'}};
global._activeProject='Prj<b>';
global.workOrders=[{id:'w1', project_name:'Prj<b>', department_id:'d1', status:'planned',
  assigned_user:'Kişi'+EVIL, workspace_name:'WS<script>', notes:'WNot'+EVIL, start_datetime:null}];
global.workOrderParts=[{work_order_id:'w1', part_id:'p1'}];
global.parts=[{id:'p1', name:'PAd'+EVIL, code:'PK<b>', drawing:"Çiz'im", status:'pending', qty:5, qty_done:1, qty_reject:0}];
global.partWaitingChildren=()=>[];
global.statusBadge=()=>'<span class="badge">x</span>';
global._dashWoOpen=new Set(['w1']);
eval(grab('woWaitingChildren'));
eval(grab('woStartBlockMsg'));
eval(grab('deptWoStripHTML'));
const dws=String(deptWoStripHTML('d1'));
console.log('\ndeptWoStripHTML (bölüm iş emri şeridi):');
chk('deptWo: personel/alan/not kaçırıldı', dws.includes('Kişi&lt;img') && dws.includes('WS&lt;script&gt;') && dws.includes('WNot&lt;img'));
chk('deptWo: parça adı (başlık + tablo) kaçırıldı', dws.includes('PAd&lt;img') && dws.includes('PK&lt;b&gt;'));
chk('deptWo: showQR onclick ea tırnak kaçışı bozulmadı', dws.includes("showQR('p1'") && dws.includes("Çiz\\'im"));
chk('deptWo: durum butonu / statusBadge (raw) korundu', dws.includes("dashWoSetStatus('w1','inprogress')") && dws.includes('<span class="badge">x</span>'));

// ── renderProjectCards (proje kartları) ──
global.parts=[{project:"Prj'"+EVIL, status:'pending', qty:2, qty_done:0, qty_reject:0, created_at:'2026-01-01'}];
global.isPartAwaitingPlanning=()=>false;
global.isPinned=()=>false;
eval(grab('woBadgeCounts'));
eval(grab('renderProjectCards'));
renderProjectCards();
const pc=store['layer-projects']||'';
console.log('\nrenderProjectCards (proje kartları):');
chk('projectCards: proje adı kaçırıldı', pc.includes("Prj&#39;&lt;img") && !pc.includes('class="project-card-name">Prj\''));
chk('projectCards: openProject onclick ea tırnak kaçışı', pc.includes("openProject('Prj\\'"));
chk('projectCards: kart yapısı (raw) korundu', pc.includes('project-mini-bars') && pc.includes('togglePin('));

// ── renderParts / updatePartsFilters / renderPartsFiltered (parça listesi) ──
global.orders=[{project_name:'OP<b>&'}];
global.depts=[{id:'d1', project:'OP<b>&', name:'Kaynak<img>'}];
global.parts=[{name:'PN<b>', code:'pc1', project:'Pr<j>', department:'Dep<t>', department_id:'d1'}];
global.partRowHTML=(p)=>'<div class="PRH">'+esc(p.name)+'</div>';
eval(grab('renderParts'));
eval(grab('updatePartsFilters'));
eval(grab('renderPartsFiltered'));
renderParts();
console.log('\nrenderParts / renderPartsFiltered (parça listesi):');
chk('parts: sipariş dropdown option kaçırıldı', (store['f-project']||'').includes('<option value="OP&lt;b&gt;&amp;"') && (store['d-project']||'').includes('OP&lt;b&gt;&amp;'));
chk('parts: bölüm filtre option kaçırıldı', (store['parts-dept-filter']||'').includes('Kaynak&lt;img&gt;'));
renderPartsFiltered();
const pfl=store['parts-list']||'';
chk('partsFiltered: proje/bölüm başlıkları kaçırıldı', pfl.includes('📁 Pr&lt;j&gt;') && pfl.includes('🏭 Dep&lt;t&gt;'));
chk('partsFiltered: partRowHTML (raw) korundu', pfl.includes('<div class="PRH">'));

// ── showLog (işlem geçmişi modalı) ──
global.loadLogs=async()=>[{created_at:'2026-01-01T10:00:00', username:'U<b>', qty_done:2, note:'LNot'+EVIL}];
let _ovl='';
global.document.createElement=()=>({ className:'', set innerHTML(v){_ovl=String(v);}, get innerHTML(){return _ovl;} });
global.document.body={appendChild(){}};
eval(grab('showLog'));
await showLog('p1','Ad<script>');
console.log('\nshowLog (işlem geçmişi modalı):');
chk('showLog: parça adı <script> kaçırıldı', _ovl.includes('Ad&lt;script&gt;') && !_ovl.includes('Ad<script>'));
chk('showLog: kullanıcı/not kaçırıldı', _ovl.includes('U&lt;b&gt;') && _ovl.includes('LNot&lt;img'));
chk('showLog: adet rozeti (raw) korundu', _ovl.includes('log-qty-done">✅ 2'));

// ── renderStatsView + loadStats + renderDetailTable (istatistik) ──
global.users=[{name:'Onay<b>', dept:'D<i>', role:'usta'}];
global.parts=[{project:'Pr<j>'}];
eval(grab('renderStatsView'));
renderStatsView();
const sv=store['stats-content']||'';
console.log('\nrenderStatsView / loadStats / renderDetailTable (istatistik):');
chk('statsView: proje/personel option kaçırıldı', sv.includes('<option value="Pr&lt;j&gt;">') && sv.includes('<option value="Onay&lt;b&gt;">Onay&lt;b&gt;</option>'));
global.dbGet=async(t)=> t==='logs'
  ? [{part_id:'p1', username:"Usr'"+EVIL, qty_done:1, qty_pending:0, qty_reject:0, status:'done', created_at:'2026-01-01T10:00:00', note:'DNot<b>'}]
  : t==='parts' ? [{id:'p1', name:'SPN<b>', code:'SPC<i>', project:'SPP<script>'}] : [];
document.getElementById('stat-start').value='2026-01-01';
document.getElementById('stat-end').value='2026-01-31';
document.getElementById('stat-project').value='';
document.getElementById('stat-user').value='';
eval(grab('renderDetailTable'));
eval(grab('loadStats'));
await loadStats();
const sr=store['stats-result']||'', dtw=store['detail-table-wrap']||'';
chk('loadStats: personel kartı adı kaçırıldı', sr.includes("Usr&#39;&lt;img"));
chk('loadStats: filterTableByUser onclick ea tırnak kaçışı', sr.includes("filterTableByUser('Usr\\'"));
chk('detailTable: parça/proje/not kaçırıldı', dtw.includes('SPN&lt;b&gt;') && dtw.includes('SPP&lt;script&gt;') && dtw.includes('DNot&lt;b&gt;'));
chk('detailTable: statusBadge (raw) korundu', dtw.includes('<span class="badge">x</span>'));

// ── renderPdForm + renderPdList (proje tarihleri) ──
global.orders=[{project_name:'PD<b>'}];
global.projectDates=[{id:'pd1', project_name:'PD<b>', start_date:'2026-01-01', end_date:'2026-02-01'}];
global.dbGet=async(t)=> t==='project_date_revisions'
  ? [{id:'rv1', project_date_id:'pd1', old_start:'2026-01-01', old_end:'2026-02-01', new_start:'2026-01-05',
     new_end:'2026-02-05', revised_by:'Rev<img>', reason:'Sebep'+EVIL, created_at:'2026-01-03'}] : [];
eval(grab('renderPdForm'));
eval(grab('renderPdList'));
renderPdForm();
await renderPdList();
const pdf=store['pd-project']||'', pdl=store['pd-list']||'';
console.log('\nrenderPdForm / renderPdList (proje tarihleri):');
chk('pdForm: proje option kaçırıldı', pdf.includes('<option value="PD&lt;b&gt;"'));
chk('pdList: proje adı / revize alanları kaçırıldı', pdl.includes('PD&lt;b&gt;') && pdl.includes('Rev&lt;img&gt;') && pdl.includes('Sebep&lt;img'));
chk('pdList: revize/sil butonları (raw) korundu', pdl.includes("openReviseModal('pd1')") && pdl.includes("deletePd('pd1')"));

// ── woLoadParts (iş emri sihirbazı parça ağacı) ──
global.PUR_STATUS={PLANNED:{icon:'📝',label:'Planlandı',color:'#f5a623'}, IN_WAREHOUSE:{icon:'🏭',label:'Depoda',color:'#2ecc71'}};
global.parts=[{id:'p1', project:'PRJ', name:"Pa'"+EVIL, code:'PC<b>', material:'M<i>', qty:2, department_id:null, parent_part_id:null, drawing:''}];
global.depts=[];
global.projectBoms=[]; global.projectBomParts=[];
global.purchaseItems=[];
global.loadProjectBoms=async()=>{};
global.whName=()=>'';
global.toast=()=>{};
global._woSelectedParts=new Set();
global.dbGet=async(t)=> t==='purchase_items'
  ? [{id:'pu1', project_name:'PRJ', code:'ZZ', name:'Mal<script>', quantity:1, unit:'ad<i>', status:'PLANNED', project_bom_part_id:null}] : [];
document.getElementById('wo-project').value='PRJ';
document.getElementById('wo-dept').value='';
eval(grab('woMatChip'));
eval(grab('woLoadParts'));
await woLoadParts();
const wog=store['wo-parts-grid']||'';
console.log('\nwoLoadParts (iş emri sihirbazı):');
chk('woLoad: parça adı/kod/malzeme kaçırıldı', wog.includes("Pa&#39;&lt;img") && wog.includes('PC&lt;b&gt;') && wog.includes('M&lt;i&gt;'));
chk('woLoad: woTogglePart onclick ea tırnak kaçışı', wog.includes("woTogglePart('p1','Pa\\'"));
chk('woLoad: satın alma satırı kaçırıldı', wog.includes('Mal&lt;script&gt;') && wog.includes('ad&lt;i&gt;'));

// ── whvProjectHTML / whvWarehouseHTML (depo görünümleri) ──
global.PUR_STATUS={IN_WAREHOUSE:{icon:'🏭',label:'Depoda',color:'#2ecc71'}, ORDERED:{icon:'📦',label:'Sipariş',color:'#2980b9'}, PLANNED:{icon:'📝',label:'Pl',color:'#f5a623'}};
global.whName=()=>'Depo<i>';
global.purchaseItems=[{id:'x1', project_name:"Pj'"+EVIL, status:'IN_WAREHOUSE', warehouse_id:'w1',
  code:'C<b>', name:'N'+EVIL, quantity:1, unit:'ad', returned_qty:0}];
global._whvActiveProj="Pj'"+EVIL;
global.whMovements=[{warehouse_id:'w1', purchase_item_id:null, item_code:'LC<b>', item_name:"Ln'"+EVIL,
  unit:'kg<i>', movement_type:'IN', quantity:2}];
eval(grab('whvProjectHTML'));
eval(grab('whvWarehouseHTML'));
const wpv=String(whvProjectHTML());
document.getElementById('whv-wh-sel').value='w1';
const wwv=String(whvWarehouseHTML());
console.log('\nwhvProjectHTML / whvWarehouseHTML (depo görünümleri):');
chk('whvProj: proje başlığı kaçırıldı', wpv.includes("Pj&#39;&lt;img"));
chk('whvProj: whvToggleProj/PDF onclick ea tırnak kaçışı', wpv.includes("whvToggleProj('Pj\\'") && wpv.includes("whvProjectPDF('Pj\\'"));
chk('whvProj: whRow + aksiyonlar (raw) korundu', wpv.includes("whXferItem('x1')") && wpv.includes("whUndo('x1')"));
chk('whvWh: proje grubu / münferit adı kaçırıldı', wwv.includes("Pj&#39;&lt;img") && wwv.includes("Ln&#39;&lt;img") && wwv.includes('LC&lt;b&gt;'));
chk('whvWh: whXferLoose onclick ea tırnak kaçışı', wwv.includes("whXferLoose('w1','Ln\\'"));

// ── renderAppUsers (uygulama kullanıcıları) ──
global.loadAppUsers=async()=>{};
global.appUsers=[{id:'u1', role:'user', username:'usr<b>', display_name:"Dn'"+EVIL,
  permissions:['scan'], is_active:true}];
eval(grab('renderAppUsers'));
await renderAppUsers();
const au=store['appusers-list']||'';
console.log('\nrenderAppUsers (uygulama kullanıcıları):');
chk('appUsers: görünen ad / kullanıcı adı kaçırıldı', au.includes("Dn&#39;&lt;img") && au.includes('@usr&lt;b&gt;'));
chk('appUsers: adminChangePassword onclick ea tırnak kaçışı', au.includes("adminChangePassword('u1','Dn\\'"));
chk('appUsers: aktif/pasif toggle boolean bozulmadı', au.includes("toggleAppUserActive('u1',true)"));
chk('appUsers: yetki rozeti / butonlar (raw) korundu', au.includes('QR Okutma') && au.includes("deleteAppUser('u1')"));

// ── renderBomTreeSvg (ürün ağacı görünümü) ──
eval(grab('dimLabel'));
// 5. tur #4: malzeme formu — form-farkında ölçü etiketleri
global.MATERIAL_FORMS={SAC:'Sac',PROFIL:'Profil',MIL:'Mil',BORU:'Boru',DELRIN:'Delrin',COK_KOMPONENTLI:'Çok Komponentli'};
eval(grab('dimLabel2'));
eval(grab('partDimLabel'));
eval(grab('pbomeDimLabel'));
global.bomParts=[{id:'b1', parent_id:null, name:'BN'+EVIL, code:'BC<b>', quantity:1, unit:'ad<i>',
  material:'M<script>', operations:[{name:'Op<b>', code:'OC<i>', duration_per_unit:null}], sort_order:1}];
global._activeBomProduct={name:'Prod<b>', code:'P<i>', unit:'ad'};
eval(grab('renderBomTreeSvg'));
renderBomTreeSvg();
const bts=store['bom-tree-container']||'';
console.log('\nrenderBomTreeSvg (ürün ağacı):');
chk('bomTree: parça adı/kodu/malzemesi kaçırıldı', bts.includes('BN&lt;img') && bts.includes('BC&lt;b&gt;') && bts.includes('M&lt;script&gt;'));
chk('bomTree: operasyon adı kaçırıldı', bts.includes('⚙Op&lt;b&gt;(OC&lt;i&gt;)'));
chk('bomTree: kök ürün adı kaçırıldı', bts.includes('P&lt;i&gt; — Prod&lt;b&gt;'));

// ── renderProjectBomList (proje BOM bağlantıları) ──
global.orders=[];
global.pbomRenderMachineRows=()=>{};
global.projectBoms=[{id:'pb1', project_name:'PBP<b>', bom_product_id:'bp1', status:'published', published_at:'2026-01-01'}];
global.bomProducts=[{id:'bp1', name:'Ürün'+EVIL, code:'UC<i>'}];
global.projectBomParts=[];
eval(grab('renderPbomDropdowns'));
eval(grab('renderProjectBomList'));
renderProjectBomList();
const pbl=store['project-bom-list']||'';
console.log('\nrenderProjectBomList (proje BOM bağlantıları):');
chk('pbomList: proje başlığı / ürün adı kaçırıldı', pbl.includes('PBP&lt;b&gt;') && pbl.includes('Ürün&lt;img') && pbl.includes('(UC&lt;i&gt;)'));
chk('pbomList: butonlar (raw) korundu', pbl.includes("viewPublishedBom('pb1')") && pbl.includes("openPbomEditor('pb1')") && pbl.includes("deleteProjectBom('pb1')"));

// ── pbomeRenderList + pbomeShowTree (proje BOM editörü) ──
global.kindBadge=()=>'<span class="KB">k</span>';
global.depts=[{id:'d1', name:'Dept<img>'}];
global._pbomeParts=[{id:'q1', parent_custom_id:null, custom_code:'QC<b>', custom_name:'QN'+EVIL,
  custom_qty:1, custom_unit:'ad<i>', custom_material:'QM<script>',
  operations:[{name:'On<b>', code:'Oc<i>'}], dept_id:'d1', bom_part_id:'x', sort_order:1}];
global._activePbomId='pb1';
eval(grab('pbomeRenderList'));
eval(grab('pbomeShowTree'));
pbomeRenderList();
const pel=store['pbome-parts-list']||'';
console.log('\npbomeRenderList / pbomeShowTree (proje BOM editörü):');
chk('pbomeList: kod/ad/malzeme/operasyon kaçırıldı', pel.includes('QC&lt;b&gt;') && pel.includes('QN&lt;img') && pel.includes('QM&lt;script&gt;') && pel.includes('⚙On&lt;b&gt;(Oc&lt;i&gt;)'));
chk('pbomeList: bölüm dropdown/rozeti kaçırıldı', pel.includes('Dept&lt;img&gt;'));
chk('pbomeList: kindBadge + butonlar (raw) korundu', pel.includes('<span class="KB">k</span>') && pel.includes("pbomeEditQty('q1')") && pel.includes("pbomeSetDept('q1'"));
pbomeShowTree();
const pst=store['pbome-tree-container']||'';
chk('pbomeTree: kod/ad ve proje adı kaçırıldı', pst.includes('QC&lt;b&gt;') && pst.includes('QN&lt;img') && pst.includes('PBP&lt;b&gt;'));

// ── renderPbomCustomParts (projeye özel parçalar) ──
global.projectBomParts=[{id:'cp1', project_bom_id:'pb1', bom_part_id:null, parent_custom_id:null,
  custom_code:'CC<b>', custom_name:'CN'+EVIL, custom_qty:1, operations:[], dept_id:null, level:0, sort_order:1}];
eval(grab('renderPbomCustomParts'));
renderPbomCustomParts('pb1');
const cpl=store['pbom-custom-parts-pb1']||'', cps=store['pbom-custom-parent']||'';
console.log('\nrenderPbomCustomParts (projeye özel parçalar):');
chk('customParts: kod/ad kaçırıldı (satır + üst parça seçimi)', cpl.includes('CC&lt;b&gt;') && cpl.includes('CN&lt;img') && cps.includes('CC&lt;b&gt; — CN&lt;img'));
chk('customParts: düzenle/sil butonları (raw) korundu', cpl.includes("openPbomCustomEdit('cp1','pb1')") && cpl.includes("removePbomCustomPart('cp1','pb1')"));

// ── xlsImportRenderList (Excel içe aktarma önizleme) ──
global._xlsImport={rows:[{level_no:"1.1'", code:'XC<b>', name:'XN'+EVIL, quantity:2,
  material:'XM<i>', material_kind:null, level:1, error:null}], checked:new Set(["1.1'"])};
eval(grab('xlsImportRenderList'));
xlsImportRenderList();
const xl=store['xls-import-list']||'';
console.log('\nxlsImportRenderList (Excel önizleme):');
chk('xlsImport: kod/ad/malzeme kaçırıldı', xl.includes('XC&lt;b&gt;') && xl.includes('XN&lt;img') && xl.includes('XM&lt;i&gt;'));
chk('xlsImport: onchange ea tırnak kaçışı', xl.includes("xlsImportToggle('1.1\\'"));

// ── viewPublishedBom (yayınlanan ağaç modalı) ──
global.parts=[];
global.dbGet=async(t)=> t==='project_bom_parts'
  ? [{id:'vp1', project_bom_id:'pb1', parent_custom_id:null, is_excluded:false, bom_part_id:'x',
     custom_code:'VC<b>', custom_name:'VN'+EVIL, custom_qty:1, operations:[], dept_id:null, sort_order:1}] : [];
_ovl='';
eval(grab('viewPublishedBom'));
await viewPublishedBom('pb1');
console.log('\nviewPublishedBom (yayınlanan ağaç):');
chk('viewPbom: proje/ürün adı kaçırıldı', _ovl.includes('PBP&lt;b&gt;') && _ovl.includes('Ürün&lt;img'));
chk('viewPbom: parça kod/ad kaçırıldı', _ovl.includes('VC&lt;b&gt;') && _ovl.includes('VN&lt;img'));

// ── renderMaterialsModalList (malzeme kartoteki — 5. tur #1) ──
global.materials=[{id:'m1', name:'ST<b>37 "X"'+EVIL, is_active:true},{id:'m2', name:'Pasif<i>', is_active:false}];
eval(grab('renderMaterialsModalList'));
renderMaterialsModalList();
const mml=store['mat-modal-list']||'';
console.log('\nrenderMaterialsModalList (malzeme kartoteki):');
chk('materials: ad onerror kaçırıldı', mml.includes('ST&lt;b&gt;37 &quot;X&quot;&lt;img') && !mml.includes('ST<b>37'));
chk('materials: pasif rozeti (raw) + butonlar korundu', mml.includes('(pasif)') && mml.includes("matRename('m1')") && mml.includes("matDelete('m2')"));

// ── searchSelect / ssRenderList (aranabilir combobox — arkadaş isteği #8) ──
global.document.addEventListener=()=>{};
els['xs-ss']={ value:'', dataset:{}, style:{}, contains:()=>false,
  set innerHTML(v){store['xs-ss']=String(v);}, get innerHTML(){return store['xs-ss']||'';} };
global._ss={}; // index.html'de searchSelect'in üstünde const olarak tanımlı
eval(grab('searchSelect'));
eval(grab('ssSet'));
eval(grab('ssClose'));
eval(grab('ssRenderList'));
searchSelect('xs', {items:[{value:'v1', label:'LB'+EVIL},{value:'v2', label:"L2'\"<b>"}], emptyLabel:'— Boş<b> —'});
ssRenderList('xs','');
const ssl=store['xs-list']||'', ssh=store['xs-ss']||'';
console.log('\nsearchSelect (aranabilir combobox):');
chk('ss: host şablonu hidden+text input kurdu (raw id korundu)', ssh.includes('id="xs"') && ssh.includes('id="xs-txt"') && ssh.includes("ssFilter('xs')"));
chk('ss: seçenek etiketi onerror kaçırıldı', ssl.includes('LB&lt;img') && !ssl.includes('LB'+EVIL));
chk('ss: tırnaklı etiket kaçırıldı', ssl.includes('L2&#39;&quot;&lt;b&gt;'));
chk('ss: emptyLabel kaçırıldı + ssPick onmousedown (raw) korundu', ssl.includes('— Boş&lt;b&gt; —') && ssl.includes("ssPick('xs',0)"));

console.log(fail?`\n${fail} HATA ❌`:'\nTÜM RENDER GÜVENLİK KONTROLLERİ GEÇTİ ✅');
process.exit(fail?1:0);
}
main();
