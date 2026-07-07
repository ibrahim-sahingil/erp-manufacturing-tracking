# 4. Tur Kod İncelemesi — Teknik Bulgu Listesi (2026-07-07)

8 açılı inceleme (42 aday → tekilleştirme → doğrulama) sonucu. Satır numaraları
o günkü HEAD'e (1307f90 sonrası) göredir; düzeltme sırasında kaydırma olabilir.
Dostane sürüm: Masaüstü\ERP-Inceleme-Raporu-2026-07-07.pdf

## DURUM (2026-07-07): B1-B10 TAMAMI DÜZELTİLDİ ✅

Commit'ler: B1 4565438 · B2 422ebfe · B3 e27f61b · B4 c7391c6 · B5 093910c ·
B6 7bc708c · B8 47eb5d4 · B9 5762045 · B10 cfb8740 · B7 c5cf927.
E2E genişletildi (B1/B2/B3/B4/B7 senaryoları) ve tüm kontroller geçiyor.
Nota değer sapmalar:
- B4: (b) seçeneği uygulanMADI — whDoTransfer IN_STOCK kökenli kalemlere de
  received_qty yazdığından sezgi stok kalemlerinin IN_STOCK dönüşünü kapatırdı;
  (a) uygulandı: UpdateRequest'e receivedAt, bölme yolları damga taşır.
- B7: kullanıcı seçimi 'Termin + damga' — expected_date + ordered_at (B4
  deseniyle) taşınır, grup adı notes'a yazılır, grup detayı kod eşleşmeli
  '🔀 Bölünmüş kalemler' gösterir; purchase_order_id kilit istisnası AÇILMADI.
Sıradaki: K1-K3 veri güvenliği turu (feature branch + /code-review ultra) →
E1 XSS → temizlik turu (aşağıdaki yedek liste).

## DÜZELTİLECEK BULGULAR (öncelik sırasıyla)

### B1 (CİDDİ) — Yeniden yayınlamada parts.qty yanlış ezilir
`index.html` ~10622, `pbomPublishParts` prodGroups exists dalı.
`exists` proje+kod ile TÜM projede aranır ama gruplama TEK pbom'a aittir:
aynı kod iki makinede varsa her yayın kendi toplamını yazar (ping-pong).
Ayrıca elle düzeltilmiş adet ezilir ve qty_done > yeni qty kontrolü yok.
FIX yönü: exists.qty güncellemesini AYNI projeye bağlı TÜM pbom'ların o kod
toplamı üzerinden hesapla (veya yalnız bu pbom'dan gelen katkıyı ayrıştır);
qty_done+qty_reject > hedef ise güncelleme yapma + mesajda bildir.

### B2 (CİDDİ) — Satın alma adedi republish'te güncellenmiyor → eksik sipariş
`index.html` ~10604, purGroups purExists dalı yalnız purSkipped++.
Ayna dal eklenince toplam artar ama PLANNED kalem eski adette kalır.
FIX yönü: purExists bulunduğunda kalem PLANNED ise quantity'yi grup toplamına
dbUpdate et (ORDERED/RECEIVED ise dokunma, mesajda "sipariş verilmiş, elle
kontrol edin" uyarısı ver); res.purUpdated sayacı ekle.

### B3 (CİDDİ) — Parent taşıma kardeş-kod kontrolünü atlıyor (backend)
`BomPartService.java` ~195 (applyParentChange) + `ProjectBomPartService.java`
~292: {parent_id}/{parent_custom_id} yalnız gelen istekte kod kontrolü yok.
FIX yönü: applyParentChange içinde hedef parent altında part.getCode() için
existsSiblingCodeExcept çağır; çakışmada BOM_PART_CODE_EXISTS fırlat.
(pbome sürüklemesi de aynı yoldan geçer.) E2E'ye taşıma senaryosu ekle.

### B4 (ORTA) — whUndo bölünmüş kalemi IN_STOCK'a düşürüyor
`index.html` ~7664: back = received_at ? RECEIVED : IN_STOCK.
Bölme ile oluşan IN_WAREHOUSE kalemlerde received_at NULL (create yolu damga
atmaz) → geri alma mal kabul akışından koparıyor.
FIX yönü: (a) bölmede yeni kaleme received_at'i frontend'den geçir (update ile
gönderilebilir mi? — UpdateRequest'te yok; ya UpdateRequest'e ekle ya da)
(b) whUndo'da received_qty>0 olan kalemi de RECEIVED say.
(b) daha basit ve yeterli.

### B5 (ORTA) — openPurImport ayna dal satırını çift sipariş edebiliyor
`index.html` ~6840: `already` seti yalnız project_bom_part_id ile.
FIX yönü: sete proje+kod anahtarını da ekle (mevcut purchase_items'tan
code eşleşmesi); listede "aynı kod zaten kalemde (adet X)" rozetiyle
öntanımlı SEÇİMSİZ getir.

### B6 (ORTA) — whXferConfirm hata yönetimi
`index.html` ~7754-7800:
- münferit: OUT/IN mv() sonuçları kontrolsüz + koşulsuz başarı toast'ı.
- kısmi: rollback received_qty'yi geri yüklemiyor; yeni kalemin warehouse_id
  update'i kontrolsüz.
FIX yönü: mv() dönüşlerini kontrol et; OUT yazılıp IN düşerse telafi (OUT'u
sil ya da kullanıcıya net hata); rollback'te received_qty: x.maxQ da yaz;
warehouse update başarısızsa kalemi sil + hata göster.

### B7 (ORTA) — Bölünen kalem grup/ağaç bağlarını kaybediyor
`rcvDoReceive` ~5969 ve whXferConfirm kısmi dal: purchase_order_id,
ordered_at, expected_date, project_bom_part_id kopyalanmıyor.
FIX yönü: bölme insert'ine expected_date + notes referansı; insert sonrası
update'e ordered_at taşınamaz (backend damgası) ama purchase_order_id
UpdateRequest'te VAR (DRAFT kilidine takılır — ORDERED grupta taşınamaz;
o yüzden en azından raporlarda görünmesi için notes'a grup adı yaz VE grup
detayı render'ında kod eşleşmeli bölünmüş kalemleri de göster). Tasarım
kararı gerektirir — uygulamadan önce kullanıcıya seçenek sun.
NOT: project_bom_part_id create'te kabul ediliyor (Request'te var) ama
duplicate guard'a takılır (existsByProjectBomPartId) — bölmede taşınamaz;
kod eşleşmesi yeterli olabilir.

### B8 (KÜÇÜK) — ea() çift tırnak kaçırmıyor
`index.html` ~2040 ea() + ~7533 whXferLoose onclick (ve aynı desendeki tüm
attribute kullanımları). FIX yönü: ea()'ya `.replace(/"/g,'&quot;')` ekle
(onclick attribute'ları çift tırnaklı olduğundan güvenli); uzun vadede E1
(XSS) turunda merkezi esc() zaten gelecek.

### B9 (KÜÇÜK) — Dal kopyalamada yetim alt ağaç
`bomCopyBranch` ~9429 / `pbomeCopyBranch` ~10338: atası eklenemeyen çocuklar
parent'sız ekleniyor. FIX yönü: idMap'te parent yoksa (kök hariç) o satırı
ve altını atla (fail say), Excel import'taki desen gibi.

### B10 (KÜÇÜK/TEMİZLİK) — Ölü publishProjectBom silinsin
`index.html` ~11088, çağıranı yok (~90 satır). Sil.

## YEDEKTE BEKLEYEN İYİLEŞTİRME ÖNERİLERİ (hata değil; ayrı "temizlik turu")
- bomCopyBranch/pbomeCopyBranch ikizleri → tek parametrik copyBranch helper.
- whXferConfirm/rcvDoReceive bölme mantığı → tek splitPurchaseItem helper.
- Kardeş-kod ön-kontrolü 7 kopya → tek findSiblingDup helper.
- warehouse_movements payload kurulumu 6 kopya → tek mvInsert helper (whXfer'in
  lokal mv()'si module-level'a çıkarılabilir).
- Depo <option> listesi + "önce depo tanımlayın" guard'ı 5 kopya → helper.
- `const inp = 'width:100%...'` stil dizesi ~40 kopya → tek sabit/CSS sınıfı.
- _whXfer globali yerine onclick'e JSON argüman (rcvBulkConfirm deseni).
- linkedThisPass Set yerine prodGroups.values() üzerinden g.rows[0] ile bağ.
- pbomRenderMachineRows tam innerHTML rebuild (focus kaybı) → option.disabled.
- woLoadParts: tüm purchase_items + tüm pbp çekiliyor → projeye filtreli çek;
  kidsOf/woMatChip O(n²) → Map ile O(1); büyük projede gecikme.
- Kopyalama/toplu kabul döngüleri sıralı await → seviye-seviye Promise.all.
- whXferConfirm sonrası tüm hareket tablosunu yeniden çekmek yerine dönen
  kayıtları listeye ekle.
- PurchaseItem.java sınıf doc bloğuna received_by/received_qty/returned_qty
  kolonlarını ekle (CLAUDE.md: entity doc = şema belgesi).

## Bağlam
- Genel sistem denetimi (K1-K3, O1-O7, E1-E7, U1-U8): docs/denetim-eksik-korkuluklar.md
- Regresyon: `node scripts/e2e-test.js <jwt>` — her düzeltme sonrası koş.
- Önerilen düzeltme sırası: B1→B6 (bu dosya) → K1-K3 veri güvenliği turu
  (feature branch + /code-review ultra) → E1 XSS → temizlik turu.
