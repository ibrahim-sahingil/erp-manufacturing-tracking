# 15. Tur — Arkadaş Cevapları (Y1-Y3 + T1-T2) Uygulama Planı

## Bağlam

15. tur = 13. tur raporuna eklenen yeni soruların (Y1-Y3 + T1-T2) arkadaş cevapları
(kullanıcı 2026-07-20'de iletti):

- **Y1:** "Paketler Sevkiyat Deposuna Aktarılır. Bu depoda paketler ve içerikleri kayıt
  altında tutulur. Nakliye planlaması yapılıp araç-tır-konteynır gelince paketler depoda
  çekilip araçlara devredilir ve çıkış yapılmış olur." → rapordaki "fiili depo hareketi
  yazılsın" seçeneği: paket depoya GİRER, araca devirde ÇIKAR, depo sayımlarında görünür.
- **Y2:** Sevkiyat'a **Yükleme** sekmesi: aktif sevk edilmemiş paketlerin tümü + proje
  filtresi; araç bilgileri + varış adresi + hangi şirkete (alıcı) + hangi şirket üzerinden
  (nakliyeci); şirket detayları sabit kayıt. **"a-) Evet"** = rapor şıkkı (a): bir kere
  girilen sabit firma ayarları (ad/adres/logo) + teslim koşulu ve menşei irsaliye başına.
- **Y3:** "Muhasebe programı faturalandırma işlemi yapıyor. Buna gerek yok." → fatura
  durumu/modülü KALICI kapsam dışı (rapora not düşülür, kod yok).
- **T1:** "Girişsiz açılsın" → `?paket=` QR sayfası login'siz çalışacak.
- **T2:** "Plakalar kullanıldıktan sonra fire plaka kalabilir. Bu Plaka depoya girilip
  tekrar kullanılabilir." → plan sonucundaki artık plaka tek tuşla depoya girilebilecek.

Dal: `tasarim-2026` (14. tur üstüne). Her paket ayrı commit + `node scripts/verify-all.js`
kapısı (EXIT + BAŞARISIZ grep — `| tail` tuzağı yok). Şema değişen paketlerde önce
`cmd /c C:\erp-backup\erp-backup.cmd` yedeği + sonrasında schema dump.

## Varsayılan kararlar (kullanıcı plan modundan çıktı — itiraz ederse geri alınır)

- **V1 (Y1):** GİRİŞ hareketi paket **KAPATILINCA** (OPEN→CLOSED, fiziken hazır), ÇIKIŞ
  **araca yüklenince** (CLOSED→LOADED — arkadaşın "araçlara devredilir ve çıkış yapılmış
  olur" cümlesi). Geri almalar (reopen/unload/sil) hareketi siler. Depo seçilmemiş pakete
  hareket yazılmaz; kapatırken UI uyarır.
- **V2 (Y1):** Paket başına TEK hareket satırı (ad=paket no + adı, miktar 1 'adet').
  İçerik zaten `shipment_package_items`'ta — parça bazlı çift defter tutulmaz.
- **V3 (Y2):** Yükleme sekmesi MEVCUT irsaliye altyapısı üstüne kurulur (yeni paralel
  varlık yok): araç+adres+alıcı = `delivery_notes` alanları. Nakliyeciler yeni `carriers`
  kartoteki (suppliers klonu); alıcı firma mevcut suppliers datalist'i.
- **V4 (T1):** Halka açık uç TEK paketi toplu döner (paket + içerik + depo adı + bağlı
  irsaliyenin araç özeti). Liste uçları kilitli kalır (aksi tüm paket/irsaliye verisini
  sızdırır — keşif bulgusu: `renderPackageView` bugün 4 liste ucu çekiyor). `notes`,
  `created_by` gibi iç alanlar dönmez. UUID tahmin edilemez.
- **V5 (T2):** v1 SAC artıkları: plan sonucunda plaka başına EN BÜYÜK boş dikdörtgen
  (guillotine `free` listesi zaten hesaplıyor, bugün atılıyor). Ad konvansiyonu
  "Artık Saç <G>x<Y>" — `_mrpLeftoverList` regex'i (9893-9899) bunu zaten çözüyor.
  Profil kalan mm gösterimde var; profil artığının depoya girişi v2.

## Kod zemini (keşif, 2026-07-20)

- `?paket=` router: index.html 3207-3227 (login-gated üçüz desen; `_qrPaketId` 7999,
  login-sonrası replay 8086-8089). `renderPackageView` 12911-12956: 4 × `dbGet` LİSTE
  çağrısı (adapter `id=eq.`'yu path'e çevirmiyor — 2712-2743). `apiFetch` 2281-2304
  token'sız da çalışır. SecurityConfig: permitAll 57-64, `anyRequest().authenticated()`
  153, shipping writeRule 124-128 (yalnız POST/PUT/DELETE — GET'e dokunmaz).
- Paket durum makinesi: `ShipmentPackageService.applyStatusChange` 144-179 (komşu geçişler;
  CLOSED packed_by, LOADED delivery_note_id ister). TÜM durum geçişleri PUT /api/
  shipment-packages/{id} üzerinden geçer → Y1 hareket kancası SERVİSTE tek yerden yazılır.
- Defter: `warehouse_movements` (schema 587-605; stok=ΣIN−ΣOUT `whStockRows` 13444-13464,
  ayrı stok tablosu yok). `whmAddMovement` 13368-13404 manuel giriş deseni.
- Çeki listesi: `dnWeighList` 12837+ — Gönderen/Consignor hücresi BOŞ (12882); teslim
  koşulu/menşei alanı yok. `dnPackingList` 12793. Ortak CSS `_SEVK_PDF_CSS`.
- İrsaliye formu: 1633-1671 (araç alanları 1658-1666, `dn-carrier` çıplak text 1656,
  `dn-recipient` sup-datalist+autofill 1636). `dnCreate` 12222-12227. `dnPkgLoad` 12446
  (CLOSED+bağsız+AYNI PROJE adayları 12394), `dnShip` 12544 (OUT hareketleri + paketler
  SHIPPED), `dnUnship` 12588.
- Sipariş zinciri: `SHIP_CHAIN` ~5459, `orderShippingRaise` 5466 (yalnız yükseltir),
  `orderShippingRecompute` 5478 (geri almalarda taze veriden).
- Suppliers kartotek şablonu: `supplier/` modülü (name/contact/phone/email/address/tax/
  notes/is_active); frontend `refreshSupplierDatalist` 9256, `ensureSupplier` 9265,
  `openSuppliersModal` 9280. SecurityConfig suppliers writeRule 137-138.
- MRP: `mrpRunGuillotine` 10117-10154 `free` dikdörtgen listesi tutar ama yalnız `placed`
  döner (10153); `mrpCalcSac` 10165-10217 `_mrpResult`; `mrpRenderSacResult` 10238+ plaka
  başına fire %. Artık plaka bugün ELLE "Parça Ekle"den adında ölçüyle giriliyor
  (boş-durum ipucu 9908); `_mrpLeftoverList` 9891-9901 adı regex'le çözer.
- Depolarda tip kolonu YOK (hepsi genel) — "Sevkiyat Deposu" = kullanıcının seçtiği
  normal depo; tip kolonu eklenmez (gereksiz).

## Sıra: A → B → C → D → E → F
A, B, C birbirinden bağımsız; D → E sıralı bağımlı (E, D'nin alanlarını basar). F kapanış.

---

## PAKET A — T1: `?paket=` girişsiz (KÜÇÜK-ORTA)
Dosyalar: `shipment/ShipmentPackageController.java` + `ShipmentPackageService.java` +
YENİ `dto/PublicPackageResponse.java`; `config/SecurityConfig.java`; `index.html`;
`scripts/verify-authz.js`

1. Backend: `GET /api/shipment-packages/{id}/public` → tek payload:
   paket (no, ad, proje, tip, ölçüler, brüt/net, durum, packed_by, packed_at) + items
   (ad/kod/miktar/birim) + warehouse_name + bağlı irsaliye özeti (note_no, vehicle_plate,
   driver_name, carrier, ship_date). `notes`/`created_by` DÖNMEZ (V4).
2. SecurityConfig: `auth.requestMatchers(HttpMethod.GET,
   "/api/shipment-packages/*/public").permitAll();` — permitAll bloğuna (57-64 bölgesi,
   `anyRequest` 153'ten ÖNCE). writeRule'la çakışmaz (o yalnız yazma metodları).
3. Frontend: router 3207-3227 branşı login kapısız `renderPackageView(pkid)` çağırır;
   `renderPackageView` 4 dbGet yerine `apiFetch('/api/shipment-packages/'+id+'/public')`
   (token'sızken Authorization başlığı zaten eklenmiyor). `_qrPaketId` + replay (8086-89)
   kaldırılır. `?receive=` GATED KALIR (mal kabul yazma işlemi yapıyor).
4. Login ekranı gizlenirken QR sayfa düzeni korunur (nav/side gizleme aynen).
5. Test: verify-authz'a 3 senaryo — token'sız public GET 200; token'sız
   `/api/shipment-packages` listesi 401; public path'e POST/PUT 401/403.
   Canlı smoke: curl token'sız. Playwright: `?paket=` login'siz içerik görüyor.

## PAKET B — T2: Fire plaka depoya tek tuş (KÜÇÜK-ORTA)
Dosyalar: yalnız `index.html` + `scripts/verify-mip.js`

1. `mrpRunGuillotine` dönüşüne `free` eklenir (`{placed, free}`) — çağıranlar uyarlanır.
2. Yeni SAF fonksiyon `mrpSheetOffcuts(plateResult, minMm=100)`: plaka başına en büyük
   boş dikdörtgen(ler) (her iki kenar ≥ minMm) → `[{w,h}]` (V5).
3. `mrpRenderSacResult`: plaka satırına "Artık: <G>×<Y> mm" + "Depoya Gir" butonu →
   modal (depo seç [whs aktif], ad önerisi `Artık Saç <G>x<Y>`, miktar 1, not: plan
   referansı) → `warehouse_movements` IN insert (`source_type:'MANUAL'`,
   `whmAddMovement` çekirdeği yeniden kullanılır/ortaklanır). `btnBusy` + insert dönüş
   kontrolü.
4. Girilen artık, MİP artık panelinde (`_mrpLeftoverList`) otomatik görünür — mevcut
   regex adı çözer; ekstra iş yok. Boş-durum ipucu metni (9908) güncellenir ("plan
   sonucundan tek tuşla da girebilirsiniz").
5. Profil: bar başına `kalan mm` zaten basılıyor — yanına bilgi notu; depoya giriş v2
   (plana not).
6. Test: verify-mip'e `mrpSheetOffcuts` senaryoları (tek/çok dikdörtgen, min eşik,
   boş plaka); verify-h-render yeni render dokunuşları için shim GLOBAL kontrolü
   (3 kez tekrarlayan ders!).

## PAKET C — Y1: Paket depo hareketleri (ORTA) — ŞEMA DEĞİŞİR
Dosyalar: `db/schema.sql`; `warehouse/{WarehouseMovement,WarehouseMovementService}.java`
+ dto; `shipment/ShipmentPackageService.java`; `index.html`; `scripts/e2e-test.js`

1. Şema (önce DB yedeği): `ALTER TABLE warehouse_movements ADD COLUMN shipment_package_id
   uuid REFERENCES shipment_packages(id) ON DELETE SET NULL;` + source_type CHECK'ine
   `'PACKAGE'` eklenir. ÜÇLÜ kural: DB CHECK + service validasyonu + DTO @Pattern
   (HEM Request HEM UpdateRequest — CREATE≠UPDATE tuzağı). Dump.
2. `ShipmentPackageService.applyStatusChange` kancaları (tek merkez — frontend'in tüm
   yolları PUT'tan geçer):
   - OPEN→CLOSED: `warehouse_id` doluysa IN hareketi (name=`<package_no> — <name>`,
     qty 1 'adet', source_type PACKAGE, shipment_package_id).
   - CLOSED→OPEN: paketin PACKAGE-IN hareketi silinir.
   - CLOSED→LOADED: OUT hareketi (araca devir = çıkış — V1).
   - LOADED→CLOSED: OUT silinir. LOADED↔SHIPPED: hareket yok (zaten çıkmış).
   - Paket SİL (OPEN/CLOSED): bağlı PACKAGE hareketleri silinir.
   - CLOSED'da warehouse_id DEĞİŞİRSE: mevcut IN hareketinin deposu güncellenir.
   Hepsi aynı transaction'da (@Transactional); hareket yazılamazsa geçiş de düşer.
3. Frontend: `shipPkgClose` depo seçilmemişse confirm uyarısı ("depo hareketi
   yazılmayacak"); depo görünümünde PACKAGE kaynaklı satıra "paket" rozeti
   (`whStockRows` zaten toplar — kod değişikliği renderda). Paket kartına "depoda /
   araçta" ibaresi zaten durum renginde var — dokunulmaz.
4. Adapter: warehouse_movements FIELD_XLATE/kolon eklemesi gerekiyorsa güncellenir.
5. Test: e2e zinciri — paket kur→kapat (stok +1) → araca yükle (stok 0) → geri al
   (stok +1) → yeniden aç (stok 0) → depo değiştir senaryosu; audit-schema; verify-all.

## PAKET D — Y2 backend: firma ayarları + nakliyeci kartoteki + irsaliye alanları (ORTA) — ŞEMA DEĞİŞİR
Dosyalar: `db/schema.sql`; YENİ `company/` (veya `settings/`) modülü; YENİ `carrier/`
modülü (supplier klonu); `delivery/DeliveryNote.java` + dto; `config/SecurityConfig.java`;
`index.html` (adapter + TABLE_ENDPOINTS)

1. Şema (önce DB yedeği):
   - `company_settings` TEK SATIR: id, name varchar(200) NOT NULL, address text,
     phone varchar(50), email varchar(150), tax_office varchar(100), tax_number
     varchar(50), logo bytea, logo_content_type varchar(100), updated_at. (Y2a: ad/adres/
     logo bir kere girilir.)
   - `carriers`: suppliers birebir klonu (name, contact_person, phone, email, address,
     notes, is_active) — nakliye firmaları.
   - `delivery_notes` += `delivery_terms varchar(100)` (örn. "DPU - Dnipro"),
     `origin_country varchar(100)` (örn. "Turkey") — HEM Request HEM UpdateRequest.
2. Endpoint'ler: `GET/PUT /api/company-settings` (yoksa boş yaratır, tek satır);
   `/api/carriers` CRUD (SupplierController klonu). Logo: JSON base64 alanı (tek logo,
   küçük). SecurityConfig writeRule: company-settings + carriers → "delivery",
   "shipping" (+ROLE_DEVELOPER).
3. Adapter: TABLE_ENDPOINTS'e `carriers`, `company_settings`; FIELD_XLATE gerekirse.
4. Test: audit-schema; verify-authz'a carriers/company-settings yazma kısıtı; canlı
   smoke (yeni DTO alanları — restart ritüeli ŞART).

## PAKET E — Y2 UI: Yükleme sekmesi + çeki üst bloğu (BÜYÜK)
Dosyalar: `index.html`; `scripts/audit-ui-sweep.js`; `scripts/verify-h-render.js`;
`tests/` (playwright)

1. Sevkiyat'a 3. alt sekme `data-shiptab="load"` **"Yükleme"** (1376-1378 bölgesi +
   `switchShippingTab`). `renderShipping` Promise.all'ına `delivery_notes` (+carriers,
   company_settings) eklenir.
2. Sekme içeriği (TÜM projeler varsayılan — diğer sekmelerin aksine proje seçicisine
   bağlı DEĞİL; "istenilirse projeye göre filtreleme" dropdown'u):
   - **Bekleyen paketler**: SHIPPED olmayan tüm paketler; durum pili + proje + depo;
     CLOSED+bağsız olanlarda checkbox → "Araca Yükle (N)" butonu.
   - **Araç/İrsaliye kartları**: DRAFT (+ SHIPPED son 30 gün?) irsaliyeler: araç bilgileri,
     bağlı paketler, "Paket Yükle" (dnPkgLoadModal yeniden kullanım), "Sevk Et" (dnShip),
     "Çeki Listesi" (dnWeighList), "Packing List" (dnPackingList), "Yazdır" (dnPrint).
   - "Araca Yükle" modalı: mevcut DRAFT irsaliye seç VEYA yeni oluştur (alıcı
     [sup-datalist+autofill], nakliyeci [YENİ carrier-datalist + ensureCarrier], adres/
     il/ilçe, araç alanları, teslim koşulu, menşei) → dnCreate çekirdeği ortaklanır →
     seçili paketler dnPkgLoad ile bağlanır. Zincir: her paketin KENDİ projesinin
     siparişine `orderShippingRaise('yuklendi')` (çapraz-proje yükleme serbest — not:
     dnPkgSectionHTML'in aynı-proje filtresi Depo tarafında değişmez).
   - Firma Ayarları: sekme başlığında "Firma Ayarları" butonu → modal (ad/adres/tel/
     e-posta/vergi/logo yükle) → PUT company-settings. openSuppliersModal benzeri
     `openCarriersModal` da buradan.
3. Çeki/Packing üst bloğu: `dnWeighList` topblok — Gönderen/Consignor =
   company_settings (ad+adres, logo varsa antet); Teslim Koşulları = n.delivery_terms;
   Menşei/Origin = n.origin_country. `dnPackingList` + `dnPrint` başlığına da firma adı/
   logo. İrsaliye formuna (1633-1671) teslim koşulu + menşei inputları + carrier
   datalist'i.
4. Kurallar: emoji yasak (`ico()`), h`` disiplini, `btnBusy`, KPI/pil dili.
5. Test: audit-ui-sweep shipping alt-sekme listesine 'load' (elle liste!);
   verify-h-render'a yeni render fonksiyonları + shim GLOBAL'leri (deliveryNotes,
   carriers, companySettings — DERS: 3 kez tekrarladı); playwright sekme turu +
   yükleme akışı; e2e çapraz-proje zincir senaryosu.

## PAKET F — Kapanış: rapor + Y3 notu + smoke (KÜÇÜK)
1. `01 - Belgeler\13-tur-rapor-2026-07-17\rapor.html` (veya yeni 15-tur-rapor klasörü):
   "Y1-Y3/T1-T2 cevapların uygulandı" bölümü + hızlı test tarifi (paket kapat→depoda
   gör→Yükleme'den araca yükle→çeki listesi başlığı→QR'ı girişsiz aç→fire plakayı
   depoya gir). Y3 notu: fatura muhasebe programında — modül bilinçli YOK.
2. Tünel: yeniden başlat + `curl <url>/api/config` 200 KONTROLÜ + rapora güncel link
   (DERS: bayat link verilmez).
3. `shipping` yetkisi dağıtımı kullanıcıda (bilinçli — tekrar sorulmaz). Bellek + push.

## SONUÇ (2026-07-20 — TAMAMLANDI)
5 paketin tamamı kodlandı, her paket ayrı commit + doğrulama kapısı 14/14:
- **A `a523a50`** — T1: ?paket= girişsiz (public tek-paket ucu, GET-only permitAll)
- **B `b0c4572`** — T2: fire plaka plan sonucundan tek tuşla depoya (mrpSheetOffcuts)
- **C `442c6e4`** — Y1: paket depo hareketleri (reconcile; kapat=GİRİŞ, devir=ÇIKIŞ)
- **D `71a1a14`** — Y2 backend: company_settings + carriers + irsaliye teslim/menşei
- **E `cf9346b`** — Y2 UI: Yükleme sekmesi + çeki Gönderen/teslim/menşei + modallar

DB yedekleri: 2026-07-20_1737 (C öncesi), _1758 (D öncesi). Y3: kod yok (rapora not).
Yeni bekçiler: verify-mip guillotine/offcut ×6, e2e 15.TUR Y1 zinciri ×18,
verify-authz public uç ×4 + kartotek/ayar ×4, verify-h-render shipRenderLoad ×4.
Ders: e2e dbInsert REDDEDİLİNCE BOŞ DİZİ döner (truthy!) — redd assertion'ı
`.length===0 && _lastApiError` ile yazılır.

## Doğrulama (her paket sonunda)
1. `MSYS_NO_PATHCONV=1 taskkill /F /IM java.exe` → `./mvnw compile` → run (restart ritüeli)
2. `node scripts/verify-all.js` → EXIT=$? + BAŞARISIZ grep (tail YOK)
3. Şema paketlerinde (C, D): önce `cmd /c C:\erp-backup\erp-backup.cmd`, sonra
   `pg_dump -U postgres --schema-only --no-owner --no-privileges uretim_takip > db/schema.sql`
4. Paket-özel bekçiler: A verify-authz+playwright · B verify-mip · C e2e+audit-schema ·
   D audit-schema+verify-authz+canlı smoke · E audit-ui-sweep+verify-h-render+playwright
5. Ayrı commit; kapı geçmeden "bitti" denmez.
