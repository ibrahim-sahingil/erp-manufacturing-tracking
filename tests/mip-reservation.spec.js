// MİP Aşama 2 — depoya rezervasyon iş emri, UÇTAN UCA UI testi (7. tur #4).
//
// test.txt senaryosu gerçek tarayıcıda, gerçek butonlarla kurulur:
//   Ağaçta 30 adet Tedarik parçası → yayınlanır; A-Depo kaydında 30 görünür.
//   MİP satırından "Rezervasyon Emri" gönderilir (modal, önerilen miktar 30).
//   Depocu sayımda 15 bulur → onay modalında 15 + açıklama + düzeltme kutusu.
// Beklenen: rezervasyon PARTIAL; RESERVATION OUT 15 + RESERVATION_ADJUST OUT 15
// (net stok 0); eksik 15 PLANNED satın alma kalemi; MİP satırında Rezerve 15 +
// Eksik 15. Onay backend'de tek transaction — UI yalnızca approve ucunu çağırır.
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

test('MİP rezervasyonu: talep → depocu kısmi onayı → stok/satın alma/MİP tutarlı', async ({ page }) => {
  const hatalar = [];
  page.on('pageerror', e => hatalar.push('[pageerror] ' + String(e).slice(0, 200)));

  await login(page);
  const sfx = Date.now().toString(36);
  const projeAdi = 'MIPR-PROJE-' + sfx;
  const kod = 'MIPR-CIV-' + sfx;

  const order = await api(page, 'POST', '/orders', { project_name: projeAdi, customer_name: 'MIPR' });
  const orderId = order.body.data.id;
  const prod = await api(page, 'POST', '/bom-products', { name: 'MIPR Ürün', code: 'MIPRP-' + sfx });
  const prodId = prod.body.data.id;
  const part = await api(page, 'POST', '/bom-parts', {
    product_id: prodId, name: 'MIPR M12 Civata', code: kod,
    quantity: 30, unit: 'adet', level: 0, material_kind: 'TEDARIK'
  });
  const partId = part.body.data.id;
  const pbom = await api(page, 'POST', '/project-bom', { project_name: projeAdi, bom_product_id: prodId });
  const pbomId = pbom.body.data.id;
  const wh = await api(page, 'POST', '/warehouses', { name: 'MIPR A-Depo ' + sfx });
  const whId = wh.body.data.id;
  // Kayıtta 30 görünür (fiziksel sayımda 15 çıkacak)
  await api(page, 'POST', '/warehouse-movements', {
    warehouse_id: whId, item_name: 'MIPR M12 Civata', item_code: kod,
    movement_type: 'IN', quantity: 30, unit: 'adet', source_type: 'MANUAL' });

  try {
    await api(page, 'PUT', '/project-bom/' + pbomId,
      { project_name: projeAdi, bom_product_id: prodId, status: 'published' });

    // ── 1) MİP: satır FROM_STOCK, buton görünür ──
    await page.evaluate(() => switchTab('mip'));
    await page.waitForTimeout(1500);
    await page.selectOption('#mip-project-sel', projeAdi);
    await page.waitForTimeout(1500);
    const once = await page.evaluate(() => _mipRows[0] && ({
      need: _mipRows[0].need, stockTotal: _mipRows[0].stockTotal,
      missing: _mipRows[0].missing, status: _mipRows[0].status }));
    console.log('  talep öncesi satır:', JSON.stringify(once));
    expect(once.status, 'stok karşılıyor → FROM_STOCK').toBe('FROM_STOCK');
    const buton = page.locator('#mip-list button', { hasText: 'Rezervasyon Emri' });
    await expect(buton, 'rezervasyon butonu görünmeli').toBeVisible();

    // ── 2) Talep modalı: öneri 30, gönder ──
    await buton.click();
    await expect(page.locator('#mipres-overlay')).toBeVisible();
    const oneri = await page.locator('.mipres-qty').inputValue();
    expect(Number(oneri), 'önerilen miktar kayıtlı stok (30)').toBe(30);
    const ozet = await page.locator('#mipres-summary').innerText();
    console.log('  talep modal özeti:', ozet);
    expect(ozet).toContain('30');
    await page.fill('#mipres-note', 'Playwright smoke talebi');
    await page.click('#mipres-confirm');
    await page.waitForTimeout(1500);
    await expect(page.locator('#mipres-overlay')).toHaveCount(0);

    // Satır artık "Depoda bekliyor" (disabled) olmalı; API'de REQUESTED kayıt
    const bekleyenBtn = page.locator('#mip-list button', { hasText: 'Depoda bekliyor' });
    await expect(bekleyenBtn).toBeVisible();
    await expect(bekleyenBtn).toBeDisabled();
    const talepler = (await api(page, 'GET', '/warehouse-reservations')).body.data
      .filter(r => r.project_name === projeAdi);
    console.log('  oluşan talep:', JSON.stringify(talepler.map(r =>
      ({ q: r.requested_qty, s: r.status, by: r.requested_by }))));
    expect(talepler.length, 'tek talep oluşmalı').toBe(1);
    expect(talepler[0].status).toBe('REQUESTED');
    expect(Number(talepler[0].requested_qty)).toBe(30);
    const resId = talepler[0].id;

    // ── 3) Depo sekmesi: talep listede, sayım/onay modalı ──
    await page.evaluate(() => switchTab('warehouse'));
    await page.waitForTimeout(1500);
    const listeMetni = await page.locator('#wh-reservations').innerText();
    console.log('  depo talep listesi:', listeMetni.replace(/\s+/g, ' ').slice(0, 200));
    expect(listeMetni).toContain('MIPR M12 Civata');
    expect(listeMetni).toContain('kayıtlı stok: 30');
    await page.locator('#wh-reservations button', { hasText: 'Sayım / Onay' }).click();
    await expect(page.locator('#wres-overlay')).toBeVisible();

    // Tam onay önerisi 30; sayımda 15 çıktı → 15 gir, açıklama alanı AÇILMALI
    expect(Number(await page.locator('#wres-qty').inputValue()), 'onay önerisi 30').toBe(30);
    await page.fill('#wres-qty', '15');
    await expect(page.locator('#wres-reason-wrap'), 'kısmi onayda açıklama alanı açılır').toBeVisible();
    const wresOzet = await page.locator('#wres-summary').innerText();
    console.log('  onay modal özeti:', wresOzet);
    expect(wresOzet).toContain('15');
    expect(wresOzet).toContain('satın almaya');
    // 9. tur M8: tek checkbox yerine 3'lü radio — kısmi onayda varsayılan
    // "eksik kadar düş"; tam redde (onay 0) varsayılan "kaydı sıfırla".
    await expect(page.locator('input[name="wres-adj-mode"][value="shortage"]'),
      'kısmi onayda varsayılan: eksik kadar düş').toBeChecked();
    const adjOnizleme = await page.locator('#wres-adj-preview').innerText();
    expect(adjOnizleme, 'düşülecek miktar önizlemesi görünür').toContain('15');

    // Açıklamasız onay reddedilmeli (toast, modal açık kalır)
    await page.click('#wres-confirm');
    await page.waitForTimeout(800);
    await expect(page.locator('#wres-overlay'), 'açıklamasız kısmi onay modalı KAPATMAMALI').toBeVisible();

    await page.fill('#wres-reason', 'Sayımda 15 çıktı — kayıp/envanter yanlış');
    await page.click('#wres-confirm');
    await page.waitForTimeout(2000);
    await expect(page.locator('#wres-overlay')).toHaveCount(0);

    // ── 4) Sonuçlar: rezervasyon PARTIAL, OUT'lar, satın alma kalemi ──
    const sonRes = (await api(page, 'GET', '/warehouse-reservations')).body.data
      .find(r => r.id === resId);
    console.log('  onay sonrası talep:', JSON.stringify(
      { s: sonRes.status, onay: sonRes.approved_qty, neden: sonRes.shortage_reason }));
    expect(sonRes.status).toBe('PARTIAL');
    expect(Number(sonRes.approved_qty)).toBe(15);

    const mvs = (await api(page, 'GET', '/warehouse-movements')).body.data
      .filter(m => m.reservation_id === resId);
    console.log('  rezervasyon hareketleri:', JSON.stringify(mvs.map(m =>
      ({ t: m.source_type, q: m.quantity, yon: m.movement_type }))));
    const out = mvs.find(m => m.source_type === 'RESERVATION');
    const adj = mvs.find(m => m.source_type === 'RESERVATION_ADJUST');
    expect(out && out.movement_type === 'OUT' && Number(out.quantity) === 15,
      'RESERVATION OUT 15').toBeTruthy();
    expect(adj && adj.movement_type === 'OUT' && Number(adj.quantity) === 15,
      'RESERVATION_ADJUST OUT 15 (hayalet stok düzeltmesi)').toBeTruthy();

    const eksikKalem = (await api(page, 'GET', '/purchase-items')).body.data
      .find(i => i.project_name === projeAdi && i.status === 'PLANNED');
    console.log('  satın almaya düşen:', JSON.stringify(eksikKalem &&
      { q: eksikKalem.quantity, not: (eksikKalem.notes || '').slice(0, 60) }));
    expect(eksikKalem, 'eksik satın almaya düşmeli').toBeTruthy();
    expect(Number(eksikKalem.quantity)).toBe(15);

    // Depo listesi sonuçlananı rozetiyle göstermeli
    const sonListe = await page.locator('#wh-reservations').innerText();
    expect(sonListe).toContain('Kısmi onay');
    expect(sonListe).toContain('onaylanan: 15');

    // ── 5) MİP tekrar: Rezerve 15, stok 0, eksik 15 (çifte sayım YOK) ──
    await page.evaluate(() => switchTab('mip'));
    await page.waitForTimeout(1500);
    await page.selectOption('#mip-project-sel', projeAdi);
    await page.waitForTimeout(1500);
    const sonra = await page.evaluate(() => _mipRows[0] && ({
      stockTotal: _mipRows[0].stockTotal, reserved: _mipRows[0].reserved,
      pendingReserve: _mipRows[0].pendingReserve, missing: _mipRows[0].missing,
      status: _mipRows[0].status }));
    console.log('  onay sonrası MİP satırı:', JSON.stringify(sonra));
    expect(sonra.stockTotal, 'net stok 0 (30−15−15)').toBe(0);
    expect(sonra.reserved, 'rezerve 15').toBe(15);
    expect(sonra.pendingReserve, 'bekleyen talep kalmadı').toBe(0);
    expect(sonra.missing, 'eksik 15 — rezerve çifte sayılmadı').toBe(15);
    const mipMetin = await page.locator('#mip-list').innerText();
    expect(mipMetin).toContain('Rezerve');
    expect(mipMetin).toContain('depoya rezerve');

    expect(hatalar, 'JS hatası olmamalı').toEqual([]);
  } finally {
    // Rezervasyonlar HAREKETLERDEN ÖNCE: bağlı hareket rezervasyon yaşarken silinemez
    for (const r of (await api(page, 'GET', '/warehouse-reservations')).body.data.filter(x => x.project_name === projeAdi))
      await api(page, 'DELETE', '/warehouse-reservations/' + r.id);
    for (const m of (await api(page, 'GET', '/warehouse-movements')).body.data.filter(x => x.item_code === kod))
      await api(page, 'DELETE', '/warehouse-movements/' + m.id);
    for (const i of (await api(page, 'GET', '/purchase-items')).body.data.filter(x => x.project_name === projeAdi))
      await api(page, 'DELETE', '/purchase-items/' + i.id);
    for (const pt of (await api(page, 'GET', '/parts')).body.data.filter(x => x.project_name === projeAdi))
      await api(page, 'DELETE', '/parts/' + pt.id);
    for (const p of (await api(page, 'GET', '/project-bom-parts')).body.data.filter(x => x.project_bom_id === pbomId))
      await api(page, 'DELETE', '/project-bom-parts/' + p.id);
    await api(page, 'DELETE', '/project-bom/' + pbomId);
    await api(page, 'DELETE', '/warehouses/' + whId);
    await api(page, 'DELETE', '/bom-parts/' + partId);
    await api(page, 'DELETE', '/bom-products/' + prodId);
    await api(page, 'DELETE', '/orders/' + orderId);
  }
});
