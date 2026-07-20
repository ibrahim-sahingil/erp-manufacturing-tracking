# 14. Tur — Arkadaş Cevapları Uygulama Özeti (2026-07-17)

## Bağlam
14. tur = 13. tur raporundaki S1-S7 sorularına arkadaşın cevapları (kullanıcı iletti):
"1: adet bazlı. 2: parçalar paketlenip istenilen depoda dursun. 3: Downloads/Çeki Listesi.xlsx.
4: sipariş üzerinden izlensin. 5: yalnız havuza aktar beklemesi yeterli. 6: kilitli olsun. 7: ad önerilsin."
Dal: `tasarim-2026`; her paket ayrı commit + doğrulama kapısı 14/14 (EXIT kontrolüyle).

## Paketler
- **G `d36ec3a` (S5+S6):** mrpApprove + mrpUseLeftover kalemleri yeniden DOĞRUDAN satın almaya düşer
  (sent_to_purchasing=false kaldırıldı) — yalnız "Havuza Aktar" MİP'te bekler. 'ORTAK (MRP)' plaka
  istisnası kalktı: ortak plaka da kilitler ("Ortak havuz" rozeti, "Bunu Kullan" çıkmaz).
- **H `7819f3a` (S7):** Opdef "Bölüm (öneri)" alanı geri geldi (13. tur K3 kısmen geri alındı — arkadaş
  kararı). İşlem eklerken parça bölümü BOŞSA ve ad projede MEVCUT bölümle eşleşiyorsa atanır
  (pbomeAddOpCore/oneriDeptId). OLUŞTURMA / CASCADE / EZME yok; backend update department_name'i yazar.
- **I `4818e01` (S1):** `project_bom_parts.ship_planned_qty` (UPDATE DTO presence-takipli, explicit null
  temizler). Plana alırken adet sorulur (tam adet varsayılan, aşamaz); "SEVK PLANINDA ×N" rozeti;
  paketleme kalan hesabı `shipTargetQty` (planlıysa plan adedi).
- **K `7915740` (S2+S3):** `shipment_packages`'a warehouse_id (presence-takipli) + package_type
  (CHECK PACKAGE/BOX/PALLET/CRATE — ÜÇLÜ kural) + net_weight_kg. Form tip/net/depo seçicileri;
  kart + QR sayfası + pkgPrint'te gösterim. **dnWeighList arkadaşın Excel şablonuna göre yeniden
  yazıldı** (iki dilli başlıklar, üst blok, paket içi satırlar rowspan'lı, Brüt/Net, TOTAL n PCS PACKING).
- **J `19d9176` (S4):** `orders.shipping_status` KALICI zincir (hazirlaniyor/yuklendi/sevk_edildi/
  teslim_edildi; null=dokunma, ""=temizle — tam-gövde PUT'lar sıfırlamaz). Otomatik geçişler yalnız
  YÜKSELTİR (ilk paket / araca yükleme / irsaliye sevki); geri almalar TAZE veriden türetir
  (orderShippingRecompute — teslim işareti düşer, bilinçli). "Teslim Edildi" ELLE (kart butonu) + geri al.
- **Düzeltme `31c56f3` (arkadaş smoke notu):** Paketleme "Sevk Edilecekler"e "Yalnız sevk planındakiler"
  filtresi (planlı satır varsa varsayılan AÇIK; data-user ile kullanıcı tercihi korunur) + ham
  plaka/profil PLAN-PARENT kayıtları sevk kaynağından tamamen çıkarıldı.

## Şema değişiklikleri (yedek + dump her birinde)
project_bom_parts.ship_planned_qty · shipment_packages.{warehouse_id, package_type, net_weight_kg} ·
orders.shipping_status. DB yedekleri: 2026-07-17_1046 / _1056 / _1109.

## Dersler
- Render fonksiyonuna yeni GLOBAL/DOM bağımlılığı eklenince verify-h-render shim'i de güncellenmeli
  (3 kez tekrarladı: shipmentPackages, SHIP_CHAIN, checkbox.dataset/checked → stub'a eklendi).
- Kapı sonucu `| tail` ile değil `EXIT=$?` + BAŞARISIZ grep'iyle kontrol edilir (sessiz exit-0 tuzağı).
- Quick tunnel günde birkaç kez ölebiliyor — arkadaşa link vermeden `curl <url>/api/config` 200 kontrolü.

## Açık noktalar (rapora Y1-Y3 + T1-T2 olarak eklendi — cevaplar 15. turu şekillendirir)
- **Y1:** Paket deposu bilgi alanı mı kalsın, fiili depo hareketi mi yazılsın (S2 varsayım teyidi)?
- **Y2:** Çeki listesi üst bloğu (Gönderen/Teslim Koşulları/Menşei) kaynağı: firma ayarları / irsaliye
  alanları / elle?
- **Y3:** Fatura durumu: elle alan / erteleme / dış kaynak?
- **T1:** ?paket= QR sayfası girişli kalsın mı? **T2:** Ortak plakaya "serbest bırak" gereksin mi?

Rapor: `Desktop\ERP proje\01 - Belgeler\13-tur-rapor-2026-07-17\rapor.html` (güncellemeler + yeni
sorular + canlı adres içinde). `shipping` yetkisi kullanıcı kararıyla şimdilik kimseye verilmedi
(smoke sonrası kendisi dağıtacak). Master merge kararı arkadaş smoke'u sonrası.
