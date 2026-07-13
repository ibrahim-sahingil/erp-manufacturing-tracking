// (9. tur M2) "Kaydetmeden Kapat" gerçek tarayıcı testi.
// Editör anlık yazar; çıkışta 3 seçenekli modal çıkar. Kaydetmeden Kapat
// girişten sonraki değişiklikleri (ekleme + miktar) DIFF bazlı geri alır;
// Kaydet-Kapat olduğu gibi bırakır. Diff çekirdeği sunucusuz test edilir
// (scripts/verify-pbome-revert.js) — burada uçtan uca DB durumu doğrulanır.
const { test, expect } = require('@playwright/test');

async function login(page) {
  await page.goto('/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await expect(page.locator('#user-badge')).toBeVisible({ timeout: 15000 });
}
async function api(page, method, path, body) {
  return await page.evaluate(async ([m, p, b]) => {
    const tok = sessionStorage.getItem('ut_token');
    const r = await fetch('/api' + p, { method: m,
      headers: { Authorization: 'Bearer ' + tok, 'Content-Type': 'application/json' },
      body: b ? JSON.stringify(b) : undefined });
    let j = null; try { j = await r.json(); } catch (e) {}
    return { status: r.status, body: j };
  }, [method, path, body || null]);
}

test('kaydetmeden kapat: değişiklikler geri alınır, kaydet-kapat kalıcıdır', async ({ page }) => {
  await login(page);
  const sfx = Date.now().toString(36);
  const PRJ = 'RVT-PRJ-' + sfx;
  // Şablon ürün + kök parça + proje + bağlantı (API ile kur)
  const prod = await api(page, 'POST', '/bom-products', { name: 'RVT Ürün', code: 'RVT-' + sfx });
  const prodId = prod.body.data.id;
  await api(page, 'POST', '/bom-parts', { product_id: prodId, name: 'RVT Gövde',
    code: 'RVTG-' + sfx, quantity: 2, unit: 'adet', level: 0, sort_order: 1 });
  const ord = await api(page, 'POST', '/orders', { project_name: PRJ, customer_name: 'RVT Müşteri' });
  const orderId = ord.body.data.id;
  const pbom = await api(page, 'POST', '/project-bom',
    { project_name: PRJ, bom_product_id: prodId, status: 'draft' });
  const pbomId = pbom.body.data.id;

  try {
    // Editörü aç (şablondan 1 parça kopyalanır — bu taban "kaydedilmiş" sayılır)
    await page.evaluate(() => switchTab('bom'));
    await page.waitForTimeout(800);
    await page.evaluate(async id => { await loadProjectBoms(); await openPbomEditor(id); }, pbomId);
    await page.waitForTimeout(1500);
    let pbParts = (await api(page, 'GET', '/project-bom-parts')).body.data
      .filter(p => p.project_bom_id === pbomId);
    expect(pbParts.length, 'şablondan 1 parça kopyalanmalı').toBe(1);
    const rootId = pbParts[0].id;

    // Değişiklik 1: yeni parça ekle (anında yazılır)
    await page.fill('#pbome-part-name', 'RVT Eklenen');
    await page.fill('#pbome-part-code', 'RVTE-' + sfx);
    await page.locator('#view-pbom-editor button', { hasText: '+ Parça Ekle' }).click();
    await page.waitForTimeout(900);
    // Değişiklik 2: kök parçanın miktarı 2 → 7
    await page.evaluate(id => pbomeEditQty(id), rootId);
    await page.fill('#pbome-qty-val', '7');
    await page.locator('#pbome-qty-overlay button', { hasText: 'Kaydet' }).click();
    await page.waitForTimeout(900);
    pbParts = (await api(page, 'GET', '/project-bom-parts')).body.data
      .filter(p => p.project_bom_id === pbomId);
    expect(pbParts.length, 'ekleme anında yazılmalı').toBe(2);
    expect(Number(pbParts.find(p => p.id === rootId).custom_qty), 'miktar anında yazılmalı').toBe(7);

    // ← Geri → modal → Kaydetmeden Kapat
    await page.click('#view-pbom-editor button:has-text("← Geri")');
    await expect(page.locator('#pbome-close-overlay'), 'çıkış modalı açılmalı').toBeVisible();
    const ozet = await page.locator('#pbome-close-overlay').innerText();
    expect(ozet).toContain('1 parça eklendi');
    expect(ozet).toContain('1 parça değişti');
    await page.click('#pbome-close-discard');
    await page.waitForTimeout(2500);
    await expect(page.locator('#pbome-close-overlay')).toHaveCount(0);

    pbParts = (await api(page, 'GET', '/project-bom-parts')).body.data
      .filter(p => p.project_bom_id === pbomId);
    expect(pbParts.length, 'eklenen parça geri alınmalı').toBe(1);
    expect(pbParts[0].id, 'dokunulmayan satırın ID\'si korunmalı').toBe(rootId);
    expect(Number(pbParts[0].custom_qty), 'miktar eski değerine dönmeli').toBe(2);

    // Tekrar aç → değişiklik yap → Kaydet-Kapat → kalıcı olmalı
    await page.evaluate(async id => { await openPbomEditor(id); }, pbomId);
    await page.waitForTimeout(1200);
    await page.evaluate(id => pbomeEditQty(id), rootId);
    await page.fill('#pbome-qty-val', '5');
    await page.locator('#pbome-qty-overlay button', { hasText: 'Kaydet' }).click();
    await page.waitForTimeout(900);
    await page.click('#view-pbom-editor button:has-text("← Geri")');
    await expect(page.locator('#pbome-close-overlay')).toBeVisible();
    await page.click('#pbome-close-save');
    await page.waitForTimeout(800);
    pbParts = (await api(page, 'GET', '/project-bom-parts')).body.data
      .filter(p => p.project_bom_id === pbomId);
    expect(Number(pbParts[0].custom_qty), 'kaydet-kapat kalıcı olmalı').toBe(5);

    // Değişiklik yokken tekrar aç-kapa → modal ÇIKMAMALI (sessiz kapanış)
    await page.evaluate(async id => { await openPbomEditor(id); }, pbomId);
    await page.waitForTimeout(1200);
    await page.click('#view-pbom-editor button:has-text("← Geri")');
    await page.waitForTimeout(600);
    await expect(page.locator('#pbome-close-overlay'), 'temiz çıkışta modal yok').toHaveCount(0);
  } finally {
    for (const p of (await api(page, 'GET', '/project-bom-parts')).body.data
        .filter(p => p.project_bom_id === pbomId).sort((a, b) => (b.level || 0) - (a.level || 0)))
      await api(page, 'DELETE', '/project-bom-parts/' + p.id);
    await api(page, 'DELETE', '/project-bom/' + pbomId);
    for (const p of (await api(page, 'GET', '/bom-parts')).body.data.filter(p => p.product_id === prodId))
      await api(page, 'DELETE', '/bom-parts/' + p.id);
    await api(page, 'DELETE', '/bom-products/' + prodId);
    await api(page, 'DELETE', '/orders/' + orderId);
  }
});
