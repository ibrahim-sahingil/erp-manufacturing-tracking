# 16. Tur — Arkadaş İstekleri Uygulama Planı (klasör: "14.test ve değişiklikler")

## Bağlam

Kaynak: `Desktop\ERP proje\02 - Test ve Istekler\14.test ve değişiklikler\` (istekler.txt
+ 100-103.jpeg). Arkadaş 15. turu inceledi; 3 yeni madde. Kullanıcı uyarısı: istekler
İNCE HATAYA MÜSAİT — dikkatli, paket paket, her pakette kapı.

- **M1 (img 100):** Satın Alma düzensiz → (a) proje kartlı GİRİŞ dashboard'u (ana
  dashboard gibi yeniden→eskiye; girmeden "verilmiş/verilecek/beklenen/planlanan"
  sayıları), tüm kalemlere ayrı bakış kalsın; (b) her siparişe OTOMATİK MÜKERRERSİZ
  KOD + koddan anında arama; (c) "her bölümün kendine özgü dashboard'u".
- **M2 (img 101 vs 102):** Şablon ağacı düzenleyicisinde (bom_parts) Bölüm alanı yok →
  her projeye bağlayışta bölümler elle yeniden atanıyor. Şablona bölüm eklensin ve
  projeye bağlanırken taşınsın.
- **M3 (img 103):** (1) Ürün Ağacı'nda proje bağlama AYRI kısım olsun; (2) teknik
  resimler PROJEYE özgü depolansın (kategoriler: Sipariş / İmalat / Diğer), parçalara
  bağlansın, DİĞER EKRANLARDA parça üstünden indirilebilsin (satın almacı sipariş
  resmini, imalatçı/iş emri yapan imalat resmini görsün); (3) Depo'ya ayrı dashboard.

**Kullanıcı kararları (AskUserQuestion, 2026-07-21):**
- K1: Otomatik kod = TOPLU SİPARİŞ GRUPLARINA (SIP-2026-0001; PKT/IRS deseni);
  arama grup kodu + kalem kodu/adında.
- K2: Dashboard kapsamı bu turda = Satın Alma + Depo (diğer modüller sonra).
- K3: Şablon bölümü projeye taşınırken ad eşleşmezse bölüm OTOMATİK OLUŞTURULUR
  (13. tur b90806c "asla oluşturma" kararının BİLİNÇLİ revizyonu — yalnız bu akışta;
  14. tur S7 önerisi "asla oluşturmaz" olarak KALIR, çakışmaz: S7 yalnız BOŞ bölüme
  öneri atar, attach-copy dept_id'yi zaten doldurmuş olur).
- K4: Teknik resimler İKİSİ YAN YANA: ürün(şablon) bazlı mevcut sistem kalır,
  PROJE bazlı yeni katman eklenir.
- Kullanıcı: "anlaşılmayan olursa bana yaz, arkadaşa iletirim."

## Kod zemini (keşif, 2026-07-21 — üç ajan raporundan)

- **Satın Alma:** `renderPurchasing` 9925 (veri yükleyici) → `renderPurchaseList` 11109
  (chip deseni 11121-38: `PUR_STATUS` sayaçları + Tahmini toplam per-currency);
  alt sekmeler `switchPurchasingTab` 9955 (`_purActivateTab`, section id `pur-tab-<t>`);
  `renderPurchaseOrders` 11663 (#po-list); `PO_STATUS` 11553. `purUpdateSub` 11099.
- **purchase_orders'ta KOD ALANI YOK** (entity 52-77; schema 492-504). Üretici deseni:
  `DeliveryNoteService.nextNoteNo` 183-194 + repo count/exists; UNIQUE DB'de
  (delivery_notes_note_no_key 841). PurchaseOrderService.create 58-94 (builder'a
  `.code(nextOrderNo())`).
- **Proje kartı deseni:** `renderProjectCards` 3998+ (.project-card CSS 264-276);
  KPI: `dnRenderKpis` 12815 (.stat-card/.stat-label/.stat-value .un/.stat-sub .delta;
  BÜYÜK RAKAM MÜREKKEP, renk yalnız .delta/.st pili; boş kıyasta `.delta.nd`).
- **Arama deseni:** `#order-search` 967 + renderOrders 5346-51 (`toLowerCase().includes`).
- **Depo:** alt sekmeler 1543-47 (`data-whtab`: view/receiving/additem/manage/delivery;
  `_whActivateTab` 12019); `renderWarehouse` 12062 (kaynak veriler); `whStockRows`
  14025; `whvProjectHTML` 12364 (proje akordeonu); `whvWarehouseHTML` 12429;
  `#wh-summary` 1559 mevcut çipler. DİKKAT: `whAllowedSubTabs` (11. tur F1 dersi —
  yeni alt sekme eklerken izin listesi de güncellenir!).
- **bom_parts'ta BÖLÜM KOLONU YOK** (schema 94-117). `project_bom_parts.dept_id` VAR
  (FK departments SET NULL). `departments` PROJE KAPSAMLI (order_id FK; FIELD_XLATE
  project↔order_id 2504-21). `bom_operations.department_name` = AD-string taşıma
  emsali (projeye uygulanırken adla çözülür). **Şablona UUID değil AD yazılır**
  (şablon proje-bağımsız; UUID çapraz-proje sızıntısı yaratır).
- Şablon editörü: `renderBomList` 15879 (satır aksiyonları 15925-31: İşlem/Düzenle/
  Kopyala/Miktar/Sil), `editBomPart` 16117 (modal — bölüm alanı buraya),
  `saveBomPartEdit` 16169, `addBomPart` 15708 (create payload 15725).
- Kopyalama: `ProjectBomService.autoPopulateBomParts` 241-294 — `.deptId(null)` :266
  (b90806c bilinçli boşaltması; K3 ile burada ad→bölüm çözümü/oluşturma yapılacak).
  Publish tarafı HAZIR: pbomPublishParts 17691-98 (fill-only backfill) + 17717
  (create'te department_id) — attach dept_id doldurursa gerisi akar.
- S7 önerisi: `oneriDeptId` 16992-17003 (yalnız BOŞ parçaya, asla oluşturmaz) — kalır.
- BomPart DTO'ları: Request (create, düz) + UpdateRequest (partial; yalnız parentId
  presence-takipli) + Response + service iki dal — CREATE≠UPDATE tuzağı beşli güncelleme.
- **Teknik resimler:** `bom_documents` (product_id, category URETIM/ARGE, data bytea)
  + `bom_document_parts` join (document_id, bom_part_id, CASCADE). Controller multipart
  POST + `/{id}/download` (RFC-5987). Frontend: docsUpload 14850 (raw fetch multipart),
  docsDownload 14901 (auth'lu blob), docsPartRowsHTML 14827 (checkbox ağacı),
  `_docPartIds` yeşil-ad ipucu (renderBomList 15913, renderBomTreeSvg 16252).
- **PROJE = STRING** (projects tablosu yok; orders.project_name UNIQUE). Yeni tablo
  project_name varchar(100) ile anahtarlanır (FK yok).
- En yakın şablon: `orderdocument/` modülü (12. tur) — MAX_BYTES 50MB guard'lı servis,
  meta JPQL (data'sız), multipart controller. writeRule bom-documents: "bom","docs".
- Proje bağlama bölümü: view-bom 1903-1929 (kendi başlıklı bağımsız div; pbom*
  fonksiyonları 16342-16517) — alt sekmeye taşınmaya hazır, mantık değişmez.
- Parça-üstü indirme yüzeyleri: woRenderPartsGrid 6472 (nodeRow 6549-64; parts.id+code),
  renderPurchaseList satır aksiyonları 11213-20 (project_bom_part_id bazen null → kod
  eşleşmesi), pbomeRenderList satırı (pbp.id). Üretim parçası→pbp eşleşmesi KOD ile
  (pbpByCode deseni, _shipRows 6653).

## Sıra: A → B → C → D → E → F → G → H → I
Şema değişen paketler: **A, D, F** (her birinden önce `cmd /c C:\erp-backup\erp-backup.cmd`
+ sonrasında `pg_dump -U postgres --schema-only --no-owner --no-privileges uretim_takip > db/schema.sql`).
psql yolu: `"/c/Program Files/PostgreSQL/18/bin/psql.exe"`. Her paket: taskkill java →
mvnw compile → run (Start-Process cmd '/c','.\mvnw.cmd spring-boot:run...') →
`node scripts/verify-all.js` ÖN PLANDA timeout 600000 (arka planda 2 kez sebepsiz
kill edildi — 15. tur dersi) → EXIT + BAŞARISIZ grep → ayrı commit.

---

## PAKET A — M1b: Toplu sipariş grubuna SIP kodu + arama (KÜÇÜK-ORTA, ŞEMA)
Dosyalar: db/schema.sql; purchasing/{PurchaseOrder,PurchaseOrderService,
PurchaseOrderRepository}.java + PurchaseOrderResponse; index.html; scripts/e2e-test.js

1. Şema: `ALTER TABLE purchase_orders ADD COLUMN code varchar(30);` + mevcut gruplara
   backfill (created_at sırasıyla `SIP-<yıl>-<sıra>` — yıl created_at'ten) + sonra
   `ALTER TABLE ... ALTER COLUMN code SET NOT NULL; ADD CONSTRAINT purchase_orders_code_key UNIQUE (code);`
2. Backend: entity `code`; `nextOrderNo()` (nextNoteNo klonu, "SIP-"); create'te
   `.code(nextOrderNo())`; Repository `countByCodeStartingWith`+`existsByCode`;
   Response'a code. Request/UpdateRequest'e KOD GİRİLMEZ (backend üretir — PKT deseni).
3. UI: grup kartı başlığına mono kod rozeti (`${o.code}` — pbom-card-title yanı);
   Toplu Sipariş sekmesi başına arama kutusu (#po-search, order-search deseni):
   kod + grup adı + üye kalem adı/kodu arar. Kalemler sekmesindeki listeye de arama
   kutusu (#pur-search): kalem adı/kodu + bağlı grup kodu arar (M1 "binlerce sipariş
   arasından koddan bul").
4. Test: e2e — grup oluştur → code SIP-\d{4}-\d{4} + ikinci grupta sıra artar;
   audit-schema; canlı smoke.

## PAKET B — M1a: Satın Alma proje kartlı giriş dashboard'u (ORTA, yalnız frontend)
Dosyalar: index.html (+ verify-h-render senaryosu)

1. Yeni alt sekme `data-purtab="projects"` **"Projeler"** — VARSAYILAN AÇILIŞ
   (switchTab('purchasing') → renderPurchasing → projects aktifse renderPurProjects).
   Mevcut "Kalemler"/"Toplu Sipariş & Teklif" kalır ("tüm parçalara ayrıca bakalım").
2. Üst KPI şeridi (.stat-card, GERÇEK veri): Açık Kalem (CANCELLED değil, IN_WAREHOUSE
   değil) · Teklif Bekleyen Grup (DRAFT) · Bu Ay Sipariş (ORDERED, ordered_at bu ay)
   · Depoya Giren (bu ay IN_WAREHOUSE). Kıyas satırı yoksa `.delta.nd`.
3. Proje kartları YENİDEN→ESKİYE (projenin İLK kalem created_at'i tersten; pinned
   önceliği YOK — arkadaş "yeniden eskiye" dedi): kart = proje adı + PUR_STATUS
   sayaç çipleri (Planlandı/Sipariş Verildi/Teslim Alındı/Depoda — "girmeden
   verilmiş/verilecek/beklenen/planlanan") + grup sayısı + tahmini toplam (para
   birimli) + son hareket tarihi. Tık → Kalemler sekmesi o proje filtreli açılır
   (`#pur-project-sel` set + _purActivateTab('items')).
4. h`` disiplini + emoji yasak + verify-h-render'a renderPurProjects XSS senaryosu
   (proje adı EVIL) + shim globalleri (purchaseOrders vb.).

## PAKET C — M1c/M3-son: Depo "Özet" dashboard alt sekmesi (ORTA, yalnız frontend)
Dosyalar: index.html (+ verify-h-render)

1. Depo'ya yeni İLK alt sekme `data-whtab="dash"` **"Özet"** (+ `whAllowedSubTabs`
   İZİN LİSTESİNE ekle — 11. tur F1 dersi; varsayılan sekme davranışını bozma:
   mevcut kullanıcı akışları 'view' varsayılanıyla kalsın, Özet İLK BUTON ama
   aktif sekme yine 'view' açılır — karışıklık olmasın diye Özet'e tıklamayla girilir).
2. İçerik (hepsi mevcut hesaplardan): stat-card şeridi (Depo sayısı · Depodaki kalem
   · Mal kabul bekleyen · Bekleyen rezervasyon talebi · Depodaki paket [PACKAGE IN
   net]) + depo başına doluluk kartları (whStockRows gruplaması: kalem çeşidi, net
   toplam, paket sayısı) + proje bazlı bekleyen/depoda özeti (whvProjectHTML
   verisinin sayaç hali — akordeonun kendisi değil).
3. verify-h-render senaryosu + playwright sekme turu (otomatik .planning-tab taraması).

## PAKET D — M2: Şablon ağacına Bölüm (AD) + projeye taşıma (ORTA-BÜYÜK, ŞEMA)
Dosyalar: db/schema.sql; bom/{BomPart,BomPartService}.java + dto (Request +
UpdateRequest + Response); projectbom/ProjectBomService.java; index.html;
scripts/verify-opdef-cascade.js veya yeni bekçi senaryosu; e2e

1. Şema: `ALTER TABLE bom_parts ADD COLUMN department_name varchar(100);` (AD-string —
   bom_operations.department_name emsali; UUID DEĞİL: şablon proje-bağımsız).
2. Backend DTO beşlisi: entity + Request + UpdateRequest (null=dokunma; ""=temizle
   blankToNull) + Response + service create/update dalları.
3. Şablon UI: `editBomPart` modalına "Bölüm (ad)" text input (datalist: mevcut şablon
   department_name'leri + opdef department_name'leri); `addBomPart` formuna aynı alan;
   `renderBomList` satırında bölüm çipi (varsa). Kolay atama: satır aksiyonlarına
   küçük "Bölüm" düğmesi gerekmiyorsa modal yeterli (İNCE HATA riskini küçült).
4. **Taşıma (K3):** `ProjectBomService.autoPopulateBomParts` :266 —
   `department_name` doluysa: hedef projenin departments'ında ad eşleşmesi
   (tr-lowercase) → varsa deptId; YOKSA departments'a `{order_id: <projenin order'ı>,
   name}` OLUŞTUR (aynı attach içinde tek sefer — ada göre cache map) → deptId ata.
   DİKKAT: departments.order_id ister → ProjectBom'da project_name var, order'ı
   OrderRepository'den project_name UNIQUE ile bul; bulunamazsa dept oluşturma,
   null bırak (log). DepartmentRepository injection.
5. Publish tarafına DOKUNMA (fill-only backfill hazır).
6. Republish/yeniden bağlama: autoPopulate yalnız CREATE'te koşar — mevcut bağlara
   dokunmaz (bilinçli; plana not).
7. Test: e2e — şablon parça department_name'li → projeye bağla → pbp.dept_id dolu +
   departments'ta YENİ bölüm oluştu (ad eşleşmesizken) + İKİNCİ bağlamada mükerrer
   bölüm OLUŞMAZ; audit-schema; verify-h-render şablon çipi.

## PAKET E — M3.1: Proje Bağlama ayrı alt sekme (KÜÇÜK, yalnız frontend)
Dosyalar: index.html

1. view-bom başına .planning-tabs: `data-bomtab="edit"` "Ağaç Düzenleme" (mevcut
   1-4 bölümler) + `data-bomtab="attach"` "Proje Bağlama" (1903-1929 bloğu taşınır).
   `switchBomTab` (switchShippingTab klonu); initBom mevcut davranış: edit varsayılan.
2. Mantık değişmez (pbom* fonksiyonları aynen); yalnız yer değişimi + sekme geçişinde
   renderProjectBomList().
3. audit-ui-sweep .planning-tab'ı otomatik tarar; playwright turu.

## PAKET F — M3.2a: project_documents backend (ORTA, ŞEMA)
Dosyalar: db/schema.sql; YENİ projectdocument/ modülü (orderdocument klonu + part
join); config/SecurityConfig.java; index.html (TABLE_ENDPOINTS)

1. Şema:
```sql
CREATE TABLE project_documents (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  project_name varchar(100) NOT NULL,     -- proje = string (projects tablosu yok)
  category varchar(20) NOT NULL DEFAULT 'DIGER'
    CONSTRAINT project_documents_category_check CHECK (category IN ('SIPARIS','IMALAT','DIGER')),
  filename varchar(300) NOT NULL, content_type varchar(150), size_bytes bigint,
  data bytea NOT NULL, uploaded_by varchar(150),
  created_at timestamp DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE project_document_parts (
  document_id uuid NOT NULL REFERENCES project_documents(id) ON DELETE CASCADE,
  project_bom_part_id uuid NOT NULL REFERENCES project_bom_parts(id) ON DELETE CASCADE,
  PRIMARY KEY (document_id, project_bom_part_id)
);
CREATE INDEX idx_project_document_parts_part ON project_document_parts(project_bom_part_id);
```
2. Modül: OrderDocument klonu + BomDocument'ın @ElementCollection partIds deseni;
   MAX_BYTES 50MB guard (OrderDocumentService'ten); kategori ÜÇLÜ kural; meta JPQL
   data'sız; endpoints: GET ?project=<ad> (meta+part_ids), POST multipart (file,
   project_name, category, part_ids CSV, uploaded_by), GET /{id}/download (RFC-5987),
   PUT /{id} (kategori/part bağları), DELETE (bağlıysa DOC_LINKED guard — bom deseni).
   Part doğrulaması: bağlanan pbp'ler AYNI projenin pbom'larına ait olmalı
   (DOC_PART_PROJECT_MISMATCH).
3. SecurityConfig: `writeRule("/api/project-documents/**", "ROLE_DEVELOPER","bom","docs")`.
4. TABLE_ENDPOINTS += project_documents (meta işlemleri için; upload/download raw fetch).
5. Test: audit-schema; verify-authz (docs'suz kullanıcı yazamaz); canlı smoke
   (multipart POST + download + kategori reddi).

## PAKET G — M3.2b: Teknik Resimler sekmesine "Proje Resimleri" (BÜYÜK, frontend)
Dosyalar: index.html (+ verify-h-render)

1. Docs sekmesine .planning-tabs: "Ürün (Şablon) Resimleri" (mevcut içerik aynen) +
   **"Proje Resimleri"** (yeni): proje seçici (activeOrders — teklifler girmez) →
   üç kategori bölümü (Sipariş Resimleri / İmalat Resimleri / Diğer Resimler) +
   kategori başına Yükle butonu (docsUpload deseni: raw fetch multipart) + kart:
   dosya adı/boyut/yükleyen/tarih + İndir (auth'lu blob) + Parçalar (bağ düzenle) +
   Sil (önce bağları çöz).
2. Parça bağlama modalı: projenin pbp ağacı checkbox listesi (docsPartRowsHTML'in
   pbp uyarlaması — custom_code||resolved_code + ad; girinti level'dan).
3. `_projDocs` state + `projDocsLoad(proj)` (meta). Yeşil-ipucu: pbome satırında
   bağlı resmi olan parça adı yeşil (mevcut _docPartIds deseninin pbp versiyonu).
4. verify-h-render: yeni render'lara XSS senaryoları (dosya adı/proje adı EVIL) +
   shim globalleri.

## PAKET H — M3.2c: Parça üstü indirme — diğer ekranlar (ORTA, frontend)
Dosyalar: index.html (+ verify-h-render)

1. Saf yardımcı `projDocsByPartKey(docs, pbps)`: pbp.id → doc listesi; üretim
   parçası/satın alma kalemi için KOD eşleşmesi köprüsü (pbpByCode deseni).
2. Küçük ikon buton (ico('file'/'download')) + `partDocsModal(projeAdi, anahtar)`:
   parçanın bağlı PROJE resimleri listesi (kategori rozetli) + İndir.
   Yüzeyler (v1): woRenderPartsGrid nodeRow (imalatçı — İMALAT önde sıralı),
   renderPurchaseList satır aksiyonları (satın almacı — SİPARİŞ önde),
   pbomeRenderList satırı. Buton yalnız bağlı resmi olan parçada görünür
   (görsel gürültü yok).
3. Meta yükleme: ekran render'ında proje belli olduğunda lazily
   `projDocsEnsure(proj)` (cache map; meta hafif — data yok).
4. verify-h-render senaryoları; playwright smoke.

## PAKET I — Kapanış (KÜÇÜK)
1. YENİ rapor: `01 - Belgeler\16-tur-rapor-<tarih>\rapor.html` — 3 maddenin nasıl
   uygulandığı + hızlı test tarifleri + K3 kararının açık anlatımı ("bölüm artık
   şablondan taşınır ve gerekirse projede OLUŞUR — 13. turdaki 'hep manuel'
   kuralının bilinçli revizyonu") + arkadaşa notlar.
2. Tünel yenile + `curl <url>/api/config` 200 + linki rapora.
3. Bellek + push (tasarim-2026). `shipping`/`docs` yetki dağıtımı kullanıcıda.

## Arkadaşa iletilecek varsayılanlar (kullanıcı onayı yeterli; itiraz gelirse düzeltilir)
- V1: Satın Alma dashboard kartına tıklayınca KALEMLER görünümü o proje filtreli açılır.
- V2: SIP kodu YALNIZ toplu sipariş gruplarında; kalemlerin kendi malzeme kodu esas.
- V3: Depo Özet sekmesi bilgi amaçlı (aksiyon butonu yok — aksiyonlar mevcut sekmelerde).
- V4: Parça-üstü indirme v1'de PROJE resimlerinden (şablon resimleri Docs>Ürün'de kalır).
- V5: Şablon bölümü republish/yeniden-yayında MEVCUT bağlara dokunmaz (yalnız yeni
  bağlanan projelere taşınır); mevcut projelerde bölüm yine elle/S7.

## SONUÇ (2026-07-22 — TAMAMLANDI)
8 paketin tamamı kodlandı, her paket ayrı commit + doğrulama kapısı 14/14:
- **A `ec510a2`** — M1b: purchase_orders.code (SIP-yıl-sıra, 4 gruba backfill) + iki arama kutusu
- **B `a80f3d3`** — M1a: Satın Alma "Projeler" giriş dashboard'u (KPI + kartlar yeniden→eskiye)
- **C `48f480a`** — M1c: Depo "Özet" dashboard alt sekmesi (whAllowedSubTabs + spec 6 sekme)
- **D `e0ef14a`** — M2: bom_parts.department_name + autoPopulate ad çözümü/K3 oluşturma
- **E `5a7ac91`** — M3.1: Proje Bağlama ayrı alt sekme (taşıma, mantık aynı)
- **F `bd09bca`** — M3.2a: project_documents backend (3 kategori + pbp bağları + 50MB)
- **G `d86423d`** — M3.2b: Docs > "Proje Resimleri" sekmesi (pdocs* ailesi)
- **H `3ff809d`** — M3.2c: parça üstü indirme (wo/satın alma/pbome + partDocsOpen)

DB yedekleri: 2026-07-21_2325 (A), _2350 (D), 2026-07-22_0013 (F).
Dersler: harness EP haritasında olmayan tablo dbGet'te sessizce [] döner
(departments eklendi); harness dbGet HAM API döner (FIELD_XLATE yok — 'project'
değil 'order_id'); onclick'e kullanıcı verisi gömme yerine kayıt defteri +
indeks deseni (_purProjCards, _partDocsReg).

## Doğrulama (her paket sonunda)
1. taskkill java → mvnw compile → detached run → sağlık 200
2. `node scripts/verify-all.js` ÖN PLANDA (timeout 600000) → EXIT + BAŞARISIZ grep
3. Paket-özel: A e2e+audit-schema+smoke · B/C/G/H verify-h-render+playwright ·
   D e2e (bölüm taşıma zinciri)+audit-schema · E playwright · F verify-authz+smoke
4. Şema paketlerinde (A, D, F) önce DB yedeği, sonra schema dump
5. Ayrı commit; kapı geçmeden "bitti" denmez.
