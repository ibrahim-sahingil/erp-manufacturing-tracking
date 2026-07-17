// (7. tur #3) Islem tanimi: kod degisince agac kodlarinin otomatik guncellenmesi.
//
// #3: Islem kodu -PW -> -XZ degisince, bu islemi tasiyan TUM parcalarin
//     (bom_parts sablonlari + project_bom_parts yayinlanmis agaclar) hem
//     operations dizisi hem KODU guncellenmeli. Kod yeniden insa edilir:
//     ortadaki islem degisse bile kalinti kalmaz.
// (13. tur madde 1) BOLUM CASCADE'I TERSINE CEVRILDI: islem tanimindaki bolum
//     degisikligi proje parcalarina DOKUNMAZ ve projede bolum OLUSTURMAZ.
// (14. tur S7) department_name alani ONERI olarak geri geldi: update alani
//     YAZAR (cascade hala YOK); oneri yalniz frontend'de, islem eklenirken
//     parca bolumu BOSSA ve ad projede MEVCUT bolumle eslesiyorsa atanir.
//
// Kullanim: node scripts/verify-opdef-cascade.js   (sunucu 8080'de calismali)
const BASE = 'http://localhost:8080/api/';
let TOK;
let fail = 0;
const chk = (ad, ok, ek='') => {
  console.log((ok ? '  ✅ ' : '  ❌ FAIL ') + ad + (ek ? '  — ' + ek : ''));
  if (!ok) fail++;
};

async function call(m, p, b) {
  const r = await fetch(BASE + p.replace(/^\//, ''), { method: m,
    headers: { Authorization: 'Bearer ' + TOK, 'Content-Type': 'application/json' },
    body: b ? JSON.stringify(b) : undefined });
  let j = null; try { j = await r.json(); } catch (e) {}
  return { status: r.status, ok: r.ok && !(j && j.success === false), body: j, msg: (j && j.message) || '' };
}

(async () => {
  const lr = await fetch(BASE + 'auth/login', { method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'testdev', password: 'test1234' }) }).then(r => r.json());
  TOK = lr.data.token;
  const sfx = Date.now().toString(36);
  const temizlik = [];

  try {
    // ── #1: bolum alani ──
    const op = await call('POST', '/bom-operations',
      { name: 'CASC Kaynak', code: 'CW' + sfx.slice(-3).toUpperCase(), department_name: 'Kaynak' });
    chk('islem tanimi bolum alaniyla olustu', op.ok, op.msg);
    const opId = op.body.data.id;
    const opCode = op.body.data.code;
    temizlik.push(['/bom-operations', opId]);
    chk('department_name API cevabinda donuyor', op.body.data.department_name === 'Kaynak',
        JSON.stringify(op.body.data.department_name));

    // ── Sablon agaci: iki islemli parca (ORTADAKI degisecek) ──
    const prod = await call('POST', '/bom-products', { name: 'CASC Urun', code: 'CASC-' + sfx });
    const prodId = prod.body.data.id; temizlik.push(['/bom-products', prodId]);
    const taban = 'CASCP-' + sfx;
    const part = await call('POST', '/bom-parts', {
      product_id: prodId, name: 'CASC Parca', code: taban + opCode + 'PNT',
      quantity: 1, unit: 'adet', level: 0,
      operations: [
        { name: 'CASC Kaynak', code: opCode, desc: '', duration_per_unit: 0, total_duration: 0 },
        { name: 'Boya',        code: 'PNT',  desc: '', duration_per_unit: 0, total_duration: 0 }
      ]
    });
    chk('sablon parcasi olustu', part.ok, part.msg);
    const partId = part.body.data.id;

    // ── Yayinlanmis proje agaci ──
    const projeAdi = 'CASC-PROJE-' + sfx;
    const order = await call('POST', '/orders', { project_name: projeAdi, customer_name: 'CASC' });
    const orderId = order.body.data.id;
    const pbom = await call('POST', '/project-bom', { project_name: projeAdi, bom_product_id: prodId });
    const pbomId = pbom.body.data.id;
    // NOT: Yayinlama (POST /project-bom) sablon parcalarini ZATEN kopyalar —
    // ayrica elle kopya eklemek mukerrer kayit yaratir.
    const pbps = (await call('GET', '/project-bom-parts')).body.data
      .filter(x => x.project_bom_id === pbomId);
    chk('yayinlama sablon parcasini otomatik kopyaladi', pbps.length === 1, 'adet=' + pbps.length);
    const pbpId = pbps[0] && pbps[0].id;

    // ── Onizleme ucu (frontend snake_case bekliyor) ──
    const usage = await call('GET', '/bom-operations/' + opId + '/usage');
    chk('usage ucu calisiyor', usage.ok, usage.msg);
    chk('usage snake_case donuyor (bom_parts / project_bom_parts)',
        usage.body.data && 'bom_parts' in usage.body.data && 'project_bom_parts' in usage.body.data,
        JSON.stringify(usage.body.data));
    chk('usage sayilari dogru (1 sablon + 1 proje)',
        usage.body.data.bom_parts === 1 && usage.body.data.project_bom_parts === 1,
        JSON.stringify(usage.body.data));

    // ── #3: KOD DEGISIMI → cascade ──
    const yeniKod = 'XZ' + sfx.slice(-3).toUpperCase();
    const upd = await call('PUT', '/bom-operations/' + opId,
      { name: 'CASC Kaynak', code: yeniKod, department_name: 'Kaynakhane' });
    chk('islem kodu guncellendi', upd.ok, upd.msg);
    chk('(14. tur S7) update department_name alanini YAZAR (oneri etiketi)',
        upd.body.data.department_name === 'Kaynakhane',
        JSON.stringify(upd.body.data.department_name));

    const partSonra = await call('GET', '/bom-parts/' + partId);
    const kod = partSonra.body.data.code;
    const ops = partSonra.body.data.operations || [];
    console.log('    sablon parca kodu: ' + kod);
    chk('sablon parca kodu yeni kodu icermeli', kod.includes(yeniKod), kod);
    chk('sablon parca kodunda ESKI kod kalmamali', !kod.includes(opCode), kod);
    chk('sablon parca kodu tam beklenen deger', kod === taban + yeniKod + 'PNT', kod);
    chk('operations dizisindeki kod da guncellendi',
        ops.some(o => o.code === yeniKod) && !ops.some(o => o.code === opCode),
        JSON.stringify(ops.map(o => o.code)));

    // (a) custom_code BOS olan kopya: override yok, etkin kod sablondan turer.
    //     Ustune yazilirsa taban kod yok olur ("XZPNT" gibi). Dokunulmamali.
    const pbpSonra = await call('GET', '/project-bom-parts/' + pbpId);
    const pkod = pbpSonra.body.data.custom_code;
    const pops = pbpSonra.body.data.operations || [];
    console.log('    proje parca custom_code (override yok): ' + JSON.stringify(pkod));
    chk('override YOKKEN custom_code bozulmadi (taban kod yok olmadi)',
        !pkod || pkod === '' || pkod.startsWith(taban), JSON.stringify(pkod));
    chk('override YOKKEN operations etiketi guncellendi',
        pops.some(o => o.code === yeniKod), JSON.stringify(pops.map(o => o.code)));

    // (b) custom_code DOLU olan parca: kod yeniden insa edilmeli
    const ozel = await call('POST', '/project-bom-parts', {
      project_bom_id: pbomId, bom_part_id: null, is_excluded: false,
      custom_name: 'CASC Ozel', custom_code: taban + 'X' + yeniKod + 'PNT',
      custom_qty: 1, custom_unit: 'adet', level: 0, sort_order: 1,
      operations: [
        { name: 'CASC Kaynak', code: yeniKod, desc: '' },
        { name: 'Boya',        code: 'PNT',   desc: '' }
      ]
    });
    const ozelId = ozel.body.data.id;

    // ── (8. tur taramasi) Etkin kod degisince TUREVLER de duzeltilmeli:
    //    ayni koddaki satin alma kalemi + BEKLEYEN rezervasyonun kod
    //    snapshot'i. Yoksa MIP eslesmesi kopar, kullanici ikinci kez
    //    gonderir (cifte siparis) — arkadasin buldugu sinifin kardesi.
    const etkinEski = taban + yeniKod + 'PNT';
    const pi = await call('POST', '/purchase-items', { project_name: projeAdi,
      name: 'CASC Parca', code: etkinEski, quantity: 3, unit: 'adet' });
    const piId = pi.body.data.id;
    const whr = await call('POST', '/warehouses', { name: 'CASC Depo ' + sfx });
    const whrId = whr.body.data.id;
    const rez = await call('POST', '/warehouse-reservations', { project_name: projeAdi,
      warehouse_id: whrId, item_name: 'CASC Parca', item_code: etkinEski, requested_qty: 2 });
    const rezId = rez.body.data.id;

    const geriKod = 'YY' + sfx.slice(-3).toUpperCase();
    await call('PUT', '/bom-operations/' + opId, { name: 'CASC Kaynak', code: geriKod });
    const ozelSonra = await call('GET', '/project-bom-parts/' + ozelId);
    console.log('    override kod: ' + ozelSonra.body.data.custom_code);
    chk('override VARKEN kod yeniden insa edildi',
        ozelSonra.body.data.custom_code === taban + 'X' + geriKod + 'PNT',
        ozelSonra.body.data.custom_code);

    const etkinYeni = taban + geriKod + 'PNT';
    const piSonra = (await call('GET', '/purchase-items/' + piId)).body.data;
    chk('satin alma kaleminin kodu cascade ile duzeltildi (MIP eslesmesi kopmaz)',
        piSonra.code === etkinYeni, piSonra.code);
    const rezSonra = (await call('GET', '/warehouse-reservations')).body.data
      .find(x => x.id === rezId);
    chk('bekleyen rezervasyonun kod snapshot\'i duzeltildi',
        rezSonra && rezSonra.item_code === etkinYeni, rezSonra && rezSonra.item_code);

    // ── (13. tur madde 1 — TERS KONTROL) Islem tanimina bolum gonderilse bile
    //    proje parcalarina DOKUNULMAMALI ve projede bolum OLUSTURULMAMALI.
    //    Once parcaya ELLE bolum atanir; opdef guncellemesi bunu EZMEMELI.
    const elleBolum = await call('POST', '/departments',
      { order_id: orderId, name: 'CASC Elle Bolum ' + sfx, sort_order: 1 });
    const elleBolumId = elleBolum.body.data.id;
    await call('PUT', '/project-bom-parts/' + pbpId, { dept_id: elleBolumId });
    const yeniBolum = 'CASC Cascade Bolum ' + sfx;
    const updD = await call('PUT', '/bom-operations/' + opId,
      { name: 'CASC Kaynak', code: geriKod, department_name: yeniBolum });
    chk('bolumlu update istegi kabul edildi', updD.ok, updD.msg);
    const depts = (await call('GET', '/departments')).body.data
      .filter(d => d.order_id === orderId);
    chk('(13. tur) projede bolum OLUSTURULMADI (cascade yok)',
        !depts.some(d => d.name === yeniBolum),
        JSON.stringify(depts.map(d => d.name)));
    const pbpD  = (await call('GET', '/project-bom-parts/' + pbpId)).body.data;
    const ozelD = (await call('GET', '/project-bom-parts/' + ozelId)).body.data;
    chk('(13. tur) elle atanan bolum EZILMEDI', pbpD.dept_id === elleBolumId,
        JSON.stringify({ dept: pbpD.dept_id, beklenen: elleBolumId }));
    chk('(13. tur) bolumsuz parcaya bolum YAZILMADI', !ozelD.dept_id,
        JSON.stringify({ dept: ozelD.dept_id }));
    await call('DELETE', '/departments/' + elleBolumId);

    // temizlik sirasi: rezervasyon/kalem/depo -> proje -> sablon
    // (K1 guard: bagli pi/rezervasyon varken proje silinemez)
    await call('DELETE', '/warehouse-reservations/' + rezId);
    await call('DELETE', '/purchase-items/' + piId);
    await call('DELETE', '/warehouses/' + whrId);
    await call('DELETE', '/project-bom-parts/' + ozelId);
    await call('DELETE', '/project-bom-parts/' + pbpId);
    await call('DELETE', '/project-bom/' + pbomId);
    await call('DELETE', '/orders/' + orderId);
    await call('DELETE', '/bom-parts/' + partId);
  } finally {
    for (const [ep, id] of temizlik.reverse()) await call('DELETE', ep + '/' + id);
  }

  console.log('\n' + '─'.repeat(60));
  if (fail) { console.log(`❌ ${fail} kontrol BASARISIZ.`); process.exit(1); }
  console.log('✅ ISLEM TANIMI: bolum alani + kod cascade SAGLAM.');
})().catch(e => { console.error('HATA:', e.message); process.exit(1); });
