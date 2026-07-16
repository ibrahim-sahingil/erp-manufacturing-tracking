// (13. tur madde 1) Bölümler MANUEL — işlem tanımındaki bölüm hiçbir yere
// otomatik gitmez, projede bölüm otomatik OLUŞMAZ.
//
// Zincir (yeni):
//   → şablon ağacındaki işlemli parça projeye kopyalanırken dept_id NULL kalır
//   → kullanıcı bölümü elle kaydeder (pbomeAddDeptQuick / Bölümler modalı)
//     ve parçaya elle atar (pbomeSetDept)
//   → yayınlanınca ELLE atanan bölüm üretim parçasına (parts.department_id)
//     taşınır; republish boş bölümü doldurur (fill-only), doluyu ezmez.
//
// (7. tur #1 otomatik türetme davranışı bilinçli geri alındı — arkadaş:
// "bölümler manuel liste olsun, kişi bölüm kaydetsin sonra parçaya atasın".)
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

test('bölümler manuel: kopyalamada otomatik bölüm yok; elle atama yayına taşınır', async ({ page }) => {
  await login(page);
  const sfx = Date.now().toString(36);
  const projeAdi = 'DEPT-PROJE-' + sfx;
  const bolumAdi = 'Kaynakhane-' + sfx;   // opdef'te duran MİRAS değer — hiçbir yere gitmemeli
  const opKodu = 'DW' + sfx.slice(-3).toUpperCase();

  const opdef = await api(page, 'POST', '/bom-operations',
    { name: 'DEPT Kaynak', code: opKodu, department_name: bolumAdi });
  const opId = opdef.body.data.id;
  const order = await api(page, 'POST', '/orders', { project_name: projeAdi, customer_name: 'DEPT' });
  const orderId = order.body.data.id;
  // Proje API ile (adapter dışında) açıldı → sayfanın 30 sn'lik referans cache'i
  // bayat kalır ve dbInsert('parts',{project}) order_id'yi null yazar. Tazele.
  await page.evaluate(() => invalidateRef('orders'));
  const prod = await api(page, 'POST', '/bom-products', { name: 'DEPT Ürün', code: 'DEPT-' + sfx });
  const prodId = prod.body.data.id;
  // Şablonda işlemli parça (işlem tanımında bölüm adı DURUYOR — inert olmalı)
  const islemli = await api(page, 'POST', '/bom-parts', {
    product_id: prodId, name: 'DEPT Gövde', code: 'DEPTG-' + sfx + opKodu,
    quantity: 2, unit: 'adet', level: 0, material_kind: 'YARI_MAMUL',
    operations: [{ name: 'DEPT Kaynak', code: opKodu, desc: '', duration_per_unit: 0, total_duration: 0 }]
  });
  const islemliId = islemli.body.data.id;
  const sade = await api(page, 'POST', '/bom-parts', {
    product_id: prodId, name: 'DEPT Sade', code: 'DEPTS-' + sfx,
    quantity: 1, unit: 'adet', level: 0, material_kind: 'YARI_MAMUL'
  });
  const sadeId = sade.body.data.id;

  const pbom = await api(page, 'POST', '/project-bom', { project_name: projeAdi, bom_product_id: prodId });
  const pbomId = pbom.body.data.id;

  try {
    // Editörü aç → şablon parçaları kopyalanır (dept_id NULL kalmalı)
    await page.evaluate(() => switchTab('bom'));
    await page.waitForTimeout(1200);
    await page.evaluate(id => openPbomEditor(id), pbomId);
    await page.waitForTimeout(2500);

    // (13. tur) İşlem tanımındaki bölüm projede OLUŞTURULMAMALI
    const bolumler = (await api(page, 'GET', '/departments')).body.data
      .filter(d => d.order_id === orderId);
    console.log('  projede bölümler (boş olmalı):', JSON.stringify(bolumler.map(d => d.name)));
    expect(bolumler.find(d => d.name === bolumAdi),
      'işlem tanımındaki bölüm projede OTOMATİK OLUŞMAMALI').toBeFalsy();

    // Kopyalanan parçalarda dept_id NULL olmalı
    const pbps = (await api(page, 'GET', '/project-bom-parts')).body.data
      .filter(x => x.project_bom_id === pbomId);
    const etkinKod = p => p.custom_code || p.resolved_code || '';
    const pIslemli = pbps.find(p => etkinKod(p).includes(opKodu));
    const pSade = pbps.find(p => etkinKod(p).startsWith('DEPTS-'));
    expect(pbps.length, 'iki parça kopyalanmalı').toBe(2);
    expect(pIslemli.dept_id, 'işlemli parça da bölümsüz kopyalanmalı').toBeFalsy();
    expect(pSade.dept_id, 'işlemsiz parça bölümsüz kalmalı').toBeFalsy();

    // ELLE bölüm kaydet (pbomeAddDeptQuick — "+ Yeni bölüm…" / Bölümler modalı çekirdeği)
    const elleDeptId = await page.evaluate(async ad => await pbomeAddDeptQuick(ad), bolumAdi);
    expect(elleDeptId, 'elle bölüm kaydı oluşmalı').toBeTruthy();
    // ELLE parçaya ata (pbomeSetDept — satırdaki dropdown çekirdeği)
    await page.evaluate(([pid, did]) => pbomeSetDept(pid, did), [pIslemli.id, elleDeptId]);
    await page.waitForTimeout(800);
    const pIslemli2 = (await api(page, 'GET', '/project-bom-parts/' + pIslemli.id)).body.data;
    expect(pIslemli2.dept_id, 'elle atama kaydolmalı').toBe(elleDeptId);

    // (9. tur M4) Yayın öncesi karar: her iki parça YARI_MAMUL → PRODUCE
    await api(page, 'POST', '/project-bom-parts/decisions',
      { items: pbps.map(p => ({ id: p.id, decision: 'PRODUCE' })), decided_by: 'TEST' });

    // Yayınla → üretim parçası ELLE atanan bölümü devralmalı
    const yayinSonuc = await page.evaluate(async () => {
      const pb = projectBoms.find(p => p.id === _activePbomId);
      const prod = bomProducts.find(p => p.id === pb.bom_product_id);
      const fresh = await dbGet('project_bom_parts', 'project_bom_id=eq.' + _activePbomId);
      const tpl = await dbGet('bom_parts', 'product_id=eq.' + prod.id);
      return await pbomPublishParts(pb, fresh, tpl);
    });
    console.log('  yayınlama sonucu:', JSON.stringify(yayinSonuc));
    await page.waitForTimeout(2000);

    const uretim = (await api(page, 'GET', '/parts')).body.data
      .filter(p => (p.code || '').includes(sfx));
    expect(uretim.length, 'iki parça üretime çıkmalı').toBe(2);
    expect(uretim[0].order_id, 'üretim parçası projeye bağlanmalı').toBe(orderId);
    const uIslemli = uretim.find(p => (p.code || '').includes(opKodu));
    expect(uIslemli, 'işlemli parça üretime çıkmalı').toBeTruthy();
    expect(uIslemli.department_id, 'üretim parçası ELLE atanan bölümü devralmalı').toBe(elleDeptId);
    const uSade = uretim.find(p => (p.code || '').startsWith('DEPTS-'));
    expect(uSade && uSade.department_id, 'atanmamış parça bölümsüz kalmalı').toBeFalsy();

    // Republish fill-only backfill KORUNDU: boşaltılan bölüm yeniden dolmalı
    await api(page, 'PUT', '/parts/' + uIslemli.id, { department_id: null });
    const yayin2 = await page.evaluate(async () => {
      const pb = projectBoms.find(p => p.id === _activePbomId);
      const prod = bomProducts.find(p => p.id === pb.bom_product_id);
      const fresh = await dbGet('project_bom_parts', 'project_bom_id=eq.' + _activePbomId);
      const tpl = await dbGet('bom_parts', 'product_id=eq.' + prod.id);
      globalThis.parts = await dbGet('parts');
      return await pbomPublishParts(pb, fresh, tpl);
    });
    console.log('  republish sonucu:', JSON.stringify(yayin2));
    const uIslemli2 = (await api(page, 'GET', '/parts')).body.data.find(p => p.id === uIslemli.id);
    expect(uIslemli2.department_id, 'republish boş bölümü doldurmalı (fill-only)').toBe(elleDeptId);
  } finally {
    for (const p of (await api(page, 'GET', '/parts')).body.data.filter(x => (x.code || '').includes(sfx)))
      await api(page, 'DELETE', '/parts/' + p.id);
    const pbps = (await api(page, 'GET', '/project-bom-parts')).body.data.filter(x => x.project_bom_id === pbomId);
    for (const p of pbps.sort((a, b) => (b.level || 0) - (a.level || 0)))
      await api(page, 'DELETE', '/project-bom-parts/' + p.id);
    await api(page, 'DELETE', '/project-bom/' + pbomId);
    await api(page, 'DELETE', '/bom-parts/' + islemliId);
    await api(page, 'DELETE', '/bom-parts/' + sadeId);
    await api(page, 'DELETE', '/bom-products/' + prodId);
    await api(page, 'DELETE', '/bom-operations/' + opId);
    for (const d of (await api(page, 'GET', '/departments')).body.data.filter(x => x.order_id === orderId))
      await api(page, 'DELETE', '/departments/' + d.id);
    await api(page, 'DELETE', '/orders/' + orderId);
  }
});
