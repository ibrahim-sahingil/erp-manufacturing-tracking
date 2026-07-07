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
  return html.slice(start,i);
}
// h`` mekanizması — index.html'deki esc/raw/_hval/h ile AYNI olmalı
global.esc = s => String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
global.raw = s => { const r=new String(s==null?'':String(s)); r.__html=true; return r; };
global._hval = v => (v==null||v===false||v===true)?'':(v&&v.__html)?v.toString():Array.isArray(v)?v.map(_hval).join(''):esc(v);
global.h = (strings,...vals)=>{ let out=strings[0]; for(let i=0;i<vals.length;i++) out+=_hval(vals[i])+strings[i+1]; return raw(out); };

const store={};
global.document={ getElementById:id=>({ set innerHTML(v){store[id]=String(v);}, get innerHTML(){return store[id]||'';}, value:'' }) };
global.parts=[];
const EVIL='<img src=x onerror=alert(1)>';

let fail=0;
const chk=(n,c)=>{ console.log((c?'  ✅':'  ❌ FAIL')+' '+n); if(!c)fail++; };

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

console.log(fail?`\n${fail} HATA ❌`:'\nTÜM RENDER GÜVENLİK KONTROLLERİ GEÇTİ ✅');
process.exit(fail?1:0);
