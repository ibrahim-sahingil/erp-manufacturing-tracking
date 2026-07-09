// DUMAN TESTİ — 12 sekmeyi gezer, konsol/sayfa/5xx hatası toplar; izole bir
// ürün üzerinde form akışlarını ve uç girdileri (boş form, negatif adet, 5000
// karakter ad, mükerrer kod) dener, arkasını temizler.
//
// 5. denetim turunda keşif aracı olarak yazıldı, kalıcılaştırıldı: yeni bir
// sekme/render eklendiğinde sessiz konsol hatalarını yakalayan tek test budur.
// Kasıtlı kötü girdilere backend'in 400 dönmesi BEKLENEN davranıştır (beklenen[]
// listesine düşer, testi kırmaz); gerçek hata = pageerror, 5xx, düşen istek.
const { test, expect } = require('@playwright/test');

const TABS = ['dashboard','parts','users','stats','orders','purchasing',
              'warehouse','delivery','planning','bom','docs','appusers'];

// Kasıtlı kötü girdi denemelerimize backend'in 400 dönmesi DOĞRU davranıştır;
// bunlar hata sayılmaz (beklenen[] listesine düşer). Gerçek hata: pageerror,
// 5xx ve doğrulama kaynaklı olmayan konsol hataları.
const BEKLENEN_400 = /status of 400|Dogrulama hatasi|Doğrulama hatası/i;
function attachCollectors(page, bag, beklenen){
  page.on('console', m => {
    if(m.type()!=='error') return;
    const t=m.text().slice(0,300);
    (BEKLENEN_400.test(t) ? beklenen : bag).push('[konsol] '+t);
  });
  page.on('pageerror', e => bag.push('[pageerror] '+String(e).slice(0,300)));
  page.on('response', r => {
    if(r.status()>=500) bag.push('[http '+r.status()+'] '+r.url());
  });
  page.on('requestfailed', r => {
    const f=(r.failure()||{}).errorText||'';
    if(!f.includes('ERR_ABORTED')) bag.push('[istek düştü] '+r.url()+' '+f);
  });
}

async function login(page){
  await page.goto('/');
  await page.fill('#login-username','testdev');
  await page.fill('#login-password','test1234');
  await page.click('#login-btn');
  await expect(page.locator('#user-badge')).toBeVisible({ timeout: 15000 });
}

async function api(page, method, path, body){
  return await page.evaluate(async ([m,p,b])=>{
    const tok=sessionStorage.getItem('ut_token');
    const r=await fetch('/api'+p,{method:m,
      headers:{Authorization:'Bearer '+tok,'Content-Type':'application/json'},
      body:b?JSON.stringify(b):undefined});
    let j=null; try{ j=await r.json(); }catch(e){}
    return {status:r.status, body:j};
  },[method,path,body||null]);
}

test('keşif: tüm sekmeler gezilir — konsol/sayfa/HTTP hatası çıkmaz', async ({ page }) => {
  const errors=[], beklenen=[];
  attachCollectors(page, errors, beklenen);
  await login(page);
  for(const tab of TABS){
    await page.evaluate(t=>switchTab(t),tab);
    await page.waitForTimeout(1200); // render + fetch'lerin oturması
  }
  expect(errors, 'Sekme turunda toplanan hatalar:\n'+errors.join('\n')).toEqual([]);
});

test('keşif: BOM izole üründe form akışları + 6. tur UI + uç girdiler', async ({ page }) => {
  const errors=[], beklenen=[];
  attachCollectors(page, errors, beklenen);
  await login(page);

  // İzole test ürünü kur
  const code='AUDIT-'+Date.now().toString(36);
  const prod=await api(page,'POST','/bom-products',{name:'AUDIT-UI Ürün',code});
  expect([200,201],'test ürünü oluşmalı').toContain(prod.status);
  const prodId=prod.body.data.id;
  const bulgular=[];

  try{
    await page.evaluate(()=>switchTab('bom'));
    await page.waitForTimeout(1000);
    await page.selectOption('#bom-product-select', prodId);
    await page.waitForTimeout(800);
    await expect(page.locator('#bom-content')).toBeVisible();

    // 6. tur #1: Üst Parça kutusunda ▾ oku var ve tıklayınca liste açılıyor
    await expect(page.locator('#bom-part-parent-ss .ss-arrow')).toBeVisible();
    await page.click('#bom-part-parent-ss .ss-arrow');
    await expect(page.locator('#bom-part-parent-list')).toBeVisible();
    await page.keyboard.press('Escape');

    // 6. tur #2: İşlem Seç listesi — boşsa yönlendirme, doluysa öğe listesi
    await page.click('#bom-op-def-select-txt');
    await expect(page.locator('#bom-op-def-select-list')).toBeVisible();
    const opListText=await page.locator('#bom-op-def-select-list').innerText();
    if(!opListText.trim()) bulgular.push('İşlem Seç listesi görünür ama İÇİ BOŞ (ne öğe ne yönlendirme)');
    await page.keyboard.press('Escape');

    // 6. tur #3: mod select 3 seçenekli
    expect(await page.locator('#bom-op-mode option').count()).toBe(3);

    // 6. tur #4: Excel ön-modalı
    await page.click('button:has-text("Excel\'den İçe Aktar")');
    await expect(page.locator('#xls-import-start-overlay')).toBeVisible();
    await expect(page.locator('#xls-import-start-overlay button:has-text("Örnek Excel")')).toBeVisible();
    await page.click('#xls-import-start-overlay button:has-text("İptal")');

    // Boş parça formu: eklenmemeli, toast çıkmalı
    await page.click('#bom-add-part-btn');
    await page.waitForTimeout(400);
    const t1=await page.locator('#toast').innerText().catch(()=>'');
    let parts=await api(page,'GET','/bom-parts');
    const countAfterEmpty=(parts.body.data||[]).filter(p=>p.product_id===prodId).length;
    if(countAfterEmpty!==0) bulgular.push('BOŞ formla parça EKLENDİ (doğrulama yok)');
    if(!t1) bulgular.push('Boş form gönderiminde kullanıcıya toast uyarısı çıkmadı');

    // NEGATİF adet denemesi (UI üzerinden)
    await page.fill('#bom-part-name','Audit Negatif');
    await page.fill('#bom-part-code','AUD-NEG');
    await page.fill('#bom-part-qty','-5');
    await page.click('#bom-add-part-btn');
    await page.waitForTimeout(700);
    parts=await api(page,'GET','/bom-parts');
    const neg=(parts.body.data||[]).find(p=>p.product_id===prodId&&p.code==='AUD-NEG');
    if(neg) bulgular.push('NEGATİF adetli parça kabul edildi (qty='+neg.quantity+')');

    // Normal parça: eklenebilmeli
    await page.fill('#bom-part-name','Audit Gövde');
    await page.fill('#bom-part-code','AUD-001');
    await page.fill('#bom-part-qty','2');
    await page.click('#bom-add-part-btn');
    await page.waitForTimeout(900);
    parts=await api(page,'GET','/bom-parts');
    const ok=(parts.body.data||[]).find(p=>p.product_id===prodId&&p.code==='AUD-001');
    expect(ok,'normal parça eklenebilmeli').toBeTruthy();

    // ÇOK UZUN isim (5000 karakter) — API doğrudan
    const longName='U'.repeat(5000);
    const long=await api(page,'POST','/bom-parts',{product_id:prodId,name:longName,code:'AUD-LONG',quantity:1,unit:'adet'});
    if(long.status===200||long.status===201) bulgular.push('5000 karakterlik parça adı KABUL edildi (limit yok)');
    else if(long.status>=500) bulgular.push('5000 karakterlik ad 500 döndürdü (dostça 400 yerine): '+JSON.stringify(long.body).slice(0,150));

    // Aynı koda İKİNCİ parça (kardeş-kod mükerrerliği) — API doğrudan
    const dup=await api(page,'POST','/bom-parts',{product_id:prodId,name:'Audit Kopya',code:'AUD-001',quantity:1,unit:'adet'});
    if(dup.status===200||dup.status===201) bulgular.push('Aynı ürün altında AYNI KODLA ikinci parça API\'den kabul edildi');
  } finally {
    // Temizlik: ürünün parçaları + ürün
    const parts=await api(page,'GET','/bom-parts');
    for(const p of (parts.body.data||[]).filter(x=>x.product_id===prodId))
      await api(page,'DELETE','/bom-parts/'+p.id);
    await api(page,'DELETE','/bom-products/'+prodId);
  }

  console.log('\n=== KEŞİF BULGULARI ===');
  (bulgular.length?bulgular:['(davranış bulgusu yok)']).forEach(b=>console.log('• '+b));
  console.log('=== BEKLENEN DOĞRULAMA RETLERİ (backend doğru çalışıyor) ===');
  (beklenen.length?beklenen:['(yok)']).forEach(b=>console.log('• '+b));
  console.log('=== GERÇEK KONSOL/HTTP HATALARI ===');
  (errors.length?errors:['(yok)']).forEach(b=>console.log('• '+b));
  // Bulgular test'i KIRMAZ — rapor içindir. Sadece konsol/sayfa hataları kırar.
  expect(errors,'Konsol/HTTP hataları:\n'+errors.join('\n')).toEqual([]);
});
