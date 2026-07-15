// Ürün Ağacı sekmesi geçiş-sonrası ekran görüntüleri (tasarım 2026 sonuç kanıtı)
// Kullanım: node scripts/shot-bom.js <çıktı-klasörü>
const { chromium } = require('@playwright/test');
const out = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  page.on('pageerror', e => { console.error('JS HATASI: ' + e.message); process.exitCode = 1; });

  await page.goto('http://localhost:8080/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await page.waitForSelector('#user-badge', { state: 'visible', timeout: 15000 });
  await page.waitForTimeout(2000);

  // Ürün Ağacı sekmesi + ilk ürün seçimi
  await page.evaluate(() => switchTab('bom'));
  await page.waitForTimeout(1200);
  await page.evaluate(async () => {
    const s = document.getElementById('bom-product-select');
    if (s && s.options.length > 1 && !s.value) { s.value = s.options[1].value; await onBomProductChange(); }
  });
  await page.waitForTimeout(1200);
  await page.screenshot({ path: out + '/bom-kehribar.png', fullPage: false });

  // Sayfayı biraz kaydır: proje bağlama kartları
  await page.evaluate(() => { const el = document.getElementById('project-bom-list'); if (el) el.scrollIntoView({ block: 'center' }); });
  await page.waitForTimeout(500);
  await page.screenshot({ path: out + '/bom-pbaglama.png' });

  // Proje BOM editörü (ilk mevcut bağlantı) + KPI şeridi
  const opened = await page.evaluate(() => {
    const btn = document.querySelector('#project-bom-list button[onclick^="openPbomEditor"]');
    if (btn) { btn.click(); return true; }
    return false;
  });
  if (opened) {
    await page.waitForTimeout(2500);
    await page.screenshot({ path: out + '/bom-pbome-ust.png' });
    await page.evaluate(() => { const el = document.getElementById('pbome-kpis'); if (el) el.scrollIntoView({ block: 'center' }); });
    await page.waitForTimeout(500);
    await page.screenshot({ path: out + '/bom-pbome-kpi.png' });
    await page.evaluate(() => closePbomEditor());
    await page.waitForTimeout(800);
  } else {
    console.log('NOT: proje bağlantısı yok — pbome ekran görüntüsü atlandı');
  }

  // Fildişi (açık) temada aynı ekran
  await page.evaluate(() => document.documentElement.setAttribute('data-variant', 'light'));
  await page.waitForTimeout(600);
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: out + '/bom-fildisi.png' });

  await browser.close();
  console.log('Ekran görüntüleri alındı → ' + out);
})().catch(e => { console.error(e.message); process.exit(2); });
