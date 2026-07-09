// 7. tur #2 — "Projeye bağladığım ürün ağacında işlemi kaldırmak istiyorum ama
// ✕ butonu çalışmıyor". Bu test hatayı ÜRETMEK için yazıldı: proje kurar,
// ürün ağacını projeye bağlar, editörde bir parçaya işlem ekler, sonra ✕'e basar.
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

test('pbome: işlem tag\'indeki ✕ işlemi gerçekten kaldırır', async ({ page }) => {
  const hatalar = [];
  page.on('pageerror', e => hatalar.push('[pageerror] ' + String(e).slice(0, 200)));
  page.on('console', m => { if (m.type() === 'error') hatalar.push('[konsol] ' + m.text().slice(0, 200)); });

  await login(page);
  const sfx = Date.now().toString(36);
  const projeAdi = 'PBRM-PROJE-' + sfx;

  const order = await api(page, 'POST', '/orders', { project_name: projeAdi, customer_name: 'PBRM' });
  const orderId = order.body.data.id;
  const prod = await api(page, 'POST', '/bom-products', { name: 'PBRM Ürün', code: 'PBRM-' + sfx });
  const prodId = prod.body.data.id;
  const opdef = await api(page, 'POST', '/bom-operations', { name: 'PBRM Kaynak', code: '-PW' });
  const opId = opdef.body.data.id;
  // ARKADAŞIN SENARYOSU (image 100/101): kök parçanın İKİ işlemi (WLD+PNT) ve
  // ALT PARÇALARI var; işlemler şablonda tanımlı, yayınlamayla kopyalanıyor.
  const part = await api(page, 'POST', '/bom-parts',
    { product_id: prodId, name: 'PBRM Gövde', code: 'PBRMG-' + sfx + 'WLDPNT', quantity: 1, unit: 'adet', level: 0,
      operations: [
        { name: 'Kaynak', code: 'WLD', desc: '', duration_per_unit: 0, total_duration: 0 },
        { name: 'Boya',   code: 'PNT', desc: '', duration_per_unit: 0, total_duration: 0 }
      ] });
  const partId = part.body.data.id;
  const cocuk = await api(page, 'POST', '/bom-parts',
    { product_id: prodId, parent_id: partId, name: 'PBRM Sac', code: 'PBRMS-' + sfx, quantity: 2, unit: 'adet', level: 1 });
  const cocukId = cocuk.body.data.id;
  const pbom = await api(page, 'POST', '/project-bom', { project_name: projeAdi, bom_product_id: prodId });
  const pbomId = pbom.body.data.id;

  try {
    // Ürün ağacı sekmesi → proje BOM editörünü aç (kopyalama burada oluyor)
    await page.evaluate(() => switchTab('bom'));
    await page.waitForTimeout(1200);
    await page.evaluate(id => openPbomEditor(id), pbomId);
    await page.waitForTimeout(1500);

    let pbParts = await page.evaluate(() => _pbomeParts.map(p => ({
      id: p.id, code: p.custom_code, ops: (p.operations || []).length })));
    console.log('  kopyalanan parçalar:', JSON.stringify(pbParts));
    expect(pbParts.length, 'ağaç projeye kopyalanmalı').toBeGreaterThan(0);

    // İşlem şablondan KOPYALANMIŞ olarak gelmeli (yayınlama)
    const opluParca = pbParts.find(p => p.ops > 0);
    expect(opluParca, 'kopyalanan parçada işlem olmalı (şablondan)').toBeTruthy();

    // İLK işlemin (WLD) ✕ butonuna bas — arkadaşın yaptığı da bu
    const tagSayisi = await page.locator('.bom-op-tag').count();
    console.log('  işlem tag sayısı:', tagSayisi);
    const xBtn = page.locator('.bom-op-tag button').first();
    await expect(xBtn).toBeVisible();
    await xBtn.click();
    await page.waitForTimeout(1500);
    const toastTxt = await page.locator('#toast').innerText().catch(() => '');
    console.log('  toast:', toastTxt || '(yok)');

    const sonrasi = await page.evaluate(() => _pbomeParts.map(p => ({
      id: p.id, code: p.custom_code, ops: (p.operations || []).length })));
    console.log('  ✕ sonrası:', JSON.stringify(sonrasi));
    console.log('  JS hataları:', hatalar.length ? hatalar.join(' | ') : '(yok)');

    const kalan = sonrasi.find(p => p.id === opluParca.id);
    expect(kalan.ops, '✕ bir işlemi kaldırmalı (2 → 1)').toBe(opluParca.ops - 1);
    // ORTADAKİ işlem (WLD) kaldırıldı → kod artık WLD içermemeli, PNT kalmalı.
    // Eski kod "sondaki sonekle bitiyorsa kes" yaptığından WLD kalıntı kalıyordu.
    expect(kalan.code, 'kaldırılan işlemin kodu parçada kalmamalı').not.toContain('WLD');
    expect(kalan.code, 'kalan işlemin kodu korunmalı').toContain('PNT');
    expect(hatalar, 'JS hatası olmamalı').toEqual([]);

    // Son işlemi de kaldır → kod temel haline dönmeli (ekle/kaldır simetrisi)
    await page.locator('.bom-op-tag button').first().click();
    await page.waitForTimeout(1500);
    const bos = await page.evaluate(id => {
      const p = _pbomeParts.find(x => x.id === id);
      return { code: p.custom_code, ops: (p.operations || []).length };
    }, opluParca.id);
    console.log('  tüm işlemler kaldırıldıktan sonra:', JSON.stringify(bos));
    expect(bos.ops, 'işlem kalmamalı').toBe(0);
    expect(bos.code, 'kod temel haline dönmeli').toBe('PBRMG-' + sfx);
  } finally {
    for (const p of (await api(page, 'GET', '/project-bom-parts')).body.data.filter(x => x.project_bom_id === pbomId))
      await api(page, 'DELETE', '/project-bom-parts/' + p.id);
    await api(page, 'DELETE', '/project-bom/' + pbomId);
    await api(page, 'DELETE', '/bom-parts/' + cocukId);
    await api(page, 'DELETE', '/bom-parts/' + partId);
    await api(page, 'DELETE', '/bom-products/' + prodId);
    await api(page, 'DELETE', '/bom-operations/' + opId);
    await api(page, 'DELETE', '/orders/' + orderId);
  }
});
