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

test('ürün silme: onaydan sonra ürün + tüm ağacı (yapraktan köke) silinir', async ({ page }) => {
  await login(page);
  const sfx = Date.now().toString(36);
  const prod = await api(page, 'POST', '/bom-products', { name: 'CASCADE Ürün', code: 'CSC-' + sfx });
  const prodId = prod.body.data.id;
  // 3 seviyeli ağaç: kök → çocuk → torun (BomPartService alt parçası olanı silmez,
  // bu yüzden frontend yapraktan köke silmek zorunda)
  const kok = await api(page, 'POST', '/bom-parts',
    { product_id: prodId, name: 'Kök', code: 'CSC-K-' + sfx, quantity: 1, unit: 'adet', level: 0 });
  const cocuk = await api(page, 'POST', '/bom-parts',
    { product_id: prodId, parent_id: kok.body.data.id, name: 'Çocuk', code: 'CSC-C-' + sfx, quantity: 1, unit: 'adet', level: 1 });
  await api(page, 'POST', '/bom-parts',
    { product_id: prodId, parent_id: cocuk.body.data.id, name: 'Torun', code: 'CSC-T-' + sfx, quantity: 1, unit: 'adet', level: 2 });

  page.on('dialog', d => d.accept().catch(() => {}));
  await page.evaluate(() => switchTab('bom'));
  await page.waitForTimeout(800);
  await page.selectOption('#bom-product-select', prodId);
  await page.waitForTimeout(600);

  await page.click('#bom-delete-product-btn');
  await page.waitForTimeout(3000);

  const kalanParca = (await api(page, 'GET', '/bom-parts')).body.data.filter(p => p.product_id === prodId);
  const kalanUrun  = (await api(page, 'GET', '/bom-products')).body.data.filter(p => p.id === prodId);
  expect(kalanParca.length, '3 parçanın hepsi silinmeli').toBe(0);
  expect(kalanUrun.length, 'ürün silinmeli').toBe(0);
  await expect(page.locator('#toast')).toContainText(/3 parçası silindi/i);
});

test('appusers sekmesi developer olmayan kullanıcıya açılmaz', async ({ page }) => {
  await login(page);
  // Kullanıcıyı geçici olarak developer DEĞİLmiş gibi göster (yalnızca istemci
  // tarafı kontrolü sınanıyor; yazma uçları backend'de zaten kilitli)
  await page.evaluate(() => {
    currentUser = { ...currentUser, role: 'user', permissions: ['dashboard'] };
  });
  await page.evaluate(() => switchTab('appusers'));
  await page.waitForTimeout(500);
  await expect(page.locator('#toast')).toContainText(/yetkiniz yok/i);
  const acik = await page.evaluate(() =>
    document.getElementById('view-appusers').classList.contains('active'));
  expect(acik, 'appusers görünümü açılmamalı').toBe(false);
});

test('silme reddedilirse UI "silindi" demez ve ürün listeden kalkmaz', async ({ page }) => {
  await login(page);
  const sfx = Date.now().toString(36);
  const projeAdi = 'DELFB-PROJE-' + sfx;
  const order = await api(page, 'POST', '/orders', { project_name: projeAdi, customer_name: 'DELFB Müşteri' });
  const orderId = order.body.data.id;
  const prod = await api(page, 'POST', '/bom-products', { name: 'DELFB Ürün', code: 'DELFB-' + sfx });
  const prodId = prod.body.data.id;
  // Ürün bir PROJEDE kullanılıyor → BomProductService silmeyi reddeder
  const pbom = await api(page, 'POST', '/project-bom', { project_name: projeAdi, bom_product_id: prodId });
  const pbomId = pbom.body.data && pbom.body.data.id;
  expect(pbomId, 'proje-ürün bağlantısı kurulmalı').toBeTruthy();

  try {
    page.on('dialog', d => d.accept().catch(() => {}));

    await page.evaluate(() => switchTab('bom'));
    await page.waitForTimeout(800);
    await page.selectOption('#bom-product-select', prodId);
    await page.waitForTimeout(600);

    // Projede kullanılıyor → backend reddetmeli
    await page.click('#bom-delete-product-btn');
    await page.waitForTimeout(2000);

    // 1) Yanlış başarı mesajı GÖSTERİLMEMELİ
    const toastTxt = await page.locator('#toast').innerText().catch(() => '');
    expect(toastTxt, 'reddedilen silmede "silindi" denmemeli').not.toMatch(/silindi/i);

    // 2) Ürün açılır listeden KALKMAMALI
    const stillListed = await page.evaluate(id =>
      [...document.getElementById('bom-product-select').options].some(o => o.value === id), prodId);
    expect(stillListed, 'reddedilen silmede ürün listede kalmalı').toBe(true);

    // 3) Ürün gerçekten DB'de duruyor olmalı
    const kalan = (await api(page, 'GET', '/bom-products')).body.data.filter(p => p.id === prodId);
    expect(kalan.length, 'ürün DB\'de durmalı').toBe(1);
  } finally {
    await api(page, 'DELETE', '/project-bom/' + pbomId);
    await api(page, 'DELETE', '/bom-products/' + prodId);
    await api(page, 'DELETE', '/orders/' + orderId);
  }
});
