// Sevkiyat ekranı görüntüsü (13. tur madde 4 sonuç kanıtı) + temel canlı akış:
// gerçek bir projede ağaç görünümü, paket açma, satır ekleme (drop çekirdeği
// dbInsert ile), paket kapatma denenir; görüntüler alınır, DEMO paket SİLİNİR.
// Kullanım: node scripts/shot-shipping.js <klasör>
const { chromium } = require('@playwright/test');
const out = process.argv[2] || '.';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  page.on('pageerror', e => { console.error('JS HATASI: ' + e.message); process.exitCode = 1; });
  await page.goto('http://localhost:8080/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await page.waitForSelector('#user-badge', { state: 'visible', timeout: 15000 });
  await page.waitForTimeout(2000);

  await page.evaluate(() => switchTab('shipping'));
  await page.waitForTimeout(2500);
  // İlk gerçek projeyi seç
  const proj = await page.evaluate(() => {
    const sel = document.getElementById('ship-project-sel');
    const opt = [...sel.options].find(o => o.value);
    if (!opt) return null;
    sel.value = opt.value; shipRenderActive();
    return opt.value;
  });
  if (!proj) { console.error('Sevk edilecek proje yok'); process.exit(1); }
  await page.waitForTimeout(1200);
  await page.screenshot({ path: out + '/sevkiyat-agac.png' });

  // Paketleme sekmesi: paket aç → ilk satırı pakete ekle (drop çekirdeği) → kapat
  const sonuc = await page.evaluate(async () => {
    switchShippingTab('pack');
    const d = await dbInsert('shipment_packages', { project_name: document.getElementById('ship-project-sel').value, name: 'DEMO Paket — Görüntü', length_cm: 120, width_cm: 80, height_cm: 60, weight_kg: 42.5 });
    const pkg = d && d[0];
    if (!pkg) return { hata: 'paket oluşmadı' };
    shipmentPackages.push(pkg);
    const rows = _shipRows(document.getElementById('ship-project-sel').value);
    const r = [...rows.partRows, ...rows.purRows][0];
    let satir = null;
    if (r) {
      const di = await dbInsert('shipment_package_items', { package_id: pkg.id, part_id: r.partId || null, project_bom_part_id: r.pbpId || null, item_name: r.name, item_code: r.code || null, quantity: 1, unit: r.unit || 'adet' });
      satir = di && di[0];
      if (satir) shipmentPackageItems.push(satir);
    }
    shipRenderPacking();
    return { pkgId: pkg.id, pkgNo: pkg.package_no, satir: !!satir };
  });
  console.log('demo paket:', JSON.stringify(sonuc));
  await page.waitForTimeout(1200);
  await page.screenshot({ path: out + '/sevkiyat-paketleme.png' });

  // Temizlik: demo paket (satır CASCADE)
  if (sonuc.pkgId) await page.evaluate(async id => { await dbDelete('shipment_packages', id); }, sonuc.pkgId);
  await browser.close();
  console.log('Görüntüler alındı, DEMO paket silindi → ' + out);
})().catch(e => { console.error(e.message); process.exit(2); });
