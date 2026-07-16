# 13. Tur — Arkadaş İstekleri Uygulama Planı

## Bağlam

Arkadaşın 13. tur istekleri `Desktop\ERP proje\02 - Test ve Istekler\13. test ve değişiklikler\` klasöründe: istek.txt (4 madde) + 3 ekran görüntüsü + Sistem.pdf (6 sayfa) + 38 sn ses kaydı (Whisper dökümü alındı). Dal: `tasarim-2026` (12. tur commit'li, master'a merge edilmedi). Amaç: bölüm atamasını manuelleştirmek, hammaddenin satın almaya otomatik düşmesini durdurmak, başka projenin plakasını kilitleme ve Sistem.pdf'e göre sevkiyat modülü kurmak.

**Ses kaydı dökümü (tamamı Madde 2'ye ait):**
- 0:00–0:19 — "ürün ağacını oluşturup MİP'e gönderdikten sonra hammadde olarak seçilen malzemeler satın almaya otomatik düşüyor"
- 0:19–0:38 — "otomatik düşmesin; ancak ve ancak MİP bölümünde istenilirse düşsün; sistem ürün ağacında hammaddeyi görüp direkt satın almaya göndermesin"

**Kullanıcı kararları (AskUserQuestion, 2026-07-16):**
- K1: Sevkiyat = YENİ ÜST SEKME + yeni `shipping` yetki anahtarı
- K2: Kapsam = sadece sipariş+sevkiyat; garanti modülü, teklif PDF üretimi, fatura durumu ERTELENDİ
- K3: İşlem tanımındaki "Bölüm" alanı UI'dan TAMAMEN kaldırılır (DB kolonu kalır, veri kaybı yok)
- K4: Madde 2 geçişi = sadece YENİ kalemler etkilenir (mevcut PLANNED kayıtlar Satın Alma'da görünür kalır)

**Kodda doğrulanan kritik zemin:**
- Yayınlama (pbomPublishParts 15920-16023) satın almaya kalem YAZMIYOR (8. tur kararı). "Otomatik düşme"nin gerçek kaynakları: `_mipPoolApply` (8425/8450-71, "Havuza Aktar" → anında PLANNED+needs_planning, notes 'MİP havuz kararı (TÜR)'), `mrpApprove` (9729-49, 'MRP planı'), `mrpUseLeftover` (9300-11) — ve `renderPurchaseList`'in (9897-9965) hepsini filtresiz listelemesi.
- Bölüm otomatiği İKİ katmanlı: frontend `ensureProjectDept` (15236, dbInsert yapar!) + `opsDeptName` (15269) + `ensureProjectDepts` (15280) + `pbomeOpDeptId` (15290); çağıranlar: ağaç kopyalama (14903-16), `pbomeAddOpCore` (15327/15335/15346-50), `pbomeRemoveOp` (15679). Backend: `BomOperationService.update:164` → `cascadeDepartmentChange` (284-308).
- Sürükle-bırak emsali VAR: `pbomeDragStart/Over/Drop` (15707-11), HTML5 native draggable (15419).
- Paket/packing/çeki tablosu-UI HİÇ YOK — tamamen yeni. İrsaliye (`delivery_notes`, dnPrint 11805 A4 şablonu, `nextNoteNo` IRS-yıl-sıra deseni) ve depo hareket defteri (stok=SUM(IN)−SUM(OUT)) yeniden kullanılır.
- Parça durumu bileşimi referansı: `woRenderPartsGrid` (6260) + `woMatChip` (6362) + PUR_STATUS/WO_STATUS + canlı depo stoğu.

**Çalışma düzeni:** `tasarim-2026` dalında devam. Her paket: taskkill java → `./mvnw compile` → run → `node scripts/verify-all.js` → ayrı commit. Şema değişen paketlerde (B, D, F) önce `cmd /c C:\erp-backup\erp-backup.cmd` DB yedeği + sonrasında `pg_dump -U postgres --schema-only --no-owner --no-privileges uretim_takip > db/schema.sql`.

## Sıra: A → B → C → D → E → F
A/B/C bağımsız küçük-orta düzeltmeler; D→E→F (Sevkiyat) sıralı bağımlı. Arkadaş cevapları (aşağıdaki A1-A7) gelmeden D-E-F'e başlamak riskliyse A/B/C önce bitirilir.

---

## PAKET A — Bölümler manuel liste (Madde 1) — KÜÇÜK-ORTA
Dosyalar: `index.html`; `bom/BomOperationService.java`; `scripts/verify-opdef-cascade.js`

1. Otomatik oluşturma+türetme kalkar (frontend): ağaç kopyalamada `deptMap`/`opsDeptName` kalkar → `dept_id:null`; `pbomeAddOpCore`'daki `pbomeOpDeptId` çağrısı (15327) ve dept_id atamaları (15335/15346/15350) kalkar; `pbomeRemoveOp` bölüm-türetme bloğu (15675-81, 12. tur m5) kalkar (yorumla belgelenir — m5 bilinçli geri alındı); `ensureProjectDept/ensureProjectDepts/opsDeptName/pbomeOpDeptId` SİLİNİR (test scriptlerinde `grab` eden var mı grep'lenir).
2. Backend cascade kalkar: `BomOperationService.update:161-165` çağrısı + `cascadeDepartmentChange` (284-308) + servis-içi `ensureProjectDept` silinir. `department_name` kolonu/entity alanı KALIR (audit-schema uyumu); `cascadeCodeChange` dokunulmaz.
3. K3 kararı: İşlem Tanımları formundan (`opdef-dept` 13553) ve liste rozetinden (13537) Bölüm alanı kaldırılır; `refreshOpDeptDatalist` (13512) sadeleşir.
4. Manuel bölüm CRUD ağaç editörüne gelir: pbome başlığına "Bölümler" butonu → modal (aktif projenin bölümleri + ekle/sil; `addDept` 4200 / `deleteDept` 4227 desenleri). Satır dropdown'ına (15447-52) son seçenek "+ Yeni bölüm…" → ad sor + `dbInsert('departments',{project,...})` + seç. Planlama > Fabrika Çalışma Alanları ekranı aynen kalır.
5. Republish backfill (15994-97) KALIR — elle atanan dept_id'nin üretime taşınması istenen davranış. "⚠ (başka proje)" seçeneği (15451, 16388) ve F3 guard'ları (15500, 16417) KALIR (eski çapraz kayıtlar görünür kalsın ki düzeltilsin).
6. Test: `verify-opdef-cascade.js` bölüm senaryosu TERSine çevrilir (opdef bölümü değişince pbp dept_id DEĞİŞMEMELİ); kod-cascade senaryoları kalır; verify-all.

Tuzak: `depts[].project` FIELD_XLATE ile order_id'den türer — yeni modal aynı adaptörden geçmeli; verify-pbome-revert shim'lerinde silinen fonksiyon referansı kalmasın.

## PAKET B — Hammadde satın almaya otomatik düşmesin (Madde 2) — ORTA
Dosyalar: `db/schema.sql`; `purchasing/{PurchaseItem}.java` + dto (Request + UpdateRequest + Response); `index.html`

Model: kalem verisi purchase_items'ta kalır (MİP muhasebesi `mipCalcRow.planned` 7740 / `mipBuyQty` 7718 bozulmasın); GÖRÜNÜRLÜK yeni bayrağa bağlanır.

1. Şema: `ALTER TABLE purchase_items ADD COLUMN sent_to_purchasing boolean NOT NULL DEFAULT true;` + dump. K4 kararı: migration UPDATE'i YOK — mevcut kayıtlar görünür kalır.
2. Backend: entity + HEM `PurchaseItemRequest` HEM `PurchaseItemUpdateRequest` (CREATE≠UPDATE tuzağı — needs_planning dersi) + Response. Boolean → ÜÇLÜ enum kuralı gerekmez.
3. Kalem yaratan akışlar: `_mipPoolApply` (8450) insert'e `sent_to_purchasing:false` + 8468-71'deki "PUT ile garanti" deseni bu bayrağa da; `mrpApprove` (9729) `false` + confirm metni güncellenir; `mrpUseLeftover` (9300) plan kalemi `false`; `mipBuyConfirm` (8364) DOKUNULMAZ (explicit "Satın Almaya Gönder" zaten istenen).
4. Satın Alma ekranı: `renderPurchaseList` listesi + sayaç/toplamlar `i.sent_to_purchasing!==false` filtresi; üstte bilgi çipi "MİP'te bekleyen N kalem" (tık → switchTab('mip')).
5. Explicit gönderme MİP'te: Kesim Planlama'ya (renderMrp) "MİP'te Bekleyenler" bölümü: `sent_to_purchasing===false && status==='PLANNED'` kalemler; satırda `btnBusy`'li "Satın Almaya Gönder" → `dbUpdate({sent_to_purchasing:true})`.
6. Etkileşim: `pbomFullSync` (16132) PLANNED iptali aynı; `mipGuardMsg` (7904) değişmez; PURCHASE_ITEM_DUPLICATE guard'ı (PurchaseItemService:72-77) etkilenmez.
7. Test: e2e (POOL kararı → sent=false → gönder → true); verify-mip mevcut senaryolar değişmeden geçmeli; audit-schema; canlı smoke (Create DTO'ya yeni alan — restart ritüeli ŞART).

## PAKET C — Başka projenin plakasında "Bunu Kullan" olmasın (Madde 3) — KÜÇÜK
Dosyalar: yalnız `index.html` (+ `scripts/verify-mip.js`)

1. Yeni SAF fonksiyon `mrpLeftoverOwners(r, purchaseItems)`: `status==='IN_WAREHOUSE' && warehouse_id===r.wh && mipMatches(name/code)` kalemlerden `[{project_name, qty}]`. Veri hazır (renderMrp purchaseItems'ı taze çeker 9176-81). Rezervasyonlar gerekmez: APPROVED rezervasyon OUT yazdığından o stok zaten whStockRows net'inde görünmez.
2. `renderMrpLeftovers` (9246-58): owner varsa rozet "Proje: X" ('ORTAK (MRP)' → "Ortak havuz"); owner'lı satırda "Bunu Kullan" ÇIKMAZ — İSTİSNA: owner projeleri, havuzdaki seçili kalemlerin projeleriyle örtüşüyorsa buton kalır (kendi projesine kullanabilsin).
3. `mrpUseLeftover` (9261) başına guard (bayat DOM'a karşı): owners doluysa ve seçili proje örtüşmüyorsa toast + return.
4. Bilinçli sınırlama (plana not): owner kalemin kısmi tüketimi v1'de miktar bazlı hesaplanmaz — rozet + proje düzeyinde guard.
5. Test: verify-mip'e `mrpLeftoverOwners` senaryoları (aynı/farklı depo, kod öncelikli eşleşme, ORTAK); shot-mip.

## PAKET D — Sevkiyat: şema + backend modülü (Madde 4/1) — ORTA-BÜYÜK
Dosyalar: `db/schema.sql`; YENİ `shipment/` modülü (delivery/ birebir şablon); `config/SecurityConfig.java`; `index.html` (yalnız adapter+perm)

1. Şema (yedek + dump):
```sql
CREATE TABLE shipment_packages (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  package_no varchar(30) NOT NULL UNIQUE,          -- 'PKT-<yıl>-<sıra>' backend üretir
  project_name varchar(100) NOT NULL,
  name varchar(150),
  length_cm numeric(10,2), width_cm numeric(10,2), height_cm numeric(10,2),
  weight_kg numeric(12,3),
  status varchar(20) NOT NULL DEFAULT 'OPEN'
    CONSTRAINT shipment_packages_status_chk CHECK (status IN ('OPEN','CLOSED','LOADED','SHIPPED')),
  delivery_note_id uuid,
  packed_by varchar(150), packed_at timestamp,
  notes text, created_by varchar(150),
  created_at timestamp DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE shipment_package_items (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  package_id uuid NOT NULL REFERENCES shipment_packages(id) ON DELETE CASCADE,
  part_id uuid, project_bom_part_id uuid,
  item_name varchar(200) NOT NULL, item_code varchar(100),
  quantity numeric(15,4) NOT NULL CHECK (quantity > 0),
  unit varchar(20) DEFAULT 'adet',
  created_at timestamp DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE project_bom_parts ADD COLUMN ship_planned boolean NOT NULL DEFAULT false;
```
2. package_no: `DeliveryNoteService.nextNoteNo` (163-170) deseni `PKT-2026-0001` + DB UNIQUE ("hiçbir paket mükerrer olmayacak" şartı DB'de garanti).
3. Durum ÜÇLÜ kural: CHECK + `@Pattern(OPEN|CLOSED|LOADED|SHIPPED)` HEM Request HEM UpdateRequest + service geçiş whitelist'i (OPEN→CLOSED→LOADED→SHIPPED; CLOSED'a packed_by zorunlu, packed_at=now service yazar). Canlı smoke şart (validation yalnız HTTP katmanında — POOL dersi).
4. `ship_planned`: `ProjectBomPartRequest` VE `ProjectBomPartUpdateRequest` + Response (CREATE≠UPDATE tuzağı).
5. SecurityConfig: `writeRule("/api/shipment-packages/**","/api/shipment-package-items/**")` → yeni `shipping` yetkisi (delivery-notes deseni, ~satır 121).
6. Adapter/perm: TABLE_ENDPOINTS'e iki tablo (2136-2170); `allPerms`'e 'shipping' (12702) + yetki grid'ine perm-item (1616 deseni). Yetki mevcut kullanıcılara ELLE verilecek (orders_quotes gibi — tur sonu notu).
7. Test: audit-schema; verify-authz'a shipping senaryosu; e2e: paket oluştur → no formatı + sıra artışı + geçersiz durum 400.

## PAKET E — Sevkiyat ekranı: ağaç + paket planı + paketleme (Madde 4/2) — BÜYÜK
Dosyalar: yalnız `index.html` (+ `scripts/audit-ui-sweep.js`, `scripts/verify-h-render.js`)

1. K1 kararı: yeni üst sekme `data-tab="shipping"` "Sevkiyat" (Depo'nun yanına). Nav sözleşmesi (783): `switchTab`/`applyPermissions` genel yolları `perms.includes('shipping')` ile otomatik çalışır; `switchTab`'a `if(tab==='shipping') renderShipping();`; **`audit-ui-sweep.js:13` TABS listesine 'shipping' eklenir** (11 sekme hardcoded). Konsept parity MAP'inde sevkiyat yok — parity'ye girmez ama tasarım dili kuralları (phead/stat-card/st pilleri, emoji yasak, h`` disiplini) uygulanır.
2. Ekran 1 — Proje ağacı + durum: proje seçici (`activeOrders()` 5145 — teklifler girmez) → `shipRenderTree()`: `woRenderPartsGrid` (6260-6356) deseninin okuma kopyası; düğümde durum bileşimi: üretildi (qty_done>=qty) / üretimde+aşama (bölüm adı + WO_STATUS) / satın alma aşaması (`woMatChip` 6362, PUR_STATUS) / depoda (IN_WAREHOUSE+whName) / stokta; artı paketlenen/kalan rozetleri (shipment_package_items toplamı) ve ship_planned "SEVK PLANINDA" rozeti.
3. Paket Planlaması: pbome ağaç satırına (15445-57) "sevk planına al/çıkar" toggle → `dbUpdate('project_bom_parts',{ship_planned})`; sevkiyat ağacında "yalnız plandakiler" filtresi. (Granülarite A1 sorusuna göre v2'de adetli olabilir — v1 işaret.)
4. Ekran 2 — Paketleme: iki panel. Sağ: paket kartları — "+ Paket" (ad, en/boy/yükseklik cm, ağırlık kg; sonradan düzenlenebilir), package_no backend'den. Sol: ağaç düğümleri draggable — pbome DnD deseni (15707-11) birebir `shipDragStart/Over/Drop`; drop'ta miktar modalı (kalan öneri; 3 adet → 2+1 bölünmesi = iki shipment_package_items satırı); kalan 0 → düğüm soluk. Her insert `btnBusy` + item_name/item_code SNAPSHOT.
5. "Paketi Kapat": paketleyen seçimi + status CLOSED (packed_at service'te). OPEN'da satır ekle/çıkar serbest, CLOSED kilitli (service guard).
6. Depo stoğu paketlemede DÜŞMEZ (varsayılan; sevk anında dnShip düşürür — A2 sorusuna göre değişebilir).
7. Test: verify-h-render'a shipRenderTree/paket kartı XSS senaryoları; e2e miktar bölme + kalan hesabı; audit-ui-sweep yeni sekmeyi tarar; playwright sekme turu.

## PAKET F — Sevkiyat: PDF + QR + araç/yükleme + belgeler (Madde 4/3) — ORTA-BÜYÜK
Dosyalar: `db/schema.sql`; `delivery/{DeliveryNote,DeliveryNoteService}.java` + dto (Request+UpdateRequest+Response); `shipment/`; `index.html`

1. Şema: `ALTER TABLE delivery_notes ADD COLUMN vehicle_plate varchar(20), ADD COLUMN driver_name varchar(150), ADD COLUMN container_no varchar(50), ADD COLUMN tir_no varchar(50), ADD COLUMN cargo_tracking_no varchar(100), ADD COLUMN eta_date date;` (carrier=nakliye firması zaten var) + iki DTO'ya alanlar + dump.
2. Paket→araç: irsaliye detayına "Paket Yükle" — projenin CLOSED paketleri seçilir → delivery_note_id + LOADED; `dnShip` (11740) sonunda bağlı paketler SHIPPED; `dnUnship` (11776) LOADED'a geri alır. İrsaliye formuna araç alanları; `dnPrint` (11805) Sevk Bilgileri bloğuna (11860-69) plaka/şoför/konteyner/TIR/takip no/varış eklenir.
3. Paket detay PDF + QR: `pkgPrint(id)` (dnPrint deseni): başlık (no, proje, ölçü, ağırlık, paketleyen+zaman) + içerik tablosu + `qrDataUrl(QR_BASE+'?paket='+id, 110)` (4357 — print penceresinde CDN'siz kanıtlı çözüm).
4. `?paket=` sayfası: `init` (3064-3109) `?part=`/`?receive=` üçüz deseni: login-gated `renderPackageView(id)` — paket bilgisi, paketleyen, paketleme zamanı, içerik, bağlı irsaliye/araç. (Halka açık uç istenmedikçe login'li — güvenlik varsayılanı.)
5. Packing List + Çeki Listesi: `dnPackingList(dnId)` (paket no/ölçü/adet/içerik) ve `dnWeighList(dnId)` (paket başına brüt ağırlık + toplam; kolonlar A3'e göre netleşir) — dnPrint kardeşleri, aynı A4 dili.
6. Sipariş kartına türetilmiş sevkiyat rozeti: bağlı irsaliye durumlarından "Hazırlanıyor / Yüklendi / Sevk Edildi" çipi (orders'a kolon YOK). K2 kararı: fatura durumu ve kalıcı durum zinciri KAPSAM DIŞI (A4 cevabına göre sonraki tur).
7. Test: e2e LOADED/SHIPPED zinciri + CLOSED-olmayan paket yüklenemez guard'ı; canlı QR smoke (?paket=); verify-authz delivery/shipping ayrımı; verify-all.

---

## ARKADAŞA İLETİLECEK SORULAR (kendisi "belirsiz noktaları bildirin" dedi — uygulama sırasında cevap beklenmeden A/B/C yapılabilir; D-E-F'te varsayılanlar plandaki gibi)
- **A1 — Paket Planlaması granülaritesi:** ürün ağacındaki plan parça başına işaret mi, adet bazlı mı ("5 adedin 3'ü sevk edilecek")? Planı kim yapar — ağacı düzenleyen planlamacı mı? (Varsayılan: parça başına işaret, planlamacı yapar.)
- **A2 — Stok düşümü zamanı:** parça pakete konunca depodan hemen mi düşsün, araç sevk edilince mi (mevcut irsaliye davranışı)? (Varsayılan: sevk anında.)
- **A3 — Çeki listesi/Packing list kolonları:** brüt/net ayrımı, hacim (m³), koli sıra no — örnek şablon var mı?
- **A4 — Durum zinciri:** Hazırlanıyor→Yüklendi→Sevk Edildi→Teslim Edildi SİPARİŞ üzerinde mi İRSALİYE üzerinde mi izlensin? "Teslim Edildi"yi kim işaretler? Fatura durumu neyden beslenecek (fatura modülü yok)?
- **A5 — Madde 2 sınırı:** Kesim Planlama'daki "Onayla" (MRP planı) kalemi de mi satın almaya düşmesin, yoksa yalnız "Havuza Aktar" mı rahatsız ediyor? (Varsayılan: ikisi de MİP'te bekler, tek tuşla gönderilir.)
- **A6 — Madde 3:** birden çok projenin ortak plakası ('ORTAK (MRP)') serbest mi kalsın, o da kilitli mi? (Varsayılan: "Ortak havuz" rozetiyle serbest.)
- **A7 — Madde 1:** bölüm ataması %100 elle mi; yoksa parça bölümü BOŞKEN işlem eklenirse MEVCUT bölümlerden ad eşleşmesi önerilsin mi (oluşturma asla yok)? (K3 kararıyla varsayılan: %100 elle.)

## Kapsam dışı / ertelenen (K2 kararı)
- Garanti modülü (Sistem.pdf sayfa 2-4: servis talebi, garanti sevkiyatı, garanti gideri, makine yaşam döngüsü)
- Teklif PDF otomatik üretimi (PDF sayfa 2)
- Satış dashboard KPI'ları (PDF sayfa 1)
- Sipariş ekranında fatura durumu (fatura modülü yok)
- Paketlemede QR/barkod ile yükleme doğrulama, konteyner doluluk, otomatik e-posta (PDF "gelişmiş özellikler")

## Doğrulama (her paket sonunda)
1. taskkill /F /IM java.exe → `./mvnw compile` → run (restart ritüeli — CLAUDE.md tuzağı)
2. `node scripts/verify-all.js` (kapı geçmeden "bitti" denmez)
3. Pakete eklenen bekçi senaryoları: verify-opdef-cascade (A) / verify-mip + e2e (B, C) / verify-authz + audit-schema (D) / verify-h-render + audit-ui-sweep + playwright (E) / e2e + canlı QR smoke (F)
4. Canlı smoke: B'nin Create-DTO alanı ve D'nin @Pattern'i özellikle (validation yalnız HTTP'de)
5. Ekran görüntüsü kullanıcıya (shot-*.js deseni); arkadaş smoke'u için tünel yenileme
6. Şema paketlerinde (B, D, F) önce DB yedeği, sonra schema.sql dump

## Tur sonu
- Plan `docs/plan-arkadas-istekleri-13-tur.md` olarak depoya kopyalanır (önceki tur deseni)
- `shipping` yetkisi mevcut kullanıcılara elle verilir (orders_quotes gibi)
- Bellek güncellenir; arkadaşa: A1-A7 soru listesi + test tarifli özet rapor + tünel

---

## SONUÇ (2026-07-17 — TAMAMLANDI)
6 paketin tamamı kodlandı, her paket ayrı commit + doğrulama kapısı 14/14:
- **A `b90806c`** — Bölümler manuel liste (madde 1)
- **B `27086fd`** — Hammadde satın almaya otomatik düşmesin (madde 2, sent_to_purchasing)
- **C `b11b53d`** — Başka projenin plakasında "Bunu Kullan" yok (madde 3, mrpLeftoverOwners)
- **D `751ac83`** — Sevkiyat şema + backend (shipment_packages/items, shipping yetkisi)
- **E `259d12f`** — Sevkiyat ekranı (ağaç+durum, paket planı, sürükle-bırak paketleme)
- **F `6e554d7`** — PDF+QR (pkgPrint, ?paket=), araç bilgileri, Packing List + Çeki Listesi, sipariş rozeti

DB yedekleri: 2026-07-17_0018 (B öncesi), _0039 (D öncesi), _0100 (F öncesi).
Yeni bekçi senaryoları: verify-opdef-cascade (ters bölüm), verify-mip (mrpLeftoverOwners ×5),
verify-h-render (ship* + dn paket/araç), verify-authz (shipping ×4), e2e (m2 ×4 + m4 D/F zinciri),
audit-ui-sweep TABS+shipping, audit-op-dept.spec manuel akışa yeniden yazıldı, shot-shipping.js.

AÇIK: `shipping` yetkisi mevcut kullanıcılara ELLE verilecek; arkadaşa A1-A7 soruları iletilecek;
arkadaş smoke sonrası master merge kararı.
