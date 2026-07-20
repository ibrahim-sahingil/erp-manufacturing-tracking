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
eval(grab('mipNameKey'));
eval(grab('mipMatches'));
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
eval(grab('mipGuardMsg')); // (11. tur Y3)
eval(grab('mipPlanInfoOf')); // (12. tur m9)
eval(grab('mrpKonsolideProfil')); // (12. tur m8 bağımlılığı)
eval(grab('mrpOptimumProfil'));   // (12. tur m8)
eval(grab('mrpItemDims'));        // (12. tur m7)
eval(grab('mrpLeftoverOwners'));  // (13. tur madde 3)
eval(grab('mrpRunGuillotine'));   // (15. tur T2 — {placed, free} dönüş şekli)
eval(grab('mrpSheetOffcuts'));    // (15. tur T2)

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

console.log('\n═══ mipMatches: toleransli eslesme (10. tur B2) ═══');
// Arkadasin hatasi: depoya KODSUZ (yalniz adla) girilen malzeme, katalog
// koduyla sorgulaninca "Stok 0 - Hicbir depoda yok" gorunuyordu.
chk('ayni kod eslesir (eski davranis korunur)',
    mipMatches('M14 Somun', 'SOM-14', 'M14 SOMUN', 'SOM-14') === true);
chk('kodsuz hareket + kodlu katalog AYNI adla eslesir',
    mipMatches('M14 Somun', null, 'M14 Somun', 'SOM-14') === true);
chk('iki FARKLI dolu kod asla eslesmez (ayni ad bile olsa)',
    mipMatches('M14 Somun', 'SOM-14', 'M14 Somun', 'SOM-99') === false);
chk('ikisi de kodsuz ayni ad eslesir (normalize)',
    mipMatches('  Conta ', '', 'conta', null) === true);
chk('kodsuz + farkli ad eslesmez',
    mipMatches('A Parcasi', null, 'B Parcasi', 'SOM-14') === false);
chk('whStockOf: kodsuz IN kaydi katalog koduyla sorgulaninca gorunur',
    whStockOf([mv('A', 'M14 Somun', null, 'IN', 20)], 'A', 'M14 Somun', 'SOM-14') === 20);
chk('mipCalcRow: kodsuz depo kaydi stokTotal\'a girer',
    mipCalcRow({key: 'som-14', code: 'SOM-14', name: 'M14 Somun', unit: 'adet', need: 20},
      WH, [mv('A', 'M14 Somun', null, 'IN', 20)], [], 'PROJE-1').status === 'FROM_STOCK');

console.log('\n═══ mipGroupParts: karar/oneri (9. tur M4) + override + adet toplama ═══');
// (M4) TUR FILTRESI KALKTI: gruplama TUM parcalari dondurur; tur yalniz
// ONERIYI (suggested) besler, akis yonlendirmesi procurement_decision ile.
const parts = [
  // ayni kod iki dalda -> 30 + 20 = 50 (her ikisi PURCHASE kararli)
  {id:'p1', custom_code: 'SOM-12', custom_name: 'M12 Somun', custom_qty: 30, material_kind: 'TEDARIK', procurement_decision: 'PURCHASE'},
  {id:'p2', custom_code: 'SOM-12', custom_name: 'M12 Somun', custom_qty: 20, material_kind: 'TEDARIK', procurement_decision: 'PURCHASE'},
  // uretim karari
  {id:'p3', custom_code: 'GVD-1', custom_name: 'Govde', custom_qty: 1, material_kind: 'MAMUL', procurement_decision: 'PRODUCE'},
  // override YOK -> resolved_* kullanilir; karar YOK -> bekliyor
  {id:'p4', custom_code: null, custom_name: null, custom_qty: null,
   resolved_code: 'SAC-3', resolved_name: 'Sac 3mm', resolved_qty: 4, resolved_material_kind: 'HAMMADDE'},
  // haric tutulan
  {id:'p5', custom_code: 'X-1', custom_name: 'Haric', custom_qty: 9, material_kind: 'SARF', is_excluded: true},
  // turu yok -> oneri PRODUCE, karar bekliyor
  {id:'p6', custom_code: 'TRS-1', custom_name: 'Tursuz', custom_qty: 3, material_kind: null},
  // ayni kod dallarinda FARKLI karar -> mixed (yeniden onay ister)
  {id:'p7', custom_code: 'MIX-1', custom_name: 'Karisik', custom_qty: 1, material_kind: 'SARF', procurement_decision: 'PURCHASE'},
  {id:'p8', custom_code: 'MIX-1', custom_name: 'Karisik', custom_qty: 1, material_kind: 'SARF', procurement_decision: 'PRODUCE'}
];
const gruplar = mipGroupParts(parts);
chk('tum parcalar gruplandi (haric tutulan disinda 5 grup)', gruplar.length === 5,
    gruplar.map(g => g.code).join(','));
const somun = gruplar.find(g => g.code === 'SOM-12');
chk('ayni kodun adetleri toplandi (30+20=50) + pbpIds 2 dal',
    somun && somun.need === 50 && somun.pbpIds.length === 2, somun && somun.need);
chk('PURCHASE karari grup uzerinde', somun && somun.decision === 'PURCHASE');
const sac = gruplar.find(g => g.code === 'SAC-3');
chk('override yokken resolved_* okundu', sac && sac.name === 'Sac 3mm' && sac.need === 4);
chk('kararsiz grup: decision null + oneri turden (HAMMADDE→PURCHASE)',
    sac && sac.decision === null && sac.suggested === 'PURCHASE');
const trs = gruplar.find(g => g.code === 'TRS-1');
chk('tursuz parca: oneri PRODUCE, karar bekliyor',
    trs && trs.decision === null && trs.suggested === 'PRODUCE');
const gvd = gruplar.find(g => g.code === 'GVD-1');
chk('PRODUCE kararli grup listede (uretilecekler bolumu)', gvd && gvd.decision === 'PRODUCE');
const mix = gruplar.find(g => g.code === 'MIX-1');
chk('dallarda farkli karar -> mixed + decision null', mix && mix.mixed === true && mix.decision === null);
chk('is_excluded parca elendi', !gruplar.some(g => g.code === 'X-1'));

console.log('\n═══ (12. tur m2) Urun adedi carpani: _pqty ═══');
{
  // renderMipList bagin product_qty'sini satirlara _pqty olarak enjekte eder;
  // mipQtyOf ihtiyaci custom_qty × _pqty hesaplar. _pqty'siz satir = 1 (eski).
  const gC = mipGroupParts([
    {id:'c1', custom_code:'KLN-1', custom_name:'Kolon', custom_qty:4, material_kind:'HAMMADDE', procurement_decision:'PURCHASE', _pqty:3},
    {id:'c2', custom_code:'KLN-1', custom_name:'Kolon', custom_qty:2, material_kind:'HAMMADDE', procurement_decision:'PURCHASE'} // baska bag, carpansiz
  ]);
  chk('carpanli ihtiyac: 4×3 + 2×1 = 14', gC.length===1 && gC[0].need===14,
      gC[0] && gC[0].need);
  chk('carpansiz eski satir davranisi degismedi (1 kabul)',
      mipQtyOf({custom_qty:5}) === 5 && mipQtyOf({custom_qty:5,_pqty:2}) === 10);
}

console.log('\n═══ (12. tur m9) mipPlanInfoOf: POOL satirina plan durumu ═══');
{
  const items = [
    {id:'s1', name:'Kapak Saci', code:'KS-1', status:'PLANNED', project_bom_part_id:'pbp1', stock_plan_id:'pl1'},
    {id:'pl1', name:'Plaka 1350x5000x3', code:null, status:'ORDERED'},
    {id:'x1', name:'Alakasiz', code:'AL-1', status:'PLANNED'}
  ];
  const info = mipPlanInfoOf({pbpIds:['pbp1'], name:'Kapak Saci', code:'KS-1'}, items);
  chk('pbp bagiyla plan bulundu + durumu ORDERED',
      info && info.planId==='pl1' && info.status==='ORDERED', JSON.stringify(info));
  const info2 = mipPlanInfoOf({pbpIds:[], name:'Kapak Saci', code:'KS-1'}, items);
  chk('pbp bagi yoksa ad/kod eslesmesiyle bulunur', info2 && info2.planId==='pl1');
  chk('plansiz grup null doner', mipPlanInfoOf({pbpIds:['yok'], name:'Alakasiz', code:'AL-1'}, items)===null);
}

console.log('\n═══ (12. tur m8) mrpOptimumProfil: en az fire kazanir ═══');
{
  // 3×3500mm: 6000'e profil basina 1 parca sigar (3 profil, fire 7500);
  // 12000'e ucu birden (1 profil, fire 1500) → 12000 kazanmali
  const lengths = [3500, 3500, 3500];
  const best = mrpOptimumProfil(lengths, [6000, 12000], 0);
  chk('12000 secildi (fire 1500 < 7500)', best && best.L===12000 && best.kons.toplamProfil===1,
      best && JSON.stringify({L:best.L, adet:best.kons.toplamProfil, fire:best.fire}));
  chk('kiyas iki adayi da raporlar', best.kiyas.length===2
      && best.kiyas.find(k=>k.L===6000).adet===3 && best.kiyas.find(k=>k.L===12000).adet===1);
  // Fire esitse KISA boy kazanir (stok/istif kolayligi): 4×2900 iki aday da 400 fire
  const tie = mrpOptimumProfil([2900,2900,2900,2900], [6000,12000], 0);
  chk('esit firede kisa boy kazanir (6000)', tie && tie.L===6000 && tie.fire===400,
      tie && JSON.stringify({L:tie.L, fire:tie.fire}));
  // 7000mm parça 6000'e sığmaz → yalnız 12000 aday kalır
  const best2 = mrpOptimumProfil([7000], [6000, 12000], 0);
  chk('sigmayan aday elenir (6000 sigmiyor, 12000 secilir)',
      best2 && best2.L===12000 && best2.kiyas.find(k=>k.L===6000).sigmiyor===true);
  chk('hicbiri sigmazsa null', mrpOptimumProfil([20000], [6000, 12000], 0)===null);
}

console.log('\n═══ (12. tur m7) mrpItemDims: custom oncelik + 5 olcu + form ═══');
{
  global.projectBomParts = [{id:'pbp9', custom_length_mm:2500, resolved_length_mm:9999,
    resolved_width_mm:40, resolved_thickness_mm:2, resolved_diameter_mm:null,
    material_form:null, resolved_material_form:'PROFIL'}];
  const d = mipNum ? mrpItemDims({project_bom_part_id:'pbp9'}) : null;
  chk('custom_length oncelikli (2500, resolved 9999 degil) + form resolved\'dan',
      d && d.l===2500 && d.form==='PROFIL' && d.w===40 && d.t===2, JSON.stringify(d));
  chk('bagsiz kalem: olculer 0 + form bos',
      JSON.stringify(mrpItemDims({}))===JSON.stringify({w:0,h:0,t:0,l:0,d:0,form:''}));
}

console.log('\n═══ POOL karari (10. tur M5) ═══');
const gPool = mipGroupParts([
  {id:'pp1', custom_code:'PL-1', custom_name:'Plaka', custom_qty:3, material_kind:'HAMMADDE', procurement_decision:'POOL'},
  {id:'pp2', custom_code:'PL-1', custom_name:'Plaka', custom_qty:2, material_kind:'HAMMADDE', procurement_decision:'POOL'}
]);
chk('POOL karari grup uzerinde tasinir + adetler toplanir',
    gPool.length === 1 && gPool[0].decision === 'POOL' && gPool[0].need === 5,
    JSON.stringify({d: gPool[0].decision, n: gPool[0].need}));
chk('POOL grubu karar-bekleyen DEGIL (decision dolu)', gPool[0].decision !== null);
// Havuza gonderilen kalem PLANNED yazilir -> mipCalcRow 'planned' sayar ->
// mipBuyQty tekrar gondermez (cifte sayim guvencesi)
chk('havuz kalemi planned sayilir -> mipBuyQty tekrar gondermez',
    mipBuyQty(mipCalcRow({key:'pl-1', code:'PL-1', name:'Plaka', unit:'adet', need:3}, WH, [],
      [pi('PROJE-1', 'Plaka', 'PL-1', 'PLANNED', 3)], 'PROJE-1')) === 0);

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
// (ama rezerve+malKabul toplami HENUZ yetmiyor -> DONE degil)
const rRez2 = mipCalcRow({...g50, need: 40}, WH, mvRezSonrasi, [], 'PROJE-1', rezOnayli);
chk('rezerve+stok yeterli -> RESERVED (FROM_STOCK degil)', rRez2.status === 'RESERVED', rRez2.status);
chk('RESERVED iken eksik 0', rRez2.missing === 0);

// DONE semantigi (8. tur #2 — "ayrim yapmayalim"): ihtiyac NASIL karsilanirsa
// karsilansin, malKabul + rezerve toplami yetiyorsa yesil DONE.
const rKarma = mipCalcRow(g50, WH, [], [pi('PROJE-1', 'M12 Somun', 'SOM-12', 'IN_WAREHOUSE', 25)],
  'PROJE-1', [wres('PROJE-1', 'M12 Somun', 'SOM-12', 'APPROVED', 25, 25)]);
chk('karma karsilamada DONE (25 mal kabul + 25 rezerve = 50)', rKarma.status === 'DONE', rKarma.status);
const rTumRez = mipCalcRow({...g50, need: 30}, WH, [], [], 'PROJE-1', rezOnayli);
chk('tamami rezerveyle karsilaninca da DONE', rTumRez.status === 'DONE', rTumRez.status);

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

console.log('\n═══ mipCalcRow: depo ret uyarisi (9. tur M8) ═══');
// Kural: EN SON sonuclanan talebin cevabi ret / eksikli kismi ise rejectInfo
// dolu; sonraki tam onay uyariyi gizler. SADECE BILGI — missing'e katilmaz
// (redde stok dusumu RESERVATION_ADJUST OUT ile gelir, ayrica saymak cifte olur).
const wresR = (status, reqQty, appQty, reason, at) =>
  ({project_name: 'PROJE-1', item_name: 'M12 Somun', item_code: 'SOM-12', status,
    requested_qty: reqQty, approved_qty: appQty, shortage_reason: reason, approved_at: at});
const rRed = mipCalcRow(g50, WH, hareketler, [], 'PROJE-1',
  [wresR('REJECTED', 10, 0, 'Depoda yok', '2026-07-13T10:00:00')]);
chk('REJECTED -> rejectInfo dolu (sebep tasinir)',
    !!rRed.rejectInfo && rRed.rejectInfo.reason === 'Depoda yok', JSON.stringify(rRed.rejectInfo));
chk('rejectInfo missing formulunu DEGISTIRMEZ', rRed.missing === 10, rRed.missing);
const rKisRed = mipCalcRow(g50, WH, hareketler, [], 'PROJE-1',
  [wresR('PARTIAL', 30, 15, 'Sayimda 15 cikti', '2026-07-13T10:00:00')]);
chk('sebepli PARTIAL uyari tasir (15/30)',
    !!rKisRed.rejectInfo && rKisRed.rejectInfo.approved === 15 && rKisRed.rejectInfo.requested === 30,
    JSON.stringify(rKisRed.rejectInfo));
const rSonOnay = mipCalcRow(g50, WH, hareketler, [], 'PROJE-1', [
  wresR('REJECTED', 10, 0, 'Depoda yok', '2026-07-13T10:00:00'),
  wresR('APPROVED', 10, 10, null, '2026-07-13T11:00:00')]);
chk('sonraki TAM onay eski reddi gizler', !rSonOnay.rejectInfo, JSON.stringify(rSonOnay.rejectInfo||null));
const rSonRed = mipCalcRow(g50, WH, hareketler, [], 'PROJE-1', [
  wresR('APPROVED', 10, 10, null, '2026-07-13T09:00:00'),
  wresR('REJECTED', 5, 0, 'Bitti', '2026-07-13T12:00:00')]);
chk('onaydan SONRAKI ret gorunur', !!rSonRed.rejectInfo && rSonRed.rejectInfo.reason === 'Bitti',
    JSON.stringify(rSonRed.rejectInfo||null));

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

console.log('\n═══ mipGuardMsg: karar degisikligi engel siniflandirmasi (11. tur Y3) ═══');
// Arkadas guard mesajini "hata" sanmisti — artik neden + yonlendirme donuyor.
// Oncelik: islemde (sert) > siparis grubu > kesim plani > havuz bayragi.
const gk = (ek={}) => ({name:'M14 Somun', code:'SOM-14', status:'PLANNED', ...ek});
chk('islemde kalem SERT engel ve oncelikli',
    mipGuardMsg([gk({status:'ORDERED'}), gk({purchase_order_id:'po1'})]).cause === 'ORDERED');
const gGrup = mipGuardMsg([gk({purchase_order_id:'po9'})]);
chk('grup bagi -> GROUP + grupId tasinir', gGrup.cause === 'GROUP' && gGrup.grupId === 'po9');
chk('kesim plani bagi -> PLAN', mipGuardMsg([gk({stock_plan_id:'sp1'})]).cause === 'PLAN');
chk('yalniz havuz bayragi -> POOL', mipGuardMsg([gk({needs_planning:true})]).cause === 'POOL');
chk('serbest PLANNED -> engel yok (null)', mipGuardMsg([gk()]) === null);
chk('mesaj kalem adlarini listeler (ilk 3 + fazlasi)',
    mipGuardMsg([gk({status:'ORDERED', code:'A1'}), gk({status:'ORDERED', code:'A2'}),
                 gk({status:'ORDERED', code:'A3'}), gk({status:'ORDERED', code:'A4'})]).msg.includes('A1, A2, A3 +1'));

console.log('\n═══ mrpLeftoverOwners: artik plakanin proje sahipligi (13. tur madde 3) ═══');
// Depodaki IN_WAREHOUSE kalemler ayni depo + ad/kod eslesmesiyle sahip sayilir;
// baska projeye atanmis plakada "Bunu Kullan" cikmaz, proje adi yazilir.
const loRow = {name:'SAC 500x1000x5', code:null, wh:'W1', unit:'adet', net:1};
const loItems = [
  {name:'SAC 500x1000x5', code:null, status:'IN_WAREHOUSE', warehouse_id:'W1', project_name:'Deneme 2', quantity:1},
  {name:'SAC 500x1000x5', code:null, status:'IN_WAREHOUSE', warehouse_id:'W2', project_name:'Baska Depo', quantity:1},   // farkli depo -> sahip degil
  {name:'SAC 500x1000x5', code:null, status:'PLANNED',      warehouse_id:'W1', project_name:'Planli', quantity:1},        // depoda degil -> sahip degil
  {name:'BASKA MALZEME',  code:null, status:'IN_WAREHOUSE', warehouse_id:'W1', project_name:'Alakasiz', quantity:1},      // ad tutmaz -> sahip degil
  {name:'SAC 500x1000x5', code:null, status:'IN_WAREHOUSE', warehouse_id:'W1', project_name:'ORTAK (MRP)', quantity:2}
];
const loOwners = mrpLeftoverOwners(loRow, loItems);
chk('yalniz ayni depo + IN_WAREHOUSE + ad eslesmesi sahip sayilir',
    loOwners.length === 2 && loOwners.some(o=>o.project_name==='Deneme 2') && loOwners.some(o=>o.project_name==='ORTAK (MRP)'),
    JSON.stringify(loOwners));
chk('adet toplanir (ORTAK 2)', (loOwners.find(o=>o.project_name==='ORTAK (MRP)')||{}).qty === 2);
chk('sahipsiz plaka bos dizi', mrpLeftoverOwners({name:'YOK', code:null, wh:'W1'}, loItems).length === 0);
// kod oncelikli eslesme: kodlar farkliysa ad tutsa da birlesmez (mipMatches kurali)
chk('farkli kod sahiplik yaratmaz',
    mrpLeftoverOwners({name:'SAC 500x1000x5', code:'KOD-A', wh:'W1'},
      [{name:'SAC 500x1000x5', code:'KOD-B', status:'IN_WAREHOUSE', warehouse_id:'W1', project_name:'X', quantity:1}]).length === 0);
chk('ayni kod sahiplik yaratir (ad farkli olsa da)',
    mrpLeftoverOwners({name:'FARKLI AD', code:'KOD-A', wh:'W1'},
      [{name:'SAC 500x1000x5', code:'KOD-A', status:'IN_WAREHOUSE', warehouse_id:'W1', project_name:'X', quantity:1}]).length === 1);

console.log('\n═══ (15. tur T2) mrpRunGuillotine {placed,free} + mrpSheetOffcuts ═══');
// 300×200 plakaya 100×100 tek parça, gap 0: yerleşim (0,0); free = sağ şerit
// 200×100 + üst şerit 300×100 (deterministik — shuffle mrpPackBest'te, burada yok)
const gRes = mrpRunGuillotine([{w:100, h:100, kod:'X', cid:'1', ci:0, _idx:0}], 300, 200, 0);
chk('placed 1 parça', Array.isArray(gRes.placed) && gRes.placed.length === 1);
chk('free dizisi döner', Array.isArray(gRes.free) && gRes.free.length === 2, JSON.stringify(gRes.free));
const offs = mrpSheetOffcuts(gRes.free);
chk('artıklar alan sırasıyla (300×100 önce)',
    offs.length === 2 && offs[0].w === 300 && offs[0].h === 100 && offs[1].w === 200 && offs[1].h === 100,
    JSON.stringify(offs));
chk('minMm eşiği eler (150mm → tek artık)',
    mrpSheetOffcuts(gRes.free, 150).length === 0); // iki şeridin de kısa kenarı 100 < 150
chk('cap sınırı uygulanır',
    mrpSheetOffcuts([{w:500,h:500},{w:400,h:400},{w:300,h:300}], 100, 2).length === 2);
chk('küsurat floor + null girdi boş dizi',
    mrpSheetOffcuts([{w:150.9, h:120.7}])[0].w === 150 && mrpSheetOffcuts(null).length === 0);

console.log('\n' + '─'.repeat(60));
if(fail){ console.log(`❌ ${fail} kontrol BASARISIZ.`); process.exit(1); }
console.log('✅ MİP HESAP ÇEKİRDEĞİ SAĞLAM.');
