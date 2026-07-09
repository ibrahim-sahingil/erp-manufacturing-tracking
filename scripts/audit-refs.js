// Tek dosya SPA'da en sık ve en sinsi hata sınıfı: KOPUK REFERANS.
// (5. denetim turu — ssRenderList çakışmasının kardeşi olan hataları arar.)
//   1. Aynı id'ye sahip iki HTML elementi  → getElementById ilkini döner,
//      ikincisi ölü; hangisinin yazıldığı görünmez.
//   2. onclick/onchange içinde çağrılan ama HİÇ tanımlanmamış fonksiyon
//      → tıklanınca sessiz ReferenceError, buton çalışmaz.
//   3. Aynı elemente iki kez yazılmış attribute (ör. iki style=) → tarayıcı
//      ikincisini yok sayar, yazan kişi fark etmez.
// Kullanım: node scripts/audit-refs.js   (sunucu GEREKMEZ)
const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, '..', 'src', 'main', 'resources', 'static', 'index.html');
const html = fs.readFileSync(file, 'utf8');
const lineOf = idx => html.slice(0, idx).split('\n').length;

let sorun = 0;

// ── 1. STATİK HTML'de mükerrer id ───────────────────────────────────────────
// Sadece <script> bloğunun DIŞINDAKİ statik işaretleme taranır; JS şablonları
// (h`` / innerHTML) çalışma anında üretildiğinden burada değerlendirilemez.
const scriptStart = html.indexOf('<script>');
const staticHtml = scriptStart > 0 ? html.slice(0, scriptStart) : html;
const idMap = new Map();
for (const m of staticHtml.matchAll(/\sid="([^"]+)"/g)) {
  if (!idMap.has(m[1])) idMap.set(m[1], []);
  idMap.get(m[1]).push(lineOf(m.index));
}
const dupIds = [...idMap.entries()].filter(([, l]) => l.length > 1);
console.log('── 1. Statik HTML\'de mükerrer id ──');
if (dupIds.length) {
  sorun += dupIds.length;
  for (const [id, ls] of dupIds) console.log(`  ❌ id="${id}"  → satır ${ls.join(', ')}`);
} else console.log('  ✅ yok');

// ── 2. Inline handler'da tanımsız fonksiyon ─────────────────────────────────
const defs = new Set();
for (const m of html.matchAll(/(?:async\s+)?function\s+([A-Za-z0-9_$]+)\s*\(/g)) defs.add(m[1]);
for (const m of html.matchAll(/(?:const|let|var)\s+([A-Za-z0-9_$]+)\s*=\s*(?:async\s*)?\(/g)) defs.add(m[1]);
for (const m of html.matchAll(/(?:const|let|var)\s+([A-Za-z0-9_$]+)\s*=\s*(?:async\s+)?function/g)) defs.add(m[1]);

const BUILTIN = new Set(['if','for','while','switch','return','typeof','catch','JSON','Number',
  'String','Array','Object','Math','parseInt','parseFloat','confirm','alert','prompt','fetch',
  'setTimeout','clearTimeout','encodeURIComponent','decodeURIComponent','isNaN','console','Boolean',
  'Date','RegExp','Promise','event','this','function','new','await','delete','void']);

const HANDLER = /\son(?:click|change|input|submit|focus|blur|keydown|keyup|mousedown|mouseup|error|load)\s*=\s*"([^"]*)"/g;
// (?<![.\w$]) → `obj.method()` gibi METOT çağrılarını eleme; sadece serbest
// fonksiyon çağrıları aranır. Ayrıca `${...}` şablon parçaları atlanır:
// bunlar JS tarafından üretilir, statik metinde adı çözülemez.
const CALL = /(?<![.\w$])([A-Za-z_$][A-Za-z0-9_$]*)\s*\(/g;
/** Eşleşme bir JS yorum satırının içinde mi? (belgelerde örnek desen geçiyor) */
function yorumSatiri(idx) {
  const satirBasi = html.lastIndexOf('\n', idx) + 1;
  const bas = html.slice(satirBasi, idx).trimStart();
  return bas.startsWith('//') || bas.startsWith('*');
}
const called = new Map();
for (const m of html.matchAll(HANDLER)) {
  if (yorumSatiri(m.index)) continue;
  const kod = m[1].replace(/\$\{[^}]*\}/g, '');
  for (const c of kod.matchAll(CALL)) {
    const fn = c[1];
    if (!BUILTIN.has(fn) && !called.has(fn)) called.set(fn, lineOf(m.index));
  }
}
const eksik = [...called.entries()].filter(([fn]) => !defs.has(fn));
console.log('\n── 2. Inline handler\'da çağrılan ama tanımsız fonksiyon ──');
if (eksik.length) {
  sorun += eksik.length;
  for (const [fn, l] of eksik) console.log(`  ❌ ${fn}()  → ilk kullanım satır ${l}`);
} else console.log(`  ✅ yok (${defs.size} tanımlı fonksiyon, ${called.size} farklı çağrı)`);

// ── 3. Aynı etikette mükerrer attribute ─────────────────────────────────────
console.log('\n── 3. Aynı etikette mükerrer attribute ──');
const dupAttr = [];
for (const tag of staticHtml.matchAll(/<[a-zA-Z][^>]*>/g)) {
  const attrs = [...tag[0].matchAll(/\s([a-zA-Z-]+)\s*=/g)].map(a => a[1].toLowerCase());
  const görülen = new Set(), tekrar = new Set();
  for (const a of attrs) { if (görülen.has(a)) tekrar.add(a); görülen.add(a); }
  if (tekrar.size) dupAttr.push([lineOf(tag.index), [...tekrar].join(', '), tag[0].slice(0, 70)]);
}
if (dupAttr.length) {
  sorun += dupAttr.length;
  for (const [l, a, snip] of dupAttr) console.log(`  ❌ satır ${l}: mükerrer [${a}]  →  ${snip}...`);
} else console.log('  ✅ yok');

console.log('\n' + '─'.repeat(60));
console.log(sorun ? `❌ Toplam ${sorun} kopuk referans bulgusu.` : '✅ Kopuk referans yok.');
process.exit(sorun ? 1 : 0);
