// KONSEPT PARİTE DENETİMİ (tasarım 2026)
// Konsept HTML'ini (file://) ve canlı uygulamayı yan yana açar, eşlenen
// bileşenlerin HESAPLANMIŞ stillerini karşılaştırır. Amaç: "konsept gibi mi?"
// sorusunu göz kararına değil ölçüme bağlamak. Sunucu 8080'de olmalı.
// Kullanım: node scripts/audit-concept-parity.js
const { chromium } = require('@playwright/test');
const { pathToFileURL } = require('url');

const CONCEPT = 'C:/Users/İbrahim/Desktop/ERP proje/01 - Belgeler/tasarim-konsepti-2026-07-14/erp-konsept.html';

// Eşleme: [ad, konsept seçici, uygulama seçici, karşılaştırılacak özellikler]
const MAP = [
  ['gövde',            'body',            'body',            ['font-family','font-size','line-height']],
  ['sayfa H1',         '.phead h1',       '.dash-phead h1',  ['font-size','font-weight','letter-spacing','color']],
  ['sayfa alt-başlık', '.phead .sub',     '.dash-phead .sub',['font-size','color']],
  ['KPI kartı',        '.kpi',            '.stat-card',      ['border-radius','padding','background-color','border-top-color']],
  ['KPI etiketi',      '.kpi .lbl',       '.stat-label',     ['font-size','font-weight','letter-spacing','text-transform','color']],
  ['KPI değeri',       '.kpi .big',       '.stat-value',     ['font-size','font-weight','letter-spacing','line-height','color']],
  ['KPI birimi',       '.kpi .big small', '.stat-value .un', ['font-size','font-weight','color'], 'stats'],
  ['KPI alt satırı',   '.kpi .foot',      '.stat-sub',       ['font-size','color']],
  ['delta rozeti',     '.delta',          '.delta',          ['font-size','font-weight','padding','border-radius'], 'stats'],
  ['durum pili',       '.st',             '.st',             ['font-size','font-weight','padding','border-radius']],
  ['ana buton',        '.hbtn.pri',       '.hbtn.pri',       ['font-size','font-weight','border-radius','padding','background-image']],
  ['kart başlığı',     '.card .ch',       '.sc-title',       ['font-size','font-weight']],
  ['menü öğesi',       '.nv:not(.act)',   '.nav-tab:not(.active)', ['font-size','font-weight','border-radius','padding','color']],
  ['aktif menü',       '.nv.act',         '.nav-tab.active', ['font-weight','color']],
  ['menü grubu',       '.ngroup',         '.ngroup',         ['font-size','letter-spacing','color']],
  ['üst çubuk',        '.top',            '.top',            ['height','border-bottom-color']],
  ['proje kartı',      '.pcard',          '.order-card',     ['border-radius','padding','background-color'], 'orders'],
  ['pstats değeri',    '.ps b',           '.ps b',           ['font-size','font-weight'], 'orders'],
  ['ilerleme çubuğu',  '.wbar',           '.wbar',           ['height','border-radius','background-color']],
  // Ürün Ağacı (tasarım 2026) — bom sekmesine geçince ilk ürün otomatik seçilir (aşağıda)
  ['ağaç düğümü',      '.tnode',          '.bom-part-row',   ['border-radius','padding'], 'bom'],
  ['ağaç kodu',        '.tnode .code',    '.bom-part-code',  ['font-size','color'], 'bom'],
  ['ağaç ikonu',       '.tic.part',       '.tic.part',       ['width','height','border-radius','background-color'], 'bom'],
  ['ağaç çipi',        '.chipk',          '.bom-part-meta>span', ['font-size','font-weight','border-radius','padding'], 'bom'],
  // Teknik Resimler (tasarım 2026) — canlıda dosya yoksa örnek .draw düğümü enjekte edilir (aşağıda)
  ['çizim kartı',      '.draw',           '.draw',           ['border-radius','background-color'], 'docs'],
  ['çizim başlığı',    '.draw .dmeta b',  '.draw .dmeta b',  ['font-size','font-weight'], 'docs'],
  ['çizim küçük resmi','.draw .thumb',    '.draw .thumb',    ['background-color'], 'docs'],
];

// Toleranslar: px ±0.8, weight ±15, letter-spacing ±0.2px, renk kanal farkı ≤10
function pxDiff(a, b) { return Math.abs(parseFloat(a) - parseFloat(b)); }
function colorDiff(a, b) {
  const p = v => (String(v).match(/\d+(\.\d+)?/g) || []).map(Number);
  const [x, y] = [p(a), p(b)];
  if (!x.length || !y.length) return a === b ? 0 : 999;
  return Math.max(...[0,1,2].map(i => Math.abs((x[i]||0) - (y[i]||0))));
}
function same(prop, a, b, fs14) {
  if (prop === 'line-height') {
    const n = v => v === 'normal' ? 18.6 : parseFloat(v); // 14px gövdede normal ≈ 1.33
    return Math.abs(n(a) - n(b)) <= 2.2;
  }
  if (a === b) return true;
  if (/color/.test(prop)) return colorDiff(a, b) <= 10;
  if (prop === 'font-weight') return pxDiff(a, b) <= 15;
  if (prop === 'letter-spacing') {
    if (a === 'normal' || b === 'normal') return (a === 'normal' ? 0 : parseFloat(a)) - (b === 'normal' ? 0 : parseFloat(b)) <= 0.2;
    return pxDiff(a, b) <= 0.2;
  }
  if (prop === 'padding') { // dört değeri ayrı karşılaştır
    const pa = a.split(' '), pb = b.split(' ');
    return pa.length === pb.length && pa.every((v, i) => pxDiff(v, pb[i]) <= 0.8);
  }
  if (/^[\d.]+px/.test(a) && /^[\d.]+px/.test(b)) return pxDiff(a, b) <= 0.8;
  if (prop === 'font-family') return a.split(',')[0] === b.split(',')[0];
  return false;
}

(async () => {
  const browser = await chromium.launch();
  const cPage = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await cPage.goto(pathToFileURL(CONCEPT).href);
  await cPage.waitForTimeout(800);

  const aPage = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await aPage.goto('http://localhost:8080/');
  await aPage.fill('#login-username', 'testdev');
  await aPage.fill('#login-password', 'test1234');
  await aPage.click('#login-btn');
  await aPage.waitForSelector('#user-badge', { state: 'visible', timeout: 15000 });
  await aPage.waitForTimeout(2500); // hero + rozetler otursun

  const grab = (page, sel, props) => page.evaluate(([s, ps]) => {
    const el = document.querySelector(s);
    if (!el) return null;
    const cs = getComputedStyle(el);
    const o = {};
    ps.forEach(p => o[p] = cs.getPropertyValue(p));
    return o;
  }, [sel, props]);

  let diff = 0, miss = 0;
  let curTab = 'dashboard';
  for (const [name, cSel, aSel, props, appTab] of MAP) {
    if (appTab && appTab !== curTab) {
      await aPage.evaluate(t => switchTab(t), appTab);
      await aPage.waitForTimeout(1800);
      if (appTab === 'bom') { // parça satırları ancak bir ürün seçilince render olur
        await aPage.evaluate(async () => {
          const s = document.getElementById('bom-product-select');
          if (s && s.options.length > 1 && !s.value) { s.value = s.options[1].value; await onBomProductChange(); }
        });
        await aPage.waitForTimeout(1200);
      }
      if (appTab === 'docs') { // dosya yoksa CSS ölçümü için ekran dışına örnek kart bas (DB'ye yazmaz)
        await aPage.evaluate(() => {
          if (!document.querySelector('.draw')) {
            const host = document.createElement('div');
            host.style.cssText = 'position:absolute;left:-9999px;top:0;width:260px';
            host.innerHTML = '<div class="draw"><div class="thumb"><span class="ext">DWG</span></div><div class="dmeta"><b>örnek</b><span>ölçüm</span></div></div>';
            document.body.appendChild(host);
          }
        });
        await aPage.waitForTimeout(300);
      }
      curTab = appTab;
    }
    const [c, a] = [await grab(cPage, cSel, props), await grab(aPage, aSel, props)];
    if (!c || !a) { console.log(`  ⚠ ${name}: bulunamadı (konsept:${!!c} uygulama:${!!a})`); miss++; continue; }
    for (const p of props) {
      if (same(p, c[p], a[p])) continue;
      console.log(`  ❌ ${name} · ${p}: konsept=${c[p]}  uygulama=${a[p]}`);
      diff++;
    }
  }
  console.log(diff || miss
    ? `\n${diff} fark, ${miss} eksik eşleme — konsept pariteye çekilmeli`
    : '\nKONSEPT PARİTESİ TAM ✅');
  await browser.close();
  process.exit(diff ? 1 : 0);
})().catch(e => { console.error(e.message); process.exit(2); });
