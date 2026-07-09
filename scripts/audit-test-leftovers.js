// TEST ARTIĞI BEKÇİSİ — SALT OKUNUR (hiçbir şey silmez).
//
// Test scriptleri izole kayıtlar kurup arkasını temizler. Ama bir temizlik adımı
// sessizce başarısız olursa (ör. BomProductService parçası olan ürünü SİLMEZ ve
// hata `.catch(()=>{})` ile yutulur) artıklar DB'de birikir. 2026-07-10'da
// e2e-test.js'in bıraktığı 33 "E2E Ürün" + 160 şablon parçası bulundu; e2e'nin
// kendi doğrulaması bunları SAYMADIĞI için "0 artık kayıt" diyordu.
//
// Bu bekçi, test öneklerine sahip kayıt kalıp kalmadığını kontrol eder.
// Kullanım: node scripts/audit-test-leftovers.js   (sunucu 8080'de çalışmalı)
const BASE = 'http://localhost:8080/api/';

// Test scriptlerinin ürettiği önekler (gerçek veri bu öneklerle başlamamalı)
const ONEK = /^(E2E|AUDIT|AUTHZ|CASC|CSC|DBLCLK|DELFB|DEPT|DBG|HACK|MIP|PBRM|PRB)/i;

let TOK;
const get = async ep => {
  const r = await fetch(BASE + ep, { headers: { Authorization: 'Bearer ' + TOK } });
  if (!r.ok) throw new Error(ep + ' → HTTP ' + r.status);
  const j = await r.json();
  return Array.isArray(j.data) ? j.data : [];
};
const testMi = x => [x.code, x.name, x.project_name, x.custom_code, x.custom_name,
                     x.item_code, x.item_name, x.username].some(v => ONEK.test(v || ''));

(async () => {
  const lr = await fetch(BASE + 'auth/login', { method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'testdev', password: 'test1234' }) }).then(r => r.json());
  TOK = lr.data && lr.data.token;
  if (!TOK) throw new Error('testdev login başarısız — sunucu ayakta mı?');

  const tablolar = ['parts', 'bom-parts', 'bom-products', 'project-bom-parts',
                    'bom-operations', 'orders', 'purchase-items', 'warehouses',
                    'warehouse-movements', 'warehouse-reservations'];
  let toplam = 0;
  for (const ep of tablolar) {
    const kalan = (await get(ep)).filter(testMi);
    if (kalan.length) {
      toplam += kalan.length;
      const ornek = kalan.slice(0, 3)
        .map(x => x.code || x.custom_code || x.name || x.project_name || x.item_code).join(', ');
      console.log(`  ❌ ${ep}: ${kalan.length} artık — ör. ${ornek}${kalan.length > 3 ? ' …' : ''}`);
    } else {
      console.log(`  ✅ ${ep}: temiz`);
    }
  }

  console.log('\n' + '─'.repeat(60));
  if (toplam) {
    console.log(`❌ ${toplam} artık test kaydı — bir test scriptinin temizliği sızdırıyor.`);
    process.exit(1);
  }
  console.log('✅ Artık test kaydı yok.');
})().catch(e => { console.error('HATA:', e.message); process.exit(1); });
