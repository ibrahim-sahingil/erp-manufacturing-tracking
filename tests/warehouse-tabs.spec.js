// (11. tur F1 bekçisi) Depo alt sekme GÖRÜNÜRLÜĞÜ — gerçek tarayıcıda.
// 10. turda whAllowedSubTabs'ın developer listesinde 'additem' unutuldu ve
// "Parça Ekle" sekmesi (içindeki katalog araması + Aktarım Başlat dahil)
// geliştiriciye display:none oldu; hiçbir sunucusuz test bunu göremedi
// (izin LİSTESİ İÇERİĞİ hatası). Bu spec o sınıfın bekçisidir.
// Sunucu 8080'de çalışır durumda olmalı.
const { test, expect } = require('@playwright/test');

async function login(page) {
  await page.goto('/');
  await page.fill('#login-username', 'testdev');
  await page.fill('#login-password', 'test1234');
  await page.click('#login-btn');
  await expect(page.locator('#user-badge')).toBeVisible({ timeout: 15000 });
}

test('depo: geliştirici 6 alt sekmenin 6\'sını da görür (Özet + Parça Ekle dahil)', async ({ page }) => {
  await login(page);
  await page.click('.nav-tab[data-tab="warehouse"]');
  const tabs = ['dash', 'view', 'receiving', 'additem', 'manage', 'delivery']; // (16. tur M1c) dash eklendi
  for (const t of tabs) {
    await expect(page.locator(`#warehouse-tabs .planning-tab[data-whtab="${t}"]`),
      `data-whtab="${t}" görünür olmalı`).toBeVisible();
  }
});

test('depo: Parça Ekle tıklanınca manuel hareket formu (katalog araması dahil) açılır', async ({ page }) => {
  await login(page);
  await page.click('.nav-tab[data-tab="warehouse"]');
  await page.click('#warehouse-tabs .planning-tab[data-whtab="additem"]');
  await expect(page.locator('#wh-tab-additem')).toBeVisible();
  await expect(page.locator('#whm-mv-name')).toBeVisible();      // malzeme adı girişi
  // searchSelect host'u önce HIDDEN input basar — görünür olan text input'tur
  await expect(page.locator('#whm-mv-item-ss input:not([type="hidden"])').first()).toBeVisible({ timeout: 10000 });
});
