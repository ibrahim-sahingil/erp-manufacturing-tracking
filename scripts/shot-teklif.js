// Teklif/Onaylanan ayrımı geçiş-sonrası görüntü (12. tur m1 sonuç kanıtı)
// Geçici DEMO teklifi oluşturur, görüntüyü alır, SİLER. Kullanım: node scripts/shot-teklif.js <klasör>
const { chromium } = require('@playwright/test');
const out = process.argv[2] || '.';

(async () => {
  const login = await fetch('http://localhost:8080/api/auth/login', { method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'testdev', password: 'test1234' }) }).then(r => r.json());
  const tok = login.data.token;
  const mk = await fetch('http://localhost:8080/api/orders', { method: 'POST',
    headers: { Authorization: 'Bearer ' + tok, 'Content-Type': 'application/json' },
    body: JSON.stringify({ project_name: 'DEMO Teklif — Konveyör Hattı', customer_name: 'Örnek Sanayi A.Ş.',
      customer_email: 'satinalma@ornek.com', location: 'Bursa', delivery_days: 45,
      total_price: 250000, currency: 'TRY', status: 'quote', notes: 'İlk görüşme 15.07 — revize bekleniyor',
      items: [{ item_name: 'Konveyör Modülü', description: '6m tahrikli', quantity: 3 }] }) }).then(r => r.json());
  const qid = mk.data && mk.data.id;

  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  page.on('pageerror', e => { console.error('JS HATASI: ' + e.message); process.exitCode = 1; });
  await page.goto('http://localhost:8080/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await page.waitForSelector('#user-badge', { state: 'visible', timeout: 15000 });
  await page.waitForTimeout(2000);
  await page.evaluate(() => switchTab('orders'));
  await page.waitForTimeout(1500);
  await page.evaluate(() => switchOrdersTab('quotes'));
  await page.waitForTimeout(1200);
  await page.screenshot({ path: out + '/teklifler.png' });
  await browser.close();

  if (qid) await fetch('http://localhost:8080/api/orders/' + qid, { method: 'DELETE',
    headers: { Authorization: 'Bearer ' + tok } });
  console.log('Görüntü alındı, DEMO teklif silindi → ' + out);
})().catch(e => { console.error(e.message); process.exit(2); });
