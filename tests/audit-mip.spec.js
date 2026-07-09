// MİP (Malzeme İhtiyaç Planlama) — Aşama 1 uçtan uca testi (7. tur #4).
//
// Arkadaşın senaryosu birebir kurulur:
//   Ürün ağacında 50 adet M12 Somun (Tedarik) → projeye yayınlanır
//   A-Depo'da 30 adet, B-Depo'da 10 adet var → 10 adet eksik
// MİP sekmesi açılınca satır "Sipariş verilmedi" (kırmızı) olmalı, eksik 10
// göstermeli ve öneri "30 A-Depo'dan · 10 B-Depo'dan · 10 satın alınacak" olmalı.
//
// Sonra kalan 10 adet sipariş verilir → satır "Sipariş bekleniyor"a döner.
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

test('MİP: ihtiyaç ile depo stoğunu karşılaştırır, eksiği ve durumu doğru gösterir', async ({ page }) => {
  const hatalar = [];
  page.on('pageerror', e => hatalar.push('[pageerror] ' + String(e).slice(0, 200)));

  await login(page);
  const sfx = Date.now().toString(36);
  const projeAdi = 'MIP-PROJE-' + sfx;
  const kod = 'MIPSOM-' + sfx;
  const temizlik = [];

  const order = await api(page, 'POST', '/orders', { project_name: projeAdi, customer_name: 'MIP' });
  const orderId = order.body.data.id;
  const prod = await api(page, 'POST', '/bom-products', { name: 'MIP Ürün', code: 'MIPP-' + sfx });
  const prodId = prod.body.data.id;
  // Tedarik türü parça: 50 adet
  const part = await api(page, 'POST', '/bom-parts', {
    product_id: prodId, name: 'M12 Somun', code: kod,
    quantity: 50, unit: 'adet', level: 0, material_kind: 'TEDARIK'
  });
  const partId = part.body.data.id;
  const pbom = await api(page, 'POST', '/project-bom', { project_name: projeAdi, bom_product_id: prodId });
  const pbomId = pbom.body.data.id;

  // İki depo + stok girişleri (A:30, B:10)
  const whA = await api(page, 'POST', '/warehouses', { name: 'MIP A-Depo ' + sfx });
  const whB = await api(page, 'POST', '/warehouses', { name: 'MIP B-Depo ' + sfx });
  const whAId = whA.body.data.id, whBId = whB.body.data.id;
  await api(page, 'POST', '/warehouse-movements', {
    warehouse_id: whAId, item_name: 'M12 Somun', item_code: kod,
    movement_type: 'IN', quantity: 30, unit: 'adet', source_type: 'MANUAL' });
  await api(page, 'POST', '/warehouse-movements', {
    warehouse_id: whBId, item_name: 'M12 Somun', item_code: kod,
    movement_type: 'IN', quantity: 10, unit: 'adet', source_type: 'MANUAL' });

  try {
    // Yayınla (project_bom status=published) — MİP yalnızca yayınlanmışları listeler
    await api(page, 'PUT', '/project-bom/' + pbomId, { project_name: projeAdi, bom_product_id: prodId, status: 'published' });

    // ── MİP sekmesi ──
    await page.evaluate(() => switchTab('mip'));
    await page.waitForTimeout(1500);
    await page.selectOption('#mip-project-sel', projeAdi);
    await page.waitForTimeout(1500);

    const satir = await page.evaluate(() => _mipRows.map(r => ({
      code: r.code, need: r.need, stockTotal: r.stockTotal,
      missing: r.missing, ordered: r.ordered, status: r.status,
      depo: r.stockByWh.map(s => s.qty)
    })));
    console.log('  MİP satırları:', JSON.stringify(satir));
    expect(satir.length, 'bir satır olmalı (sadece Tedarik parçası)').toBe(1);
    const r = satir[0];
    expect(r.need, 'ihtiyaç 50').toBe(50);
    expect(r.stockTotal, 'toplam stok 40').toBe(40);
    expect(r.missing, 'eksik 10').toBe(10);
    expect(r.status, 'sipariş yok → MISSING').toBe('MISSING');
    expect(r.depo.sort((a, b) => b - a), 'depo dağılımı 30 + 10').toEqual([30, 10]);

    // Ekranda görünüyor mu?
    const metin = await page.locator('#mip-list').innerText();
    expect(metin).toContain('Sipariş verilmedi');
    expect(metin).toContain('satın alınacak');
    // Özet kutularında CSS text-transform:uppercase var → innerText BÜYÜK harf
    // döner (Türkçe İ/I ayrımı nedeniyle karakter sınıfıyla eşleştiriyoruz).
    const ozet = await page.locator('#mip-summary').innerText();
    expect(ozet).toMatch(/VER[İI]LMED[İI]/);
    const missingSayisi = await page.evaluate(() =>
      _mipRows.filter(r => r.status === 'MISSING').length);
    expect(missingSayisi, 'özette 1 eksik parça sayılmalı').toBe(1);

    // ── Eksik 10 adet sipariş verilince WAITING olmalı ──
    const kalem = await api(page, 'POST', '/purchase-items', {
      project_name: projeAdi, name: 'M12 Somun', code: kod, quantity: 10, unit: 'adet' });
    const kalemId = kalem.body.data.id;
    temizlik.push(['/purchase-items', kalemId]);
    await api(page, 'PUT', '/purchase-items/' + kalemId, { status: 'ORDERED' });

    await page.evaluate(() => renderMip());
    await page.waitForTimeout(1500);
    const sonra = await page.evaluate(() => _mipRows[0] && ({ status: _mipRows[0].status, ordered: _mipRows[0].ordered }));
    console.log('  sipariş sonrası:', JSON.stringify(sonra));
    expect(sonra.status, 'sipariş verilince WAITING').toBe('WAITING');
    expect(sonra.ordered, 'siparişteki miktar 10').toBe(10);

    expect(hatalar, 'JS hatası olmamalı').toEqual([]);
  } finally {
    for (const [ep, id] of temizlik) await api(page, 'DELETE', ep + '/' + id);
    for (const p of (await api(page, 'GET', '/project-bom-parts')).body.data.filter(x => x.project_bom_id === pbomId))
      await api(page, 'DELETE', '/project-bom-parts/' + p.id);
    for (const m of (await api(page, 'GET', '/warehouse-movements')).body.data.filter(x => x.item_code === kod))
      await api(page, 'DELETE', '/warehouse-movements/' + m.id);
    for (const i of (await api(page, 'GET', '/purchase-items')).body.data.filter(x => x.project_name === projeAdi))
      await api(page, 'DELETE', '/purchase-items/' + i.id);
    for (const pt of (await api(page, 'GET', '/parts')).body.data.filter(x => x.project_name === projeAdi))
      await api(page, 'DELETE', '/parts/' + pt.id);
    await api(page, 'DELETE', '/project-bom/' + pbomId);
    await api(page, 'DELETE', '/warehouses/' + whAId);
    await api(page, 'DELETE', '/warehouses/' + whBId);
    await api(page, 'DELETE', '/bom-parts/' + partId);
    await api(page, 'DELETE', '/bom-products/' + prodId);
    await api(page, 'DELETE', '/orders/' + orderId);
  }
});
