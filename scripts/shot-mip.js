// MİP sekmesi geçiş-sonrası ekran görüntüleri (tasarım 2026 sonuç kanıtı)
// Kullanım: node scripts/shot-mip.js <çıktı-klasörü>
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

  await page.evaluate(() => switchTab('mip'));
  await page.waitForTimeout(1200);
  await page.evaluate(async () => {
    const s = document.getElementById('mip-project-sel');
    if (s && s.options.length > 1 && !s.value) { s.value = s.options[1].value; await renderMipList(); }
  });
  await page.waitForTimeout(1800);
  await page.screenshot({ path: out + '/mip-kehribar.png' });

  await page.evaluate(() => document.documentElement.setAttribute('data-variant', 'light'));
  await page.waitForTimeout(600);
  await page.screenshot({ path: out + '/mip-fildisi.png' });

  await browser.close();
  console.log('Ekran görüntüleri alındı → ' + out);
})().catch(e => { console.error(e.message); process.exit(2); });
