// SAHİPSİZ KAYIT TARAYICISI (5. denetim turu, Bölüm 3) — SALT OKUNUR.
//
// Şemada 45 FK var, ama üç tablo projeye ADIYLA bağlı (orders.project_name
// UNIQUE; parts / purchase_items / project_bom bu adı string olarak taşır).
// String bağ FK ile korunmadığından, proje adı elle değiştirilirse veya bir
// kayıt yanlış adla yazılırsa SAHİPSİZ kalır: hiçbir projede görünmez ama
// DB'de durur. K2 turu adlandırmayı taşıyacak kodu ekledi; bu script canlı
// veride kalıntı olup olmadığını doğrular.
//
// Ayrıca UUID bağlarında (FK olsa da SET NULL / eski veri ihtimaline karşı)
// hedefi kaybolmuş kayıtlar aranır. HİÇBİR ŞEY YAZILMAZ/SİLİNMEZ.
//
// Kullanım: node scripts/audit-orphans.js    (sunucu 8080'de çalışmalı)
const BASE = 'http://localhost:8080/api/';

async function login() {
  const r = await fetch(BASE + 'auth/login', { method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'testdev', password: 'test1234' }) });
  const j = await r.json();
  return j.data && (j.data.token || j.data.access_token);
}
let TOK;
async function get(ep) {
  const r = await fetch(BASE + ep, { headers: { Authorization: 'Bearer ' + TOK } });
  if (!r.ok) throw new Error(ep + ' → HTTP ' + r.status);
  const j = await r.json();
  return Array.isArray(j.data) ? j.data : [];
}

let sorun = 0;
function rapor(baslik, sahipsizler, tarif) {
  if (!sahipsizler.length) { console.log(`  ✅ ${baslik}`); return; }
  sorun += sahipsizler.length;
  console.log(`  ❌ ${baslik} — ${sahipsizler.length} sahipsiz kayıt`);
  for (const s of sahipsizler.slice(0, 5)) console.log(`       • ${tarif(s)}`);
  if (sahipsizler.length > 5) console.log(`       … ve ${sahipsizler.length - 5} tane daha`);
}

(async () => {
  TOK = await login();
  if (!TOK) throw new Error('testdev login basarisiz — sunucu ayakta mi?');

  const [orders, parts, purchaseItems, projectBom, projectBomParts,
         bomParts, bomProducts, workOrders, warehouses, whMovements,
         dnItems, deliveryNotes] = await Promise.all([
    get('orders'), get('parts'), get('purchase-items'), get('project-bom'),
    get('project-bom-parts'), get('bom-parts'), get('bom-products'),
    get('work-orders'), get('warehouses'), get('warehouse-movements'),
    get('delivery-note-items'), get('delivery-notes')
  ]);

  const projeAdlari = new Set(orders.map(o => o.project_name));
  const id = arr => new Set(arr.map(x => x.id));
  const bomPartIds = id(bomParts), bomProductIds = id(bomProducts);
  const projectBomIds = id(projectBom), warehouseIds = id(warehouses);
  const dnIds = id(deliveryNotes);

  console.log(`Veri: ${orders.length} proje · ${parts.length} üretim parçası · ` +
              `${purchaseItems.length} satın alma kalemi · ${bomParts.length} ağaç parçası\n`);

  console.log('── Projeye ADIYLA bağlı tablolar (FK yok — K2 riski) ──');
  rapor('parts.project_name → orders',
    parts.filter(p => p.project_name && !projeAdlari.has(p.project_name)),
    p => `parça "${p.name}" → bilinmeyen proje "${p.project_name}"`);
  rapor('purchase_items.project_name → orders',
    purchaseItems.filter(i => i.project_name && !projeAdlari.has(i.project_name)),
    i => `kalem "${i.name}" → bilinmeyen proje "${i.project_name}"`);
  rapor('project_bom.project_name → orders',
    projectBom.filter(b => b.project_name && !projeAdlari.has(b.project_name)),
    b => `bağlantı ${b.id} → bilinmeyen proje "${b.project_name}"`);
  rapor('work_orders.project_name → orders',
    workOrders.filter(w => w.project_name && !projeAdlari.has(w.project_name)),
    w => `iş emri ${w.id} → bilinmeyen proje "${w.project_name}"`);

  console.log('\n── UUID bağları (hedefi kaybolmuş kayıtlar) ──');
  rapor('bom_parts.product_id → bom_products',
    bomParts.filter(p => p.product_id && !bomProductIds.has(p.product_id)),
    p => `parça "${p.code}" → kayıp ürün ${p.product_id}`);
  rapor('bom_parts.parent_id → bom_parts',
    bomParts.filter(p => p.parent_id && !bomPartIds.has(p.parent_id)),
    p => `parça "${p.code}" → kayıp üst parça ${p.parent_id}`);
  rapor('project_bom_parts.project_bom_id → project_bom',
    projectBomParts.filter(p => p.project_bom_id && !projectBomIds.has(p.project_bom_id)),
    p => `pbom parçası ${p.id} → kayıp bağlantı ${p.project_bom_id}`);
  rapor('project_bom_parts.bom_part_id → bom_parts',
    projectBomParts.filter(p => p.bom_part_id && !bomPartIds.has(p.bom_part_id)),
    p => `pbom parçası ${p.id} → kayıp ağaç parçası ${p.bom_part_id}`);
  rapor('warehouse_movements.warehouse_id → warehouses',
    whMovements.filter(m => m.warehouse_id && !warehouseIds.has(m.warehouse_id)),
    m => `hareket ${m.id} → kayıp depo ${m.warehouse_id}`);
  rapor('delivery_note_items.delivery_note_id → delivery_notes',
    dnItems.filter(x => x.delivery_note_id && !dnIds.has(x.delivery_note_id)),
    x => `irsaliye kalemi "${x.item_name}" → kayıp irsaliye ${x.delivery_note_id}`);

  console.log('\n── Ağaç bütünlüğü ──');
  // Kendi kendinin üstü olan parça (döngü) — sonsuz render/rekürsiyon riski
  rapor('bom_parts: kendi kendinin üstü',
    bomParts.filter(p => p.parent_id === p.id),
    p => `parça "${p.code}" (${p.id}) kendi kendinin üstü`);
  // Üst parçası BAŞKA ürüne ait olan parça
  const urunOf = new Map(bomParts.map(p => [p.id, p.product_id]));
  rapor('bom_parts: üst parça farklı üründe',
    bomParts.filter(p => p.parent_id && urunOf.has(p.parent_id) && urunOf.get(p.parent_id) !== p.product_id),
    p => `parça "${p.code}" üst parçası başka ürüne ait`);

  console.log('\n' + '─'.repeat(60));
  if (sorun) { console.log(`❌ Toplam ${sorun} sahipsiz/tutarsız kayıt.`); process.exit(1); }
  console.log('✅ Sahipsiz kayıt yok — referans bütünlüğü sağlam.');
})().catch(e => { console.error('HATA:', e.message); process.exit(1); });
