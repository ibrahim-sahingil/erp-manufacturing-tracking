// Başarısız silme "başarılı" gibi görünmemeli (5. denetim turu, Bölüm 3).
//
// Bulunan hata: deleteCurrentProduct / deletePd / deleteWorkspace /
// removeWsMember, dbDelete()'in dönüş değerini YOK SAYIYORDU. Backend guard'ı
// silmeyi reddettiğinde (ör. ürünün parçaları var) UI yine de kaydı listeden
// kaldırıp "silindi" toast'ı gösteriyordu — kullanıcı sildiğini sanıyor, sayfayı
// yenileyince kayıt geri geliyordu. Ayrıca onay metni "ürünü ve TÜM PARÇALARINI
// siler" diye yanlış vaat veriyordu; backend parça varsa hiç silmiyor.
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

test('silme reddedilirse UI "silindi" demez ve ürün listeden kalkmaz', async ({ page }) => {
  await login(page);
  const sfx = Date.now().toString(36);
  const prod = await api(page, 'POST', '/bom-products', { name: 'DELFB Ürün', code: 'DELFB-' + sfx });
  const prodId = prod.body.data.id;
  const part = await api(page, 'POST', '/bom-parts',
    { product_id: prodId, name: 'DELFB Parça', code: 'DELFBP-' + sfx, quantity: 1, unit: 'adet' });
  const partId = part.body.data.id;

  try {
    // Onay kutusunu otomatik kabul et
    page.on('dialog', d => d.accept().catch(() => {}));

    await page.evaluate(() => switchTab('bom'));
    await page.waitForTimeout(800);
    await page.selectOption('#bom-product-select', prodId);
    await page.waitForTimeout(600);

    // Parçası VAR → backend reddetmeli
    await page.click('#bom-delete-product-btn');
    await page.waitForTimeout(1500);

    // 1) Yanlış başarı mesajı GÖSTERİLMEMELİ
    const toastTxt = await page.locator('#toast').innerText().catch(() => '');
    expect(toastTxt, 'reddedilen silmede "Ürün silindi" denmemeli').not.toMatch(/Ürün silindi/i);

    // 2) Ürün açılır listeden KALKMAMALI
    const stillListed = await page.evaluate(id =>
      [...document.getElementById('bom-product-select').options].some(o => o.value === id), prodId);
    expect(stillListed, 'reddedilen silmede ürün listede kalmalı').toBe(true);

    // 3) Ürün gerçekten DB'de duruyor olmalı
    const kalan = (await api(page, 'GET', '/bom-products')).body.data.filter(p => p.id === prodId);
    expect(kalan.length, 'ürün DB\'de durmalı').toBe(1);
  } finally {
    await api(page, 'DELETE', '/bom-parts/' + partId);
    await api(page, 'DELETE', '/bom-products/' + prodId);
  }
});
