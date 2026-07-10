// MİP (Malzeme İhtiyaç Planlama) hesap çekirdeğinin sanity testi — SUNUCU GEREKMEZ.
//
// index.html'den saf fonksiyonlari grab() ile cikarip Node'da kosar:
//   mipKey / whStockOf / mipGroupParts / mipCalcRow / mipSuggest
//
// Kapsanan kurallar (7. tur #4, Asama 1 + Asama 2 rezervasyon):
//   - Sadece satin alinan turler (TEDARIK/HAMMADDE/SARF) listelenir
//   - custom_* bir OVERRIDE'dir; bossa resolved_* kullanilir
//   - Ayni kod agacin farkli dallarinda tekrar edebilir -> adetler TOPLANIR
//   - Eslesme: kod oncelikli, kod yoksa ad
//   - missing = max(0, need - stok - malKabul - rezerve)
//   - Durum onceligi: DONE > RESERVED > FROM_STOCK > WAITING > SUPPLY > MISSING
//   - APPROVED/PARTIAL rezervasyonun approved_qty'si 'reserved' sayilir
//     (OUT stoktan zaten dusurdu — cifte sayim yok); REQUESTED yalniz
//     pendingReserve olarak raporlanir, missing'i DEGISTIRMEZ
//   - mipReservePlan: karsilanmamis ihtiyaci depolara SIRAYLA dagitir
//
// Kullanim: node scripts/verify-mip.js
const fs = require('fs');
const path = require('path');
const html = fs.readFileSync(path.join('src', 'main', 'resources', 'static', 'index.html'), 'utf8');

function grab(name){
  const marker = 'function ' + name;
  const start = html.indexOf(marker);
  if(start < 0) throw new Error(name + ' bulunamadı');
  let i = html.indexOf('(', start), pd = 1; i++;
  while(pd > 0){ const c = html[i]; if(c === '(') pd++; if(c === ')') pd--; i++; }
  i = html.indexOf('{', i);
  let d = 1; i++;
  while(d > 0){ const c = html[i]; if(c === '{') d++; if(c === '}') d--; i++; }
  const asyncPrefix = html.slice(Math.max(0, start - 6), start) === 'async ' ? 'async ' : '';
  return asyncPrefix + html.slice(start, i);
}

let fail = 0;
const chk = (n, c, ek = '') => {
  console.log((c ? '  ✅' : '  ❌ FAIL') + ' ' + n + (ek ? '  — ' + ek : ''));
  if(!c) fail++;
};

// index.html'deki sabit (kopya degil, dosyadan okunur)
const m = html.match(/const PURCHASE_KINDS = (\[[^\]]*\]);/);
if(!m) throw new Error('PURCHASE_KINDS bulunamadı');
global.PURCHASE_KINDS = eval(m[1]);

eval(grab('mipKey'));
eval(grab('whStockOf'));
eval(grab('mipCodeOf'));
eval(grab('mipNameOf'));
eval(grab('mipQtyOf'));
eval(grab('mipKindOf'));
eval(grab('mipGroupParts'));
eval(grab('mipCalcRow'));
eval(grab('mipNum'));
eval(grab('mipSuggest'));
eval(grab('mipReservePlan'));
eval(grab('mipBuyQty'));

const WH = [{id: 'A', name: 'A-Depo'}, {id: 'B', name: 'B-Depo'}];
const mv = (whId, name, code, type, qty) =>
  ({warehouse_id: whId, item_name: name, item_code: code, movement_type: type, quantity: qty});
const pi = (proje, name, code, status, qty) =>
  ({project_name: proje, name, code, status, quantity: qty});

console.log('═══ mipKey: kod oncelikli, yoksa ad ═══');
chk('kod varsa kod kullanilir', mipKey('M12 Somun', 'SOM-12') === 'som-12');
chk('kod yoksa ad kullanilir', mipKey('M12 Somun', null) === 'm12 somun');
chk('bosluk/buyuk-kucuk harf normalize', mipKey('  M12 SOMUN ', '') === 'm12 somun');

console.log('\n═══ whStockOf: IN - OUT, sadece o depo ═══');
const hareketler = [
  mv('A', 'M12 Somun', 'SOM-12', 'IN', 40),
  mv('A', 'M12 Somun', 'SOM-12', 'OUT', 10),
  mv('B', 'M12 Somun', 'SOM-12', 'IN', 10),
  mv('A', 'Baska', 'BSK-1', 'IN', 999)
];
chk('A deposu net 30', whStockOf(hareketler, 'A', 'M12 Somun', 'SOM-12') === 30);
chk('B deposu net 10', whStockOf(hareketler, 'B', 'M12 Somun', 'SOM-12') === 10);
chk('baska malzeme sizmaz', whStockOf(hareketler, 'A', 'Yok', 'YOK-1') === 0);
chk('kodsuz esleme ada duser',
    whStockOf([mv('A', 'Conta', null, 'IN', 5)], 'A', 'Conta', null) === 5);

console.log('\n═══ mipGroupParts: tur filtresi + override + adet toplama ═══');
const parts = [
  // ayni kod iki dalda -> 30 + 20 = 50
  {custom_code: 'SOM-12', custom_name: 'M12 Somun', custom_qty: 30, material_kind: 'TEDARIK'},
  {custom_code: 'SOM-12', custom_name: 'M12 Somun', custom_qty: 20, material_kind: 'TEDARIK'},
  // uretim parcasi -> listelenmez
  {custom_code: 'GVD-1', custom_name: 'Govde', custom_qty: 1, material_kind: 'MAMUL'},
  // override YOK -> resolved_* kullanilir
  {custom_code: null, custom_name: null, custom_qty: null,
   resolved_code: 'SAC-3', resolved_name: 'Sac 3mm', resolved_qty: 4, resolved_material_kind: 'HAMMADDE'},
  // haric tutulan
  {custom_code: 'X-1', custom_name: 'Haric', custom_qty: 9, material_kind: 'SARF', is_excluded: true},
  // turu yok -> listelenmez (eski davranis: uretim)
  {custom_code: 'TRS-1', custom_name: 'Tursuz', custom_qty: 3, material_kind: null}
];
const gruplar = mipGroupParts(parts);
chk('sadece satin alinan turler kaldi (2 grup)', gruplar.length === 2,
    gruplar.map(g => g.code).join(','));
const somun = gruplar.find(g => g.code === 'SOM-12');
chk('ayni kodun adetleri toplandi (30+20=50)', somun && somun.need === 50, somun && somun.need);
const sac = gruplar.find(g => g.code === 'SAC-3');
chk('override yokken resolved_* okundu', sac && sac.name === 'Sac 3mm' && sac.need === 4);
chk('is_excluded parca elendi', !gruplar.some(g => g.code === 'X-1'));
chk('turu olmayan parca elendi', !gruplar.some(g => g.code === 'TRS-1'));

console.log('\n═══ mipCalcRow: arkadasin senaryosu (50 ihtiyac, A=30, B=10) ═══');
const g50 = {key: 'som-12', code: 'SOM-12', name: 'M12 Somun', unit: 'adet', need: 50};
const r1 = mipCalcRow(g50, WH, hareketler, [], 'PROJE-1');
chk('stok toplami 40', r1.stockTotal === 40, r1.stockTotal);
chk('depo dagilimi 2 kayit', r1.stockByWh.length === 2);
chk('eksik 10', r1.missing === 10, r1.missing);
chk('durum MISSING (siparis verilmedi)', r1.status === 'MISSING', r1.status);
chk('oneri metni dogru', mipSuggest(r1) === "30 A-Depo'dan · 10 B-Depo'dan · 10 satın alınacak",
    mipSuggest(r1));

console.log('\n═══ mipCalcRow: durum onceligi ═══');
// DONE: mal kabul ihtiyaci karsiliyor
const rDone = mipCalcRow(g50, WH, [], [pi('PROJE-1', 'M12 Somun', 'SOM-12', 'IN_WAREHOUSE', 50)], 'PROJE-1');
chk('mal kabul >= ihtiyac -> DONE', rDone.status === 'DONE', rDone.status);
chk('DONE iken eksik 0', rDone.missing === 0);

// FROM_STOCK: stok ihtiyaci karsiliyor, siparise gerek yok
const bolStok = [mv('A', 'M12 Somun', 'SOM-12', 'IN', 60)];
const rStock = mipCalcRow(g50, WH, bolStok, [], 'PROJE-1');
chk('stok yeterli -> FROM_STOCK', rStock.status === 'FROM_STOCK', rStock.status);

// WAITING: eksik var ama siparis verilmis
const rWait = mipCalcRow(g50, WH, hareketler, [pi('PROJE-1', 'M12 Somun', 'SOM-12', 'ORDERED', 10)], 'PROJE-1');
chk('eksik + siparis verilmis -> WAITING', rWait.status === 'WAITING', rWait.status);
chk('siparis miktari okundu', rWait.ordered === 10);

// SUPPLY: hicbir depoda yok
const rSupply = mipCalcRow(g50, WH, [], [], 'PROJE-1');
chk('stokta hic yok + siparis yok -> SUPPLY', rSupply.status === 'SUPPLY', rSupply.status);

// MISSING: kalem PLANNED olarak durur ama siparis VERILMEMIS
const rPlanned = mipCalcRow(g50, WH, hareketler, [pi('PROJE-1', 'M12 Somun', 'SOM-12', 'PLANNED', 10)], 'PROJE-1');
chk('PLANNED kalem siparis sayilmaz -> MISSING', rPlanned.status === 'MISSING', rPlanned.status);
chk('planned miktari ayri raporlanir', rPlanned.planned === 10);

console.log('\n═══ mipCalcRow: sizinti kontrolleri ═══');
chk('BASKA projenin kalemi sayilmaz',
    mipCalcRow(g50, WH, [], [pi('BASKA-PROJE', 'M12 Somun', 'SOM-12', 'ORDERED', 50)], 'PROJE-1').ordered === 0);
chk('CANCELLED kalem sayilmaz',
    mipCalcRow(g50, WH, [], [pi('PROJE-1', 'M12 Somun', 'SOM-12', 'CANCELLED', 50)], 'PROJE-1').ordered === 0);
chk('baska malzemenin kalemi sayilmaz',
    mipCalcRow(g50, WH, [], [pi('PROJE-1', 'Baska', 'BSK-9', 'ORDERED', 50)], 'PROJE-1').ordered === 0);
chk('negatif stokta eksik sismez (max 0)',
    mipCalcRow({...g50, need: 5}, WH, bolStok, [], 'PROJE-1').missing === 0);

console.log('\n═══ mipCalcRow: rezervasyonlar (Asama 2) ═══');
const wres = (proje, name, code, status, reqQty, appQty) =>
  ({project_name: proje, item_name: name, item_code: code, status,
    requested_qty: reqQty, approved_qty: appQty});

// Arkadasin senaryosu: 50 ihtiyac, A'da 40 vardi; 30'u onaylandi -> OUT yazildi,
// kayitli stok 10'a dustu. reserved=30 karsilanmis sayilir, eksik 10 kalir.
const mvRezSonrasi = [
  mv('A', 'M12 Somun', 'SOM-12', 'IN', 40),
  mv('A', 'M12 Somun', 'SOM-12', 'OUT', 30) // rezervasyon OUT'u (backend yazdi)
];
const rezOnayli = [wres('PROJE-1', 'M12 Somun', 'SOM-12', 'APPROVED', 30, 30)];
const rRez = mipCalcRow(g50, WH, mvRezSonrasi, [], 'PROJE-1', rezOnayli);
chk('reserved 30 okundu', rRez.reserved === 30, rRez.reserved);
chk('cifte sayim yok: eksik 10 (50 - 10 stok - 30 rezerve)', rRez.missing === 10, rRez.missing);
chk('oneri rezerveyi soyler', mipSuggest(rRez).includes('30 depoya rezerve'), mipSuggest(rRez));

// RESERVED durumu FROM_STOCK'tan ONCE: rezerve + stok ihtiyaci kapatiyor
const rRez2 = mipCalcRow({...g50, need: 40}, WH, mvRezSonrasi, [], 'PROJE-1', rezOnayli);
chk('rezerve+stok yeterli -> RESERVED (FROM_STOCK degil)', rRez2.status === 'RESERVED', rRez2.status);
chk('RESERVED iken eksik 0', rRez2.missing === 0);

// PARTIAL: yalniz ONAYLANAN miktar sayilir (30 istendi, 15 cikti)
const rKismi = mipCalcRow(g50, WH, mvRezSonrasi, [],
  'PROJE-1', [wres('PROJE-1', 'M12 Somun', 'SOM-12', 'PARTIAL', 30, 15)]);
chk('PARTIAL onaylanan kadar sayilir (15)', rKismi.reserved === 15, rKismi.reserved);

// REQUESTED: yalniz bilgi — missing DEGISMEZ, pendingReserve raporlanir
const rBekleyen = mipCalcRow(g50, WH, hareketler, [],
  'PROJE-1', [wres('PROJE-1', 'M12 Somun', 'SOM-12', 'REQUESTED', 40, null)]);
chk('REQUESTED missing degistirmez', rBekleyen.missing === 10, rBekleyen.missing);
chk('pendingReserve raporlanir (40)', rBekleyen.pendingReserve === 40, rBekleyen.pendingReserve);
chk('REQUESTED reserved sayilmaz', rBekleyen.reserved === 0);

// Sizinti: baska proje / CANCELLED / REJECTED sayilmaz
chk('BASKA projenin rezervasyonu sayilmaz',
    mipCalcRow(g50, WH, [], [], 'PROJE-1',
      [wres('BASKA-PROJE', 'M12 Somun', 'SOM-12', 'APPROVED', 30, 30)]).reserved === 0);
chk('CANCELLED rezervasyon sayilmaz',
    mipCalcRow(g50, WH, [], [], 'PROJE-1',
      [wres('PROJE-1', 'M12 Somun', 'SOM-12', 'CANCELLED', 30, null)]).pendingReserve === 0);
chk('REJECTED rezervasyon sayilmaz',
    mipCalcRow(g50, WH, [], [], 'PROJE-1',
      [wres('PROJE-1', 'M12 Somun', 'SOM-12', 'REJECTED', 30, 0)]).reserved === 0);
chk('baska malzemenin rezervasyonu sayilmaz',
    mipCalcRow(g50, WH, [], [], 'PROJE-1',
      [wres('PROJE-1', 'Baska', 'BSK-9', 'APPROVED', 30, 30)]).reserved === 0);

console.log('\n═══ mipReservePlan: dagitim onerisi ═══');
const pRow = (need, received, reserved, stockByWh) => ({need, received, reserved, stockByWh});
const dag1 = mipReservePlan(pRow(50, 0, 0,
  [{whId: 'A', name: 'A-Depo', qty: 30}, {whId: 'B', name: 'B-Depo', qty: 10}]));
chk('stok yetmiyorsa depolar sirayla bosaltilir (30+10)',
    dag1.length === 2 && dag1[0].qty === 30 && dag1[1].qty === 10,
    JSON.stringify(dag1.map(p => p.qty)));
const dag2 = mipReservePlan(pRow(35, 0, 0,
  [{whId: 'A', name: 'A-Depo', qty: 30}, {whId: 'B', name: 'B-Depo', qty: 10}]));
chk('kalan ihtiyac kadar istenir (30+5)',
    dag2.length === 2 && dag2[0].qty === 30 && dag2[1].qty === 5,
    JSON.stringify(dag2.map(p => p.qty)));
const dag3 = mipReservePlan(pRow(50, 10, 30, [{whId: 'A', name: 'A-Depo', qty: 25}]));
chk('mal kabul + onceki rezerve dusulur (50-10-30=10)',
    dag3.length === 1 && dag3[0].qty === 10, JSON.stringify(dag3));
chk('ihtiyac karsilanmissa plan bos',
    mipReservePlan(pRow(30, 0, 30, [{whId: 'A', name: 'A-Depo', qty: 25}])).length === 0);

console.log('\n═══ mipBuyQty: satin almaya gonderme onerisi (8. tur) ═══');
// Yayinlama artik satin almaya kalem dusurmez; eksik MIP'ten gonderilir.
// Oneri = eksik - PLANNED (gonderilmis ama siparis edilmemis) — mukerrer olmasin.
chk('planlanan yokken oneri = eksik', mipBuyQty({missing: 10, planned: 0}) === 10);
chk('PLANNED oneriden dusulur (10-8=2)', mipBuyQty({missing: 10, planned: 8}) === 2);
chk('planlanan eksigi asarsa 0 (negatif olmaz)', mipBuyQty({missing: 5, planned: 8}) === 0);
chk('eksik yoksa 0', mipBuyQty({missing: 0, planned: 0}) === 0);
// mipGroupParts artik malzeme + ilk pbp bagini tasir (gonderilen kalem
// MRP olculeri icin pbp'ye baglanir)
const gMalz = mipGroupParts([{id: 'pbp1', custom_code: 'M-1', custom_name: 'Malz',
  custom_qty: 1, material_kind: 'TEDARIK', custom_material: 'St-37'}]);
chk('grup malzeme + pbp bagi tasir', gMalz[0].material === 'St-37' && gMalz[0].pbpId === 'pbp1',
    JSON.stringify({m: gMalz[0].material, p: gMalz[0].pbpId}));

console.log('\n' + '─'.repeat(60));
if(fail){ console.log(`❌ ${fail} kontrol BASARISIZ.`); process.exit(1); }
console.log('✅ MİP HESAP ÇEKİRDEĞİ SAĞLAM.');
