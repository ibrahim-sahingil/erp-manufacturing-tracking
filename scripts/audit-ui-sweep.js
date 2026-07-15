// UI TAM TARAMASI (tasarım 2026) — "noktası virgülüne" bekçisi.
// 4 temanın DÖRDÜNDE, 11 sekmenin + tüm alt sekmelerin (planning-tab deseni)
// hepsini gezer ve şunları arar:
//   - JS hatası (pageerror)
//   - ekrana metin olarak kaçmış SVG kodu ("svg width=")
//   - kalıntı emoji (favicon hariç — body metninde)
//   - "undefined" / "NaN" metin sızıntısı (kırık interpolasyon)
//   - yatay taşma (body genişliği pencereyi aşıyor)
// Sunucu 8080'de olmalı. Kullanım: node scripts/audit-ui-sweep.js [--hizli]
// --hizli: yalnız Kehribar + Fildişi temaları (varsayılan: 4 tema).
const { chromium } = require('@playwright/test');

const TABS = ['dashboard','stats','orders','planning','bom','docs','mip','purchasing','warehouse','users','appusers'];
const THEMES = process.argv.includes('--hizli') ? ['', 'light'] : ['', 'light', 'steel', 'forest'];
const EMOJI = /[\u{1F000}-\u{1FAFF}]/u;

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const jsErrors = [];
  page.on('pageerror', e => jsErrors.push(e.message));

  await page.goto('http://localhost:8080/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await page.waitForSelector('#user-badge', { state: 'visible', timeout: 15000 });
  await page.waitForTimeout(2000);

  const bulgular = [];
  const kontrol = async (etiket) => {
    const r = await page.evaluate(() => ({
      leak: document.body.innerText.includes('svg width='),
      undef: /(^|[\s>·(])undefined([\s<·),.]|$)/.test(document.body.innerText),
      nan: /(^|[\s>·(])NaN([\s<·),.]|$)/.test(document.body.innerText),
      tasma: document.documentElement.scrollWidth > document.documentElement.clientWidth + 2,
    }));
    const emoji = await page.evaluate(() => /[\u{1F000}-\u{1FAFF}]/u.test(document.body.innerText));
    if (r.leak)  bulgular.push(etiket + ': SVG kod sızıntısı');
    if (emoji)   bulgular.push(etiket + ': emoji kalıntısı');
    if (r.undef) bulgular.push(etiket + ': "undefined" metni');
    if (r.nan)   bulgular.push(etiket + ': "NaN" metni');
    if (r.tasma) bulgular.push(etiket + ': yatay taşma');
  };

  for (const theme of THEMES) {
    await page.evaluate(t => setTheme(t), theme);
    const tAd = theme || 'kehribar';
    for (const tab of TABS) {
      await page.evaluate(tt => switchTab(tt), tab);
      await page.waitForTimeout(1100);
      await kontrol(`${tAd}/${tab}`);
      // Alt sekmeler (planning-tab deseni tüm modüllerde ortak)
      const altSayisi = await page.evaluate(() =>
        document.querySelectorAll('.view.active .planning-tab').length);
      for (let i = 0; i < altSayisi; i++) {
        await page.evaluate(idx => document.querySelectorAll('.view.active .planning-tab')[idx].click(), i);
        await page.waitForTimeout(900);
        await kontrol(`${tAd}/${tab}/alt${i + 1}`);
      }
    }
  }
  await page.evaluate(() => setTheme(''));

  jsErrors.forEach(e => bulgular.push('JS HATASI: ' + e.slice(0, 140)));
  const uniq = [...new Set(bulgular)];
  uniq.forEach(b => console.log('  ❌ ' + b));
  console.log(uniq.length
    ? `\n${uniq.length} bulgu — UI taraması BAŞARISIZ`
    : `\nUI TAM TARAMASI TEMİZ ✅ (${THEMES.length} tema × ${TABS.length} sekme + alt sekmeler)`);
  await browser.close();
  process.exit(uniq.length ? 1 : 0);
})().catch(e => { console.error(e.message); process.exit(2); });
