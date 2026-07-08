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

// ─── Hata mesajı ayrıştırma (2026-07-09: "her hata = şifre hatalı" kusuru) ───

test('mesaj: yanlış şifrede "Kullanıcı adı veya şifre hatalı" denir', async ({ page }) => {
  await page.goto('/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'yanlis-sifre-xyz');
  await page.click('#login-btn');
  await expect(page.locator('#login-err')).toHaveText(/Kullanıcı adı veya şifre hatalı/, { timeout: 10000 });
});

test('mesaj: sunucuya ulaşılamayınca ŞİFRE HATASI DENMEZ, bağlantı hatası denir', async ({ page }) => {
  await page.goto('/');
  // Login isteğini ağ seviyesinde düşür — sunucu kapalı senaryosu
  await page.route('**/api/auth/login', route => route.abort('connectionrefused'));
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await expect(page.locator('#login-err')).toHaveText(/Sunucuya ulaşılamadı/, { timeout: 10000 });
  await expect(page.locator('#login-err')).not.toHaveText(/şifre hatalı/);
});

test('mesaj: iş kuralı hatasında backend mesajı toast olarak görünür (mükerrer malzeme)', async ({ page }) => {
  await login(page);
  // Aynı adla iki kez malzeme ekle — ikincisi backend'in Türkçe mesajını göstermeli
  const name = 'PW-TEST-' + Date.now();
  const firstId = await page.evaluate(async (n) => {
    const d = await dbInsert('materials', { name: n });
    return Array.isArray(d) && d[0] ? d[0].id : null;
  }, name);
  expect(firstId).toBeTruthy();
  try {
    await page.evaluate(async (n) => { await dbInsert('materials', { name: n }); }, name);
    await expect(page.locator('#toast')).toContainText(/zaten var/, { timeout: 5000 });
  } finally {
    await page.evaluate(async (id) => { await dbDelete('materials', id); }, firstId);
  }
});

test('mesaj: okuma başarısız olunca sessiz boş liste yerine uyarı çıkar', async ({ page }) => {
  await login(page);
  await page.route('**/api/materials**', route => route.abort('connectionrefused'));
  await page.evaluate(async () => { await dbGet('materials', 'order=name.asc'); });
  await expect(page.locator('#toast')).toContainText(/Veriler yüklenemedi/, { timeout: 5000 });
});

// NOT: Silme/yayınlama/mal kabul guard'ları backend + node scripts/e2e-test.js
// tarafından API-seviyesinde kapsanıyor (K1/O1/O4/O5/U1 senaryoları). Playwright
// burada gerçek-tarayıcı davranışına (login akışı + XSS render) odaklanır; UI
// guard akışları frontend referans-cache'ine bağlı olduğundan node testlerinde
// daha stabil doğrulanır.
