// Kritik akışların gerçek tarayıcı (Chromium) testi — Faz 5.
// Sunucu 8080'de çalışır durumda olmalı (./mvnw spring-boot:run). Node
// e2e-test.js API senaryolarını, verify-h-render.js DOM-shim XSS'i kapsar;
// bu paket GERÇEK tarayıcıda tıklama/render/XSS davranışını doğrular.
const { test, expect } = require('@playwright/test');

async function login(page) {
  await page.goto('/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await expect(page.locator('#user-badge')).toBeVisible({ timeout: 15000 });
}

test('login: geçerli bilgilerle giriş yapılır, kullanıcı rozeti görünür', async ({ page }) => {
  await login(page);
  await expect(page.locator('#login-screen')).toBeHidden();
});

test('login: yanlış şifre reddedilir', async ({ page }) => {
  await page.goto('/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'yanlis-sifre-xyz');
  await page.click('#login-btn');
  // Hata mesajı görünür, kullanıcı rozeti GÖRÜNMEZ
  await expect(page.locator('#login-err')).toBeVisible({ timeout: 10000 });
  await expect(page.locator('#user-badge')).toBeHidden();
});

test('XSS: sipariş proje adındaki <img onerror> gerçek tarayıcıda ÇALIŞMAZ', async ({ page }) => {
  let dialogFired = false;
  page.on('dialog', d => { dialogFired = true; d.dismiss().catch(() => {}); });

  await login(page);
  const token = await page.evaluate(() => sessionStorage.getItem('ut_token'));
  expect(token).toBeTruthy();

  const EVIL = 'XSSPROJ<img src=x onerror=window.__xssFired=1>';
  const orderId = await page.evaluate(async ([tok, proj]) => {
    const r = await fetch('/api/orders', {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + tok, 'Content-Type': 'application/json' },
      body: JSON.stringify({ project_name: proj, customer_name: 'E2E-PW-XSS' })
    });
    const j = await r.json();
    return j && j.data ? j.data.id : null;
  }, [token, EVIL]);
  expect(orderId).toBeTruthy();

  try {
    // Siparişler ekranını gerçek DOM'a render et
    await page.evaluate(async () => { await loadOrders(); await switchTab('orders'); });
    await page.waitForTimeout(1000);

    // 1) onerror tetiklenmedi (kötü <img> DOM'a girmedi)
    const xssFired = await page.evaluate(() => window.__xssFired);
    expect(xssFired).toBeUndefined();
    // 2) Hiç dialog/alert açılmadı
    expect(dialogFired).toBe(false);
    // 3) Değer entity olarak görünür (kaçırılmış), ham <img> olarak DEĞİL
    const html = await page.evaluate(() => document.getElementById('orders-list')?.innerHTML || '');
    expect(html).toContain('XSSPROJ&lt;img');
    expect(html).not.toContain('XSSPROJ<img src=x onerror');
  } finally {
    await page.evaluate(async ([tok, id]) => {
      await fetch('/api/orders/' + id, { method: 'DELETE', headers: { Authorization: 'Bearer ' + tok } });
    }, [token, orderId]);
  }
});

// NOT: Silme/yayınlama/mal kabul guard'ları backend + node scripts/e2e-test.js
// tarafından API-seviyesinde kapsanıyor (K1/O1/O4/O5/U1 senaryoları). Playwright
// burada gerçek-tarayıcı davranışına (login akışı + XSS render) odaklanır; UI
// guard akışları frontend referans-cache'ine bağlı olduğundan node testlerinde
// daha stabil doğrulanır.
