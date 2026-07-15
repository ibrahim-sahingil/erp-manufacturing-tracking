# 12. Tur — Arkadaş İstekleri Uygulama Planı

## Bağlam
Arkadaşın 15 Temmuz sesli incelemesi (11 dk; döküm: `Desktop\ERP proje\02 - Test ve Istekler\12. test ve değişiklikler\15 Tem 18.15_dokum.srt`) 15 istek/hata içeriyor. Tasarım geçişi (tasarim-2026) tamamlandı; bu tur işlevsel. Üç keşif ajanı kod zemininin tamamını dosya:satır düzeyinde çıkardı; kritik içgörüler: BOM çarpanı hiç yok, mükerrer satın alma kaleminin kökü publish değil "Ürün Ağacından Aktar" butonu, MİP "güncellenmiyor" algısının kökü karar-bekleyen satırlarda rezerve bilgisinin görünmemesi, plaka (stock_plan) zinciri ne MİP'e ne iş emri kilidine bağlı.

**Kullanıcı kararları (AskUserQuestion):** m4 → hafif çözüm (taslak modu YOK); m1 → API düzeyinde de filtrele; m11 → openPurImport tamamen kaldır; m14 → iş emri bağı olmayan parçada QR serbest.

**Çalışma düzeni:** `tasarim-2026` dalında devam (kullanıcı merge kararı verene dek) ya da master'a merge sonrası — uygulama başında kullanıcıya tek satırla sorulur. Her paket: `./mvnw compile` → `node scripts/verify-all.js` (14 bekçi) → ayrı commit. Şema değişen pakette: elle SQL + `pg_dump -U postgres --schema-only --no-owner --no-privileges uretim_takip > db/schema.sql` + öncesinde `cmd /c C:\erp-backup\erp-backup.cmd` yedeği. Sunucu restart ritüeli: `taskkill /F /IM java.exe` → compile → run (CLAUDE.md tuzağı).

## Sıra: A → B → C → D → E → F
B (çarpan) C'nin ön koşulu (MİP hesabı çarpanla değişiyor); D, C'nin MRP bölgesinden sonra; F en büyük, bağımsız — arkadaş önceliği isterse öne alınabilir.

---

## PAKET A — Küçük düzeltmeler (m3, m5, m12, m13, m4-hafif) — KÜÇÜK
Dosyalar: `index.html`; `workorder/WorkOrderService.java`
1. **m3:** `renderFormDims` (index.html:13529) grid → `repeat(N,minmax(90px,1fr))` + input `min-width:0` + dar bağlamda `INP('8px 8px','13px')`; QR adet kutusu (CSS ~394) kontrol.
2. **m5:** `pbomeRemoveOp` (15252) işlem silince bölümü kalan SON işlemin tanımından yeniden türetir (pbomeOpDeptId mantığı, 14916 okuma-yanı); işlem kalmadıysa dept_id'ye dokunma (elle atamayı ezme).
3. **m12:** `wo-workspace` (5893) onchange → `wo-user` `wsMembers` (6882-6895, user_name İSİM eşleşmesi) ile daraltılır; üyesiz alanda tüm liste + uyarı; revize modalı (`wor-*`, 6383) aynı.
4. **m13:** ortak `dateRangeOk(start,end)` helper → `saveWorkOrder` (6122, kontrol yok), `submitWoRevise` (6410), `savePDate` (5396) ve `submitRevise` (5475) ortaklaşır; backend `WorkOrderService` create/update start>end → BusinessException.
5. **m4-hafif:** pbome + bom başlığına "otomatik kaydedildi · HH:MM" göstergesi (kayıt yapan fonksiyonlardan güncellenir). Şablon ağacın tamamını silme ZATEN VAR (deleteCurrentProduct 13421) — buton görünürlüğü yetkiye bağlanır (developer + users yetkisi vb. mevcut desen).

Test: e2e'ye dateRangeOk saf testi; playwright ters-tarih; verify-all.

## PAKET B — Ürün adedi çarpanı (m2) — ORTA
Dosyalar: `db/schema.sql`; `projectbom/{ProjectBom,ProjectBomService}.java` + dto; `index.html`
1. Şema: `ALTER TABLE project_bom ADD COLUMN product_qty integer NOT NULL DEFAULT 1;` (mevcut bağlar 1 → davranış değişmez) + dump. Şablon `bom_products`'a adet KONMAZ (adet sipariş bilgisi, şablon tanım).
2. Backend: entity + Request/Response (project_bom TEK DTO — doğrulandı; update "boş geleni koru" deseni product_qty için doğrulanır).
3. UI: bağlantı oluşturma formunda makine satırı başına adet girişi + pbom kartı/pbome başlığında "× N" rozeti; adet değişince "Yeniden Yayınla gerekli" rozeti.
4. `pbomPublishParts` (15495+): hedef = `g.qty × pb.product_qty`; sibling toplamı `otherProdQty` (15535) da `sib.product_qty` ile çarpılır (B1 ping-pong kuralı); Math.ceil (E2) korunur.
5. MİP: `renderMip` pbParts satırlarına `p._pqty` enjeksiyonu (7728-7732) + `mipQtyOf` (7370) `× (_pqty||1)` — mipGroupParts/mipCalcRow imzaları değişmez.

Test: audit-schema; verify-mip çarpan senaryosu; e2e publish hedef adet; verify-all.

## PAKET C — MİP senkron zinciri (m6, m9, m10, m15) + m11 — ORTA-BÜYÜK
Dosyalar: yalnız `index.html` (şema/backend yok)
1. **m6/m10-a:** karar-bekleyen satırlara (mipNodeRow 7776+; pendCalcByKey 7764 zaten hesaplı) "rezerve N · depoda N · gelen N" rozetleri — "20 kalıyor" algısının kökü bu görünmezlik.
2. **m6/m10-b:** depo rezervasyon onay/ret handler'ları sonunda MİP açık + aynı proje seçiliyse `renderMip()`; MİP sekmesine girişte taze veri.
3. **m6-c:** yeni `mipFromStockHTML(rows)` — "Stoktan Tedarik Edilenler" bölümü (parça, karşılanan miktar, kaynak depo; tamamı karşılanan DONE "kapandı" solukluğu) Satın Alınacaklar altına.
4. **m9:** yeni saf `mipPlanInfoOf(g, purchaseItems)` — pbpIds→kalem→stock_plan_id→plan kalemi {ad,status}; POOL satır rozetinde "planlandı → 1350×5000×3 · Sipariş verildi/Depoda"; Satın Alma'da plan kaleminin kartında bağlı kaynak parça listesi (stock_plan_id=plan.id).
5. **m15:** `woMissingMaterialsCore` (3260-3285) — kalemin stock_plan_id'si varsa etkin durum PLAN kaleminin durumu (IN_WAREHOUSE/IN_STOCK → arrived). TUZAK: plan kalemi 'ORTAK (MRP)' projesinde olabilir → planById sözlüğü proje filtresi ÖNCESİ tam listeden kurulur. Bağlılara otomatik durum YAZILMAZ (çifte muhasebe).
6. **m11:** `openPurImport` + yardımcıları TAMAMEN kaldırılır; "Ürün Ağacından Aktar" butonu → "MİP'te Planla" yönlendirmesi (switchTab('mip') + proje seçimi; yayınlanmamışsa 'önce yayınla' uyarısı). Elle kalem ekleme formu KALIR. audit-refs handler kalıntısı 0.

Test: verify-mip'e mipPlanInfoOf + stoktan-tedarik; e2e'ye woMissingMaterialsCore+plan zinciri (plan depoda→kilit açık / ORDERED→kapalı); verify-h-render yeni bölüm; verify-all.

## PAKET D — Havuz ölçüleri form-bazlı (m7) + optimum boy (m8) — ORTA
Dosyalar: yalnız `index.html`
1. **m7:** `mrpItemDims` (8798) → 5 DIM_FIELDS tümü `custom_* || resolved_*` önceliğiyle + material_form; havuz satırı alanları forma göre: SAC=En/Boy(/Kalınlık), PROFIL/MIL/BORU=Kesim Boyu=length_mm (bugün yanlışlıkla height); `mrpSelectedItems` (9025) + `renderMrpParams` (8968) form-farkında etiketler. renderMrp esc disiplini korunur (h``'ye dönüştürme YOK — CLAUDE.md).
2. **m8:** yeni saf `mrpOptimumProfil(lengths, adayBoylar, firePct)` — her aday boy için `mrpKonsolideProfil` (9215, değişmez) koşup toplam fire en azını seçer; adaylar stock_sheets kind='PROFIL' length_mm'leri; `mrp-prf-sel`'e "— Optimum (katalogdan) —" seçeneği; sonuçta kıyas satırı ("6000: 14 profil %8 · 12000: 7 profil %5"). mrpApprove değişmez.

Test: mrpOptimumProfil saf senaryolar (tek aday/iki aday/sığmayan) + mrpItemDims override testi; shot-mip görüntü; verify-all.

## PAKET E — QR başlatma kilidi (m14) — KÜÇÜK-ORTA
Dosyalar: `part/PartLogService.java`; `index.html`
Karar: parça iş emrine bağlı DEĞİLSE serbest (eski akış durmaz); bağlıysa en az bir iş emri `inprogress` olmadan kayıt engellenir.
1. **Backend ŞART** (QR halka açık): `PartLogService.create` (75-120) guard — partId→work_order_parts→work_orders; bağ var + hiçbiri 'inprogress' değil → BusinessException("WO_NOT_STARTED"). Durumlar lowercase.
2. Frontend: `submitScan` (4425) erken kontrol + toast; `renderScan`'de uyarı şeridi + Kaydet disable.

Test: e2e guard senaryosu (planned→4xx, inprogress→geçer, bağsız→geçer); canlı QR smoke; verify-all.

## PAKET F — Teklif / Onaylanan Sipariş ayrımı (m1) — BÜYÜK
Dosyalar: `db/schema.sql`; `order/{Order,OrderService}.java` + `dto/OrderRequest.java`; YENİ `orderdocument/` modülü; `config/SecurityConfig.java`; `index.html`
Kararlar: ayrı tablo YOK — `orders.status` genişler (teklif onaylanınca aynı kayıt kalır, project_name bağları kopmaz); durum kanonu LOWERCASE + CHECK; dosyalar `order_documents` (bom_documents şablonu); yetki `orders_quotes` + **API düzeyinde filtre** (kullanıcı kararı).
1. Şema (yedek + migration): `UPDATE orders SET status=lower(status); ALTER ... DEFAULT 'active'; CHECK (status IN ('quote','quote_lost','active','pending','completed','cancelled')); ADD COLUMN approved_at timestamp, approval_note text;` + `order_documents` tablosu (order_id FK CASCADE, category CHECK('QUOTE','ORDER'), filename/content_type/size_bytes/data bytea/uploaded_by) + dump.
2. Backend ÜÇLÜ kural: CHECK + OrderRequest @Pattern + OrderService durum whitelist (quote→active geçişinde approved_at=now + approval_note zorunlu); **liste filtresi:** JWT yetkilerinde `orders_quotes`/developer yoksa GET /api/orders quote/quote_lost kayıtlarını DÖNDÜRMEZ (OrderService/Controller'da authority kontrolü). `orderdocument/` modülü BomDocumentController (39-110) birebir uyarlanır; SecurityConfig writeRule + order-documents GET'i de orders_quotes'a bağlanır (teklif dosyası gizliliği).
3. Adapter: TABLE_ENDPOINTS `order_documents:'order-documents'`; orders alanları spread ile geçer (approved_by uuid↔isim çevirisi 2426/2441 mevcut).
4. UI: Siparişler'de iki alt-sekme (planning-tab deseni) "Teklifler" / "Onaylanan Siparişler"; statusMap (5140) + form durum seçenekleri quote'lu; teklif kartı: durum (onaylandı/beklemede/kabul edilmedi = active'e geçiş/quote/quote_lost), süreç notları, dosya yükle/indir/sil (docsUploadDo 12855 deseni), "Onayla → Siparişe Çevir" (not modalı + btnBusy); alt-sekme görünürlüğü orders_quotes'a bağlı (au-perms grid 1595 + switchTab 7305 desenleri); quote'lar dashboard/planlama/MİP sayımlarına girmez (kontrol edilir — proje adı bağları quote'ken de kurulabilir olduğundan publish/planlama akışları quote projeleri listelememeli).
5. Tasarım dili: tasarim-2026 konsept kuralları (phead/chipk/st pilleri, emoji yasak, h`` disiplini); parity MAP'e teklif alt-sekmesi.

Test: verify-authz'a orders_quotes senaryoları (yetkisiz LIST'te quote görünmez + order-documents 403); e2e quote→active + approved_at; canlı smoke (@Pattern yalnız HTTP'de — POOL dersi); audit-schema; verify-all.

## Kapsam dışı / ertelenen
- m4 tam taslak-onay modu (kullanıcı kararıyla hafif çözüm seçildi)
- Plan kalemi depoya girince bağlılara otomatik IN_WAREHOUSE yazımı (kilit zinciri ihtiyacı çözüyor; çifte muhasebe riski)
- İrsaliye yıl filtresi / personel bölüm filtresi (hacim artınca)

## Doğrulama (her paket sonunda)
1. `./mvnw compile` (restart ritüeliyle: taskkill java → compile → run)
2. `node scripts/verify-all.js` — 14 bekçi (parite + 4 tema taraması + e2e + authz + playwright)
3. Pakete eklenen yeni bekçi senaryoları (verify-mip / e2e / verify-authz / verify-h-render)
4. Canlı smoke: değişen akış tarayıcıda uçtan uca (özellikle F durum geçişi ve E QR — validation yalnız HTTP katmanında)
5. Ekran görüntüsü kullanıcıya (shot-*.js deseni); arkadaş smoke'u için tünel: adres değişmişse yenile
6. Şema değişen paketlerde önce `cmd /c C:\erp-backup\erp-backup.cmd` DB yedeği

## Tur sonu
- Tüm paketler bitince: docs/plan-arkadas-istekleri-12-tur.md olarak bu plan depoya kopyalanır (3. tur deseni), hafıza güncellenir, arkadaş smoke turu için tünel + özet rapor.
