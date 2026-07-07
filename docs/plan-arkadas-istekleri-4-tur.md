# Arkadaş İstekleri 4. Tur — Uygulama Planı (2026-07-07)

Kaynak: `Desktop/ERP proje/02 - Test ve Istekler/4. test ve değişiklikler/`
(test.txt + 100/101/102.jpeg). Her paket: derleme + ayrı commit.

## PAKET 1 — Aynı kod farklı dallarda + dal kopyalama (#1, görsel 100)
İstek: Sol/sağ direk gibi aynalama parçaların İÇİNDEKİ saclar aynı kod/açıklamayla
ağacın farklı dallarında tekrar kullanılabilsin; bir kopyasına da izin verilsin.

- **BomImportService**: "Ayni kod dosyada X. satirda da var" hatası yalnızca
  AYNI ÜST BASAMAK altında tekrar için kalır; farklı dalda aynı kod → hata yok.
- **BomPartService create/update**: kod benzersizliği ürün geneli → AYNI PARENT
  kapsamına indirilir (kök seviye dahil). Yeni repo metodları.
- **ProjectBomPartService create/update**: aynı gevşetme (parentCustomId kapsamı).
- **Yayınla (pbomPublishParts)**: `parts` tablosunda (order_id, kod) UNIQUE olduğundan
  aynı kod n dalda → TEK parts satırı, adet = TOPLAM (yeniden yayınlamada adet
  güncellenir). Hiyerarşi bağı ilk dalın atasına kurulur. Satın alma kalemlerinde de
  aynı kod → tek kalem, adet toplamı.
- **Dal kopyalama**: ürün ağacı + proje ağacı editörlerinde parça satırına
  "📋 Dal Kopyala" — parçayı alt ağacıyla birlikte aynı üst altına kopyalar
  (adlar/kodlar aynen; kullanıcı sonra SOL→SAĞ diye düzenler).

## PAKET 2 — Depolar arası malzeme aktarımı (#2)
- DB: `warehouse_movements_source_check` kısıtına `WAREHOUSE_TRANSFER` eklenir
  (DROP+ADD); `WarehouseMovementService.VALID_SOURCES` + `WH_SOURCE_LABELS` güncellenir.
- Depo bazlı görünümde kalem satırına "🔁 Başka Depoya" → modal: hedef depo + ADET.
  * Adet = kalemin tamamı → warehouse_id güncellenir + OUT(kaynak)/IN(hedef).
  * Adet kısmi → kalem BÖLÜNÜR: orijinal adedi azalır, hedef depoda yeni
    purchase_item (IN_WAREHOUSE) oluşur + OUT/IN hareketleri.
- Münferit (projesiz) satırlarda da "🔁": hedef depo + adet → OUT + IN
  (purchase_item_id NULL, notes "Depolar arası aktarım").

## PAKET 3 — Toplu depoya alma + mal kabul hesap/adet bilgileri (#3, görsel 101)
- DB `purchase_items`: + `received_by varchar(150)`, + `received_qty numeric(15,4)
  DEFAULT 0 NOT NULL`, + `returned_qty numeric(15,4) DEFAULT 0 NOT NULL`.
  Entity + Response + UpdateRequest güncellenir; şema yeniden dump'lanır.
- **Toplu aktarım**: Mal Kabul sekmesinde satırlara checkbox + "✅ Seçilenleri
  Depoya Al (n)" → tek modalda depo seç → hepsi aktarılır.
- **Kısmi kabul**: "Geldi (depoya al)" modalına gelen adet + iade adet alanları.
  Depoya giren = gelen − iade; `received_qty` birikir, `returned_qty` birikir;
  bekleyen = sipariş adedi − received_qty. received_qty tamamlanınca IN_WAREHOUSE,
  yoksa ORDERED kalır (kısmi gelen depoda görünür — IN hareketi kabul adediyle).
- **Kim aldı**: whDoTransfer/receiveConfirm `received_by` damgalar (movement
  performed_by zaten var); satırda "👤 <alan>" + "✅ x geldi · ↩ y iade · ⏳ z bekleniyor".
- whUndo: received_qty/received_by sıfırlar, OUT hareketi depoya girmiş adet kadar.

## PAKET 4 — Planlamada ağaç görünümü + malzeme durumu (#4, görsel 102)
- İş emri "Parça Seçimi" düz kart grid → `parts.parent_part_id` hiyerarşisiyle
  girintili AĞAÇ listesi (checkbox seçim korunur; bölüm filtresi eşleşmeyenleri
  soluk/seçilemez gösterir, bağlam kaybolmaz).
- Her düğümde malzeme durumu çipi: proje+kod eşleşmeli purchase_item varsa
  (📝 planlandı / 📦 siparişte / 📥 geldi / 🏭 depoda).
- Satın almaya yönlenen BOM malzemeleri (TEDARIK/HAMMADDE/SARF): pbp hiyerarşisinden
  en yakın üretim atası bulunup ilgili düğüm altında seçilemez bilgi satırı
  "🛒 <ad> — <durum>" olarak gösterilir; ata çözülemezse ağaç altında
  "🛒 Satın alma malzemeleri" bölümü.

## Sıra ve durum (2026-07-07 tamamlandı)
1 → 2 → 3 → 4.
- [x] PAKET 1 (f688fc6) — backend gevşetme + yayınla toplama + dal kopyala.
      BONUS BUGFIX: openPbomEditor şablon kopyalamasında parent_custom_id
      genId ile gidiyordu (backend 500 → bağlar kopuk); dönen id kullanıldı.
- [x] PAKET 2 (33c61d5) — depolar arası aktarım (tam taşıma / kısmi bölme /
      münferit); WAREHOUSE_TRANSFER kısıtı DB'de uygulandı.
- [x] PAKET 3 (05cc068) — toplu mal kabul + received_by/received_qty/
      returned_qty (DB'de uygulandı + gelen kayıtlar backfill edildi).
- [x] PAKET 4 (8b68927) — iş emri parça seçimi ağaç + malzeme durumu çipleri.
- E2E smoke: farklı dalda aynı kod OK / aynı dalda RED; WAREHOUSE_TRANSFER
  hareketi kabul; yeni alanlar API cevabında. Yedek: uretim_takip_2026-07-07_1219.dump

## Kararlar (arkadaşa not)
- Yayınlamada aynı kodlu parçalar üretimde TEK satırda toplanır (adet toplamı) —
  parts tablosunda kod proje içinde benzersiz olduğundan; iş emri bağımlılığı
  ilk dalın atasına bağlanır.
- Kısmi mal kabulde iade edilen adet "yeniden beklenen" sayılır
  (bekleyen = sipariş − kabul edilen).
