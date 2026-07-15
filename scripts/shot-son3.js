// İrsaliye + Personel + Kullanıcılar geçiş-sonrası görüntüler (tasarım 2026)
// Kullanım: node scripts/shot-son3.js <çıktı-klasörü>
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

  await page.evaluate(() => { switchTab('warehouse'); switchWarehouseTab('delivery'); });
  await page.waitForTimeout(1500);
  await page.screenshot({ path: out + '/irs-kehribar.png' });

  await page.evaluate(() => switchTab('users'));
  await page.waitForTimeout(1200);
  await page.screenshot({ path: out + '/personel-kehribar.png' });

  await page.evaluate(() => switchTab('appusers'));
  await page.waitForTimeout(1200);
  await page.screenshot({ path: out + '/kullanicilar-kehribar.png' });

  await browser.close();
  console.log('Ekran görüntüleri alındı → ' + out);
})().catch(e => { console.error(e.message); process.exit(2); });
