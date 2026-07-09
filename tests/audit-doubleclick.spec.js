// Çift tıklama / mükerrer kayıt regresyon testi (5. denetim turu, Bölüm 2).
//
// Bulunan hata: "⚙ İşlem Ekle" butonu await bitene kadar kilitlenmiyordu.
// Hızlı çift tıklamada ikinci istek birincinin cevabı dönmeden gidiyor ve ağaca
// İKİ parça ekleniyordu (canlı olarak kanıtlandı: 2 kayıt). Kod-çakışma
// kontrolü (findSiblingDup) bunu yakalayamaz, çünkü yerel `bomParts` dizisi
// birinci istek dönmeden güncellenmiyor. Çözüm: btnBusy() kilidi.
//
// Bu test hem korumalı ("+ Parça Ekle") hem de düzeltilen ("⚙ İşlem Ekle")
// butonun çift tıklamada TEK kayıt ürettiğini doğrular.
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

test('çift tıklama: kaydetme butonları mükerrer kayıt oluşturmaz', async ({ page }) => {
  await login(page);
  const sfx = Date.now().toString(36);
  const prod = await api(page, 'POST', '/bom-products', { name: 'DBLCLK Ürün', code: 'DBL-' + sfx });
  const prodId = prod.body.data.id;
  const opdef = await api(page, 'POST', '/bom-operations', { name: 'DBLCLK İşlem', code: '-DX' + sfx.slice(-2) });
  const opId = opdef.body.data.id;

  try {
    await page.evaluate(() => switchTab('bom'));
    await page.waitForTimeout(800);
    await page.selectOption('#bom-product-select', prodId);
    await page.waitForTimeout(600);

    // Kök parça (işlem bunun üzerine uygulanacak)
    await page.fill('#bom-part-name', 'DBLCLK Gövde');
    await page.fill('#bom-part-code', 'DBLG-' + sfx);
    await page.click('#bom-add-part-btn');
    await page.waitForTimeout(900);
    let parts = (await api(page, 'GET', '/bom-parts')).body.data.filter(p => p.product_id === prodId);
    expect(parts.length, 'kök parça eklenmeli').toBe(1);

    // 1) "+ Parça Ekle" — eskiden beri korumalı
    await page.fill('#bom-part-name', 'DBLCLK İkinci');
    await page.fill('#bom-part-code', 'DBLP-' + sfx);
    await page.locator('#bom-add-part-btn').click({ clickCount: 2, delay: 10 }).catch(() => {});
    await page.waitForTimeout(1500);
    parts = (await api(page, 'GET', '/bom-parts')).body.data.filter(p => p.product_id === prodId);
    expect(parts.filter(p => p.code === 'DBLP-' + sfx).length,
      '"+ Parça Ekle" çift tıklamada TEK kayıt oluşturmalı').toBe(1);

    // 2) "⚙ İşlem Ekle" — 5. denetim turunda btnBusy ile korundu
    const oncekiSayi = parts.length;
    await page.evaluate(id => ssSet('bom-op-part', id), parts[0].id);
    await page.evaluate(id => ssSet('bom-op-def-select', id), opId);
    await page.waitForTimeout(300);
    await page.locator('#bom-add-op-btn').click({ clickCount: 2, delay: 10 }).catch(() => {});
    await page.waitForTimeout(2000);
    parts = (await api(page, 'GET', '/bom-parts')).body.data.filter(p => p.product_id === prodId);
    expect(parts.length - oncekiSayi,
      '"⚙ İşlem Ekle" çift tıklamada ağaca TEK parça eklemeli').toBe(1);
  } finally {
    const parts = (await api(page, 'GET', '/bom-parts')).body.data.filter(p => p.product_id === prodId);
    for (const p of parts.sort((a, b) => (b.level || 0) - (a.level || 0)))
      await api(page, 'DELETE', '/bom-parts/' + p.id);
    await api(page, 'DELETE', '/bom-products/' + prodId);
    await api(page, 'DELETE', '/bom-operations/' + opId);
  }
});
