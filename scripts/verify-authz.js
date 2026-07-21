// (5. denetim turu) MODUL BAZLI YAZMA KILIDI regresyon testi.
//
// K3 turu yalnizca orders + users icin kilit koymustu; diger 12 modulun
// POST/PUT/DELETE uclari "sadece giris yapmis olmak" ile aciktir sanilmiyordu
// ama canli denemede 11 acik kapi bulundu: 'dashboard' yetkili bir isci hesabi
// urun agacini, depolari, malzeme/tedarikci/bolum kartlarini SILEBILIYORDU.
//
// Bu test, kisitli bir kullanici olusturup yikici uclari tek tek dener ve
// hepsinin 403 ile reddedilmesini bekler. OKUMA (GET) bilincli olarak acik
// kaldigindan dogrulanmaz. Uretim akisi uclari (part-logs, parts, work-orders,
// warehouse-movements, purchase-items) KAPSAM DISIDIR — QR okutan isci yazar.
//
// Kullanim: node scripts/verify-authz.js        (sunucu 8080'de calisir olmali)
const BASE = 'http://localhost:8080/api/';

let DEV, LIM, limitedId;
const created = [];
let fail = 0;

async function call(tok, method, path, body) {
  const r = await fetch(BASE + path.replace(/^\//, ''), {
    method,
    headers: { Authorization: 'Bearer ' + tok, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  let j = null; try { j = await r.json(); } catch (e) {}
  return { status: r.status, ok: r.ok && !(j && j.success === false), msg: (j && j.message) || '' };
}
const dev = (m, p, b) => call(DEV, m, p, b);
const lim = (m, p, b) => call(LIM, m, p, b);

async function login(u, p) {
  const r = await fetch(BASE + 'auth/login', { method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: u, password: p }) });
  const j = await r.json().catch(() => null);
  return j && j.data && (j.data.token || j.data.access_token);
}

/** Kisitli kullanici bu istegi YAPAMAMALI (403 beklenir). */
function reddedilmeli(ad, r) {
  const ok = r.status === 403;
  console.log((ok ? '  ✅ 403 reddedildi' : `  ❌ AÇIK [${r.status}]`) + '  ' + ad);
  if (!ok) fail++;
}
/** Kisitli kullanici bunu YAPABILMELI (kirilma kontrolu). */
function izinVerilmeli(ad, r) {
  const ok = r.ok;
  console.log((ok ? '  ✅ izin verildi ' : `  ❌ KIRILDI [${r.status}] ${r.msg}`) + '  ' + ad);
  if (!ok) fail++;
}

(async () => {
  DEV = await login('testdev', 'test1234');
  if (!DEV) throw new Error('testdev login basarisiz — sunucu ayakta mi?');

  const sfx = Date.now().toString(36);
  const uname = 'authz.kisitli.' + sfx;
  const lu = await dev('POST', '/users', { username: uname, password: 'authz12345',
    name: 'AUTHZ Kisitli', role: 'user', permissions: ['dashboard'] });
  if (!lu.ok) throw new Error('kisitli kullanici olusmadi: ' + lu.msg);
  limitedId = (await dev('GET', '/users')).ok
    ? null : null; // id'yi asagida cekiyoruz
  const users = await fetch(BASE + 'users', { headers: { Authorization: 'Bearer ' + DEV } })
    .then(r => r.json());
  limitedId = users.data.find(u => u.username === uname).id;

  LIM = await login(uname, 'authz12345');
  if (!LIM) throw new Error('kisitli login basarisiz');
  console.log('Kisitli kullanici hazir (yetki: sadece dashboard)\n');

  // Hedef kayitlar (dev token ile olusturulur, sonunda silinir)
  const mk = async (ep, body) => {
    const r = await fetch(BASE + ep.replace(/^\//, ''), { method: 'POST',
      headers: { Authorization: 'Bearer ' + DEV, 'Content-Type': 'application/json' },
      body: JSON.stringify(body) });
    const j = await r.json();
    const id = j.data.id; created.push([ep, id]); return id;
  };
  const prodId = await mk('/bom-products', { name: 'AUTHZ Urun', code: 'AUTHZP-' + sfx });
  const opId   = await mk('/bom-operations', { name: 'AUTHZ Islem', code: 'AUTHZO-' + sfx });
  const supId  = await mk('/suppliers', { name: 'AUTHZ Tedarikci ' + sfx });
  const matId  = await mk('/materials', { name: 'AUTHZ Malzeme ' + sfx });
  const whId   = await mk('/warehouses', { name: 'AUTHZ Depo ' + sfx });
  const deptId = await mk('/departments', { name: 'AUTHZ Bolum ' + sfx });

  console.log('═══ Kisitli kullanici YIKICI uclara erisememeli ═══');
  reddedilmeli('DELETE /bom-products (urun agaci koku)', await lim('DELETE', '/bom-products/' + prodId));
  reddedilmeli('DELETE /bom-operations (islem tanimi)',  await lim('DELETE', '/bom-operations/' + opId));
  reddedilmeli('DELETE /suppliers (tedarikci karti)',    await lim('DELETE', '/suppliers/' + supId));
  reddedilmeli('DELETE /materials (malzeme karti)',      await lim('DELETE', '/materials/' + matId));
  reddedilmeli('DELETE /warehouses (depo)',              await lim('DELETE', '/warehouses/' + whId));
  reddedilmeli('DELETE /departments (bolum)',            await lim('DELETE', '/departments/' + deptId));
  reddedilmeli('POST /bom-products (yeni urun)',
      await lim('POST', '/bom-products', { name: 'HACK', code: 'HACK-' + sfx }));
  reddedilmeli('POST /warehouses (yeni depo)',
      await lim('POST', '/warehouses', { name: 'HACK Depo ' + sfx }));
  reddedilmeli('PUT /suppliers (tedarikci adini degistir)',
      await lim('PUT', '/suppliers/' + supId, { name: 'HACKED' }));
  reddedilmeli('DELETE /bom-documents (teknik resim ucu)',
      await lim('DELETE', '/bom-documents/' + prodId)); // id sahte; 403 yetkiden once gelmeli
  reddedilmeli('DELETE /orders (proje) — K3',  await lim('DELETE', '/orders/' + prodId));
  // MIP Asama 2: rezervasyon uclari kilitli (talep=mip/warehouse, onay=warehouse)
  reddedilmeli('POST /warehouse-reservations (rezervasyon talebi)',
      await lim('POST', '/warehouse-reservations',
        { project_name: 'HACK', warehouse_id: whId, item_name: 'HACK', requested_qty: 1 }));
  reddedilmeli('POST /warehouse-reservations/{id}/approve (rezervasyon onayi)',
      await lim('POST', '/warehouse-reservations/' + prodId + '/approve',
        { approved_qty: 1 })); // id sahte; 403 yetkiden once gelmeli
  reddedilmeli('DELETE /warehouse-reservations (rezervasyon kaydi)',
      await lim('DELETE', '/warehouse-reservations/' + prodId));
  // Denetim izi: uretim gecmisi silme yalnizca developer (5. tur). POST acik
  // kalmali — QR okutan isci uretim kaydi yaziyor (asagida kirilma kontrolu).
  reddedilmeli('DELETE /part-logs (uretim gecmisi — denetim izi)',
      await lim('DELETE', '/part-logs/' + prodId)); // id sahte; 403 yetkiden once gelmeli
  // (13. tur madde 4) Sevkiyat paketleme: yazma `shipping` yetkisine kilitli
  reddedilmeli('POST /shipment-packages (sevkiyat paketi)',
      await lim('POST', '/shipment-packages', { project_name: 'HACK' }));
  reddedilmeli('DELETE /shipment-packages',
      await lim('DELETE', '/shipment-packages/' + prodId)); // id sahte; 403 yetkiden once
  reddedilmeli('POST /shipment-package-items (paket satiri)',
      await lim('POST', '/shipment-package-items',
        { package_id: prodId, item_name: 'HACK', quantity: 1 }));

  console.log('\n═══ Kirilma kontrolu: OKUMA acik kalmali ═══');
  izinVerilmeli('GET /bom-products', await lim('GET', '/bom-products'));
  izinVerilmeli('GET /warehouses',   await lim('GET', '/warehouses'));
  izinVerilmeli('GET /shipment-packages', await lim('GET', '/shipment-packages'));

  console.log('\n═══ (16. tur M3.2) PROJE TEKNIK RESIMLERI YAZMA KILIDI ═══');
  reddedilmeli('DELETE /project-documents (proje resmi)',
      await lim('DELETE', '/project-documents/' + prodId)); // id sahte; 403 yetkiden once
  reddedilmeli('PUT /project-documents (meta)',
      await lim('PUT', '/project-documents/' + prodId, { category: 'DIGER' }));

  console.log('\n═══ (15. tur Y2) NAKLIYECI KARTOTEKI + FIRMA AYARLARI YAZMA KILIDI ═══');
  reddedilmeli('POST /carriers (nakliye firmasi)',
      await lim('POST', '/carriers', { name: 'HACK Nakliyat' }));
  reddedilmeli('PUT /company-settings (firma ayarlari)',
      await lim('PUT', '/company-settings', { name: 'HACK AS' }));
  izinVerilmeli('GET /carriers (okuma acik)', await lim('GET', '/carriers'));
  izinVerilmeli('GET /company-settings (okuma acik — PDF basan herkes)',
      await lim('GET', '/company-settings'));

  console.log('\n═══ (15. tur T1) HALKA ACIK PAKET UCU — yalniz o, yalniz GET ═══');
  // Token'siz cagri: Authorization basligi HIC gonderilmez ("Bearer null" degil).
  const anon = async (method, path, body) => {
    const r = await fetch(BASE + path.replace(/^\//, ''), { method,
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined });
    let j = null; try { j = await r.json(); } catch (e) {}
    return { status: r.status, ok: r.ok && !(j && j.success === false), msg: (j && j.message) || '', data: j && j.data };
  };
  // Gercek paket kur (dev) — public ucun 200 donmesi icin
  await dev('POST', '/shipment-packages', { project_name: 'AUTHZ Paket ' + sfx });
  const pkList = await fetch(BASE + 'shipment-packages', { headers: { Authorization: 'Bearer ' + DEV } })
    .then(r => r.json());
  const pkId = (pkList.data || []).find(p => p.project_name === 'AUTHZ Paket ' + sfx)?.id;
  if (pkId) created.push(['/shipment-packages', pkId]);
  {
    const pub = await anon('GET', '/shipment-packages/' + pkId + '/public');
    const pubOk = pub.ok && pub.data && pub.data.package_no;
    console.log((pubOk ? '  ✅ 200 acik' : `  ❌ KAPALI [${pub.status}] ${pub.msg}`) + '  GET /shipment-packages/{id}/public (token yok)');
    if (!pubOk) fail++;
    // Sinirli alan kumesi: ic alanlar sizmamali
    const sizinti = pub.data && ('notes' in pub.data || 'created_by' in pub.data
      || 'warehouse_id' in pub.data || 'delivery_note_id' in pub.data);
    console.log((!sizinti ? '  ✅ ic alanlar yok' : '  ❌ SIZDI — notes/created_by/ic UUID donuyor') + '  public govde sinirli mi');
    if (sizinti) fail++;
    const anonList = await anon('GET', '/shipment-packages');
    const listKapali = anonList.status === 401 || anonList.status === 403;
    console.log((listKapali ? `  ✅ liste kapali [${anonList.status}]` : `  ❌ ACIK [${anonList.status}]`) + '  GET /shipment-packages (token yok)');
    if (!listKapali) fail++;
    const anonWrite = await anon('PUT', '/shipment-packages/' + pkId, { name: 'HACK' });
    const yazKapali = anonWrite.status === 401 || anonWrite.status === 403;
    console.log((yazKapali ? `  ✅ yazma kapali [${anonWrite.status}]` : `  ❌ ACIK [${anonWrite.status}]`) + '  PUT /shipment-packages (token yok)');
    if (!yazKapali) fail++;
  }

  console.log('\n═══ (12. tur m1) TEKLIF GIZLILIGI ═══');
  // Teklif kaydi: orders_quotes'suz kullaniciya LISTEDE de TEK KAYITTA da donmemeli
  const qOrder = await dev('POST', '/orders',
    { project_name: 'AUTHZ Teklif ' + sfx, customer_name: 'AUTHZ Musteri', status: 'quote' });
  const qList = await fetch(BASE + 'orders', { headers: { Authorization: 'Bearer ' + DEV } })
    .then(r => r.json());
  const qId = qList.data.find(o => o.project_name === 'AUTHZ Teklif ' + sfx)?.id;
  if (qId) created.push(['/orders', qId]);
  {
    const limList = await fetch(BASE + 'orders', { headers: { Authorization: 'Bearer ' + LIM } })
      .then(r => r.json()).catch(() => null);
    const gorunuyor = !!(limList && limList.data || []).length
      && limList.data.some(o => o.project_name === 'AUTHZ Teklif ' + sfx);
    console.log((gorunuyor ? '  ❌ SIZDI — quote yetkisiz listede gorunuyor' : '  ✅ liste filtreli') + '  GET /orders (orders_quotes yok)');
    if (gorunuyor) fail++;
    const tek = await lim('GET', '/orders/' + qId);
    const tekOk = tek.status === 404 || tek.status === 403 || (tek.ok === false);
    console.log((tekOk ? '  ✅ tek kayit da gizli [' + tek.status + ']' : '  ❌ SIZDI [' + tek.status + ']') + '  GET /orders/{quoteId}');
    if (!tekOk) fail++;
    // developer quote'u GORMELI (filtre yalnizca yetkisize)
    const devList = qList.data.some(o => o.id === qId);
    console.log((devList ? '  ✅ developer teklifi goruyor' : '  ❌ developer da goremiyor!') + '  GET /orders (dev)');
    if (!devList) fail++;
    // Teklif dosyalari: OKUMA DAHIL kisitli (fiyat icerir)
    reddedilmeli('GET /order-documents (teklif dosyalari — okuma da kisitli)',
      await lim('GET', '/order-documents?order_id=' + qId));
    reddedilmeli('DELETE /order-documents',
      await lim('DELETE', '/order-documents/' + qId)); // id sahte; 403 yetkiden once
  }

  console.log('\n═══ Kirilma kontrolu: uretim akisi YAZMA kilitli olmamali ═══');
  // Gecersiz govde ile 400/404 donebilir; onemli olan 403 (yetki) OLMAMASI.
  const yetkiEngeliOlmamali = (ad, r) => {
    const ok = r.status !== 403;
    console.log((ok ? `  ✅ yetki engeli yok [${r.status}]` : '  ❌ 403 — QR/saha akisi KIRILDI') + '  ' + ad);
    if (!ok) fail++;
  };
  yetkiEngeliOlmamali('POST /part-logs (QR uretim kaydi)',
      await lim('POST', '/part-logs', { part_id: prodId, quantity: 1 }));
  yetkiEngeliOlmamali('POST /warehouse-movements (mal kabul)',
      await lim('POST', '/warehouse-movements', { warehouse_id: whId, movement_type: 'IN', quantity: 1 }));

  console.log('\n═══ Kirilma kontrolu: developer hala yazabilmeli ═══');
  izinVerilmeli('dev POST /materials', await dev('POST', '/materials', { name: 'AUTHZ Dev Malzeme ' + sfx }));
  const devMat = (await fetch(BASE + 'materials', { headers: { Authorization: 'Bearer ' + DEV } })
    .then(r => r.json())).data.find(m => m.name === 'AUTHZ Dev Malzeme ' + sfx);
  if (devMat) created.push(['/materials', devMat.id]);

  // Temizlik
  console.log('\nTemizlik...');
  for (const [ep, id] of created.reverse()) await dev('DELETE', ep + '/' + id);
  await dev('DELETE', '/users/' + limitedId);

  console.log('\n' + '─'.repeat(60));
  if (fail) { console.log(`❌ ${fail} kontrol BASARISIZ.`); process.exit(1); }
  console.log('✅ YETKI KILIDI SAGLAM — tum yikici uclar reddedildi, okuma ve developer yazma calisiyor.');
})().catch(e => { console.error('HATA:', e.message); process.exit(1); });
