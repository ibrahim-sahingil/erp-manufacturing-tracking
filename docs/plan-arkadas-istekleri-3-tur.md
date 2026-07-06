# Arkadaş İstekleri 3. Tur — Uygulama Planı (2026-07-07)

Kaynak: `Desktop/ERP test/3. test ve değişiklik adımı/` (1.txt + 4 görsel + plaka-mrp.html).
Sıra, bağımlılık ve risk gözetilerek kuruldu. Her adım: derleme + Playwright E2E + ayrı commit/push.

## Durum notları
- **Madde 3 (QR mal kabul): ZATEN YAPILDI** (commit 894f62a, 07.07 gecesi) — arkadaş eski
  sürümü test etmiş. Kendisine yeni sürümde göstermek yeterli; ek iş yok.
- **plaka-mrp.html analizi**: localStorage tabanlı bağımsız SPA. Modüller: (a) PLAKA:
  2D guillotine nesting — Best Short Side Fit + rastgele restart (`packBestGuillotine`/
  `runGuillotine`), gap desteği, döndürme; canvas çizim; CSV rapor. (b) PROFİL/MİL: 1D
  kesim konsolidasyonu (`profilHesapla`/`konsolideProfil`). Algoritmalar temiz, index.html'e
  port edilebilir. Girdi: parça ölçüleri (bizde artık BOM'da width/height/thickness_mm var).

## PAKET A — Hızlı düzeltmeler (yarım gün)
1. **(#6) .xls desteği**: BomImportService'te XSSFWorkbook yerine POI `WorkbookFactory.create`
   (hem .xls hem .xlsx); uzantı kontrolüne .xls ekle. Frontend accept'e .xls ekle.
2. **(#2b) "KAZANAN" → "ONAYLANAN"**: teklif kartı etiketi (görsel: "Kazanan değil onaylanan yazsın").
3. **(#9) Dashboard "planlama bekliyor" ayrıştırma**: parçanın kodu bir purchase_item
   (project_bom_part üzerinden ya da kod eşleşmesi) ile eşleşiyor ve durumu ORDERED/RECEIVED/
   IN_WAREHOUSE ise şerit "🛒 malzemesi hazır/siparişte — üretim planlaması bekleniyor" desin;
   hiç satın alma bağı yoksa eski metin kalsın. (isPartAwaitingPlanning etrafında etiket
   fonksiyonu; parça sayısını iki gruba böl.)

## PAKET B — Satın alma maliyetleri (#2 + görseller) (küçük-orta)
- Kalem kartında fiyat alanı zaten var (unit_price) — ayrıca kalem düzenlemede hızlı fiyat girişi.
- Toplu sipariş grubunda kaleme TEK TEK fiyat yazılabilsin (görsel: "fiyat istenilirse tek tek").
- Proje bazında **malzeme bedeli**: TRY/EUR/USD ayrı toplamlar; Satın Alma özet çubuğunda proje
  seçiliyken zaten var → ek olarak Siparişler sekmesindeki proje kartına "💰 Malzeme bedeli"
  satırı (satın alma kalemlerinden, iptaller hariç).

## PAKET C — Çoklu ürün ağacı bağlama UX (#1, görsel 100) (orta)
- Proje↔BOM bağlama ekranında aynı projeye BİRDEN FAZLA ürün tek seferde bağlanabilsin
  (çoklu seçim) ve proje BOM listesi PROJE altında gruplansın (kartlar proje başlığı altında).
- Veri modeli zaten uygun (project_bom satırları); yalnız UI/UX işi.

## PAKET D — Mal Kabul sekmesi (#4) (orta)
- Yeni alt görünüm (Satın Alma içinde 3. alt-sekme "📥 Mal Kabul" ÖNERİLİR; ayrı nav sekmesi şart
  değil — kullanıcıya soruldu mu? Gerekirse nav'a alınır).
- Proje kartları → tıklayınca o projenin sipariş verilen kalemleri: gelen (RECEIVED/IN_WAREHOUSE)
  ✅, bekleyen (ORDERED) ⏳; satırdan "✅ Geldi (depoya al)" = whTransfer; QR fişi butonu da burada.

## PAKET E — Depo görünümü yeniden tasarımı (#5) (orta-büyük)
- Depo sekmesi "Görünüm" alt-sekmesi iki moda ayrılır:
  1. **Proje bazlı**: proje kartları → malzemelerin depo dağılımı; "🖨 PDF" detay çıktısı.
  2. **Depo bazlı**: depo seç → içindeki malzemeler proje gruplu + münferit (projesiz) ayrımı.
- Proje bilgisi kaynağı: movement → purchase_item_id → purchase_items.project_name (join
  frontend'te). Münferit = purchase_item bağı olmayan hareketler.
- "Depodaki ürünü projeye aktar": hareketin purchase_item bağı yoksa proje ataması için
  movements'a project_name snapshot kolonu GEREKEBİLİR → uygulamadan önce kullanıcıyla netleşt.

## PAKET F — Malzeme türü (#7) (orta-büyük; PAKET G'nin ÖN KOŞULU)
- bom_parts + project_bom_parts'a `material_kind` kolonu:
  TEDARIK / HAMMADDE / YARI_MAMUL / MAMUL / SARF (nullable).
- Ürün ağacı editörlerinde tür seçimi (ekleme + düzenleme modalı + Excel import kolonu).
- Yayınla/satın alma aktarımında yönlendirme: TEDARIK+HAMMADDE+SARF → satın alma kalemi;
  YARI_MAMUL+MAMUL → parts (üretim). (Bugünkü davranış: hepsi parts'a + isteğe bağlı satın
  alma aktarımı — geçiş kuralları kullanıcıyla netleştirilecek.)

## PAKET G — Plaka MRP entegrasyonu (#10) (BÜYÜK)
- Yeni "İhtiyaç Planlama" alt-sekmesi (Satın Alma içinde).
- Akış: satın alma kaleminde "İhtiyaç Planla" işareti → havuza düşer (tüm projeler) →
  havuzdan seçim → SAC (2D guillotine) veya PROFİL (1D) hesabı → arkadaşın algoritması
  (packBestGuillotine/runGuillotine + profil konsolidasyonu index.html'e port edilir,
  canvas çizimiyle) → sonuç listesi manuel revize (ekle/çıkar) → ONAY → her plaka/profil
  için satın alma kalemi oluşur (PLANNED), kaynak kalemler plakaya bağlanır
  (purchase_items'a `stock_plan_id`/parent bağı — DB kararı) → sipariş + QR mal kabul
  fişinde "bu plakadan çıkacak parçalar" listesi.
- Plaka/profil stok ölçüleri tanım tablosu (örn. 1350×5000×3) — DB'de küçük tablo veya
  localStorage yerine `stock_sheets` tablosu.
- Ölçü kaynağı: BOM ölçü alanları (width/height/thickness_mm — 07.07'de eklendi).

## PAKET H — İşlem bağımlılığı (#8) (BÜYÜK, ayrı tur önerilir)
- Ağaç hiyerarşisi yayınla sırasında parts'a taşınmıyor (parts düz) →
  `parts.parent_part_id` (veya pbp referansı) eklenmeli.
- Kural: üst parçanın iş emri, alt parçalar bitmeden BAŞLATILAMASIN (uyarı/engel);
  dashboard'da "X'in A ve B parçaları bekleniyor"; iş emri çıktısında alt parçalar yazsın.
- Kapsamlı iş — F/G bittikten sonra ayrıca planlanmalı.

## SONRADAN EKLENEN MADDELER (1.txt 02:18 güncellemesi)
- **(#11) "Mal kabul fişi kalemler kısmında tek tek verilenlerde yok"**: BÜYÜK OLASILIKLA
  ESKİ SAYFA — kalem bazlı "📄 Mal Kabul Fişi" butonu ORDERED/RECEIVED kalemlerde 894f62a'da
  eklendi. Yeni oturumda ÖNCE canlıda doğrula (Ctrl+F5); gerçekten eksik senaryo varsa
  (örn. PLANNED'dan doğrudan RECEIVED?) PAKET A'ya al.
- **(#12) Tedarikçi listesi (kartotek)**: `suppliers` tablosu (ad, iletişim, vergi bilgileri,
  not) + backend CRUD + Satın Alma'da kalem tedarikçisi ve teklif firması alanları listeden
  seçilsin ("listede yoksa ekle" seçeneğiyle — combobox deseni). İrsaliye alıcısı için de
  aynı listeden seçim düşünülebilir (Cari Rehber görselindeki gibi). → **PAKET B'ye dahil et**
  (satın alma maliyetleriyle aynı bölgede çalışılıyor).

## Önerilen sıra
A (hızlı) → B → C → D → E → F → G → H. Arkadaş onayı gelince ayrıca: Parçalar sekmesi kaldırma
(görev #4, bekliyor).

## Açık sorular (kullanıcıya/arkadaşa)
1. PAKET D: Mal Kabul ayrı nav sekmesi mi, Satın Alma alt-sekmesi mi? (öneri: alt-sekme)
2. PAKET E: "depodaki ürünü projeye aktar" tam olarak ne — stok satırına proje etiketi mi?
3. PAKET F: yayınlamada TEDARIK/SARF parçalar parts'a HİÇ girmesin mi (dashboard'da görünmesin)?
4. PAKET G: plaka stok ölçüleri sabit katalog mu, her hesapta elle mi girilecek?
