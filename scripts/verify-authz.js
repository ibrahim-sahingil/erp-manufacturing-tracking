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

  console.log('\n═══ Kirilma kontrolu: OKUMA acik kalmali ═══');
  izinVerilmeli('GET /bom-products', await lim('GET', '/bom-products'));
  izinVerilmeli('GET /warehouses',   await lim('GET', '/warehouses'));

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
