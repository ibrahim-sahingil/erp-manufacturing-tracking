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
  let i=html.indexOf('{',start),d=1;i++;
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

const store={};
global.document={ getElementById:id=>({ set innerHTML(v){store[id]=String(v);}, get innerHTML(){return store[id]||'';}, value:'', addEventListener(){}, style:{} }) };
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

console.log(fail?`\n${fail} HATA ❌`:'\nTÜM RENDER GÜVENLİK KONTROLLERİ GEÇTİ ✅');
process.exit(fail?1:0);
}
main();
