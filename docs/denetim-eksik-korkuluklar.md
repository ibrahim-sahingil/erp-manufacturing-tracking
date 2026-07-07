# Eksik Korkuluk / Akıllı Davranış Denetimi (2026-07-07)

Kod tabanı tarandı (backend servisleri + FK kuralları + frontend akışları).
HİÇBİR DÜZELTME YAPILMADI — önce karar bekleniyor. Önem sırasına göre:

## 🔴 KRİTİK

### K1. Sipariş (proje) silme — tek onayla tüm proje verisi gidiyor
`OrderService.delete` hiçbir kontrol yapmıyor; DB'de `orders` silinince CASCADE ile
**parçalar, bölümler, iş emirleri, proje tarihleri** topluca siliniyor. Üstelik
`purchase_items` / `project_bom` proje adını STRING tuttuğu için bunlar silinmiyor,
**sahipsiz kalıyor**. Frontend'deki koruma yalnız `confirm('Bu siparişi silmek
istiyor musunuz?')` — neyin gideceğini söylemiyor. 2026-07-06 veri kaybı dersinden
sonra buradaki en büyük risk bu.
**Öneri:** Backend'e guard: parçası/iş emri/satın alma kalemi olan proje silinemesin
("önce arşivle/pasife al" akışı); frontend onayında silinecek kayıt sayıları listelensin.

### K2. Proje ADI değişince satın alma / proje ağacı kopuyor
`orders.project_name` düzenlenebilir; ama `purchase_items.project_name`,
`project_bom.project_name`, `purchase` akışındaki tüm gruplamalar string eşleşmeyle
çalışıyor. Proje adı düzeltilirse (yazım hatası vb.) satın alma kalemleri, mal kabul,
depo proje görünümü ve BOM bağlantıları **eski ada takılı kalıyor** — ekranda proje
ikiye bölünmüş gibi görünür.
**Öneri:** (a) kısa vade: rename'de backend string tabloları da güncellesin
(tek transaction); (b) uzun vade: string yerine order_id'ye geçiş.

### K3. Yetki kontrolü yalnız frontend'de
Backend `anyRequest().authenticated()` — rol/permission hiçbir endpoint'te
denetlenmiyor. "Kullanıcı" rolündeki biri butonları göremese de API'ye doğrudan
istekle **proje silebilir, kullanıcı ekleyebilir, her şeyi değiştirebilir**.
Cloudflare tüneliyle dışarı açıldığı için JWT'si olan herkes = tam yetki.
**Öneri:** En azından yıkıcı uçlara (DELETE orders/users, user create/update)
`developer` rol şartı; SecurityConfig'te method/route bazlı kural.

## 🟠 ORTA

### O1. Üretim parçası silme korumasız
`PartService.delete` guard'sız: iş emrine bağlı, ilerlemesi (`qty_done>0`) olan,
alt parçaları olan parça tek confirm ile siliniyor (work_order_parts + part_logs
CASCADE gidiyor, alt parçaların üst bağı sessizce NULL oluyor).
**Öneri:** İş emri bağı / ilerleme / alt parça varsa engelle (BOM taraflarındaki
desenle aynı: "önce şunları kaldır" mesajı).

### O2. İş emri OLUŞTURURKEN #8 engeli atlanabiliyor
"Alt parçalar bitmeden üst başlatılamaz" kuralı yalnız dashboard durum değişiminde
ve revizede kontrol ediliyor. Yeni iş emri formunda durum "🔄 Devam Ediyor"
seçilirse `saveWorkOrder` kontrolsüz kaydediyor.
**Öneri:** `saveWorkOrder`'a da `woWaitingChildren` kontrolü (durum inprogress ise).

### O3. Depodaki kalemin miktarı düzenlenince stok defteri güncellenmiyor
`editPurchaseItem` her durumda (IN_WAREHOUSE dahil) miktarı serbestçe değiştiriyor;
depo hareketi (IN) eski adette kalıyor, `received_qty` de eski değerde. Depo özeti
ile kalem listesi sessizce ayrışıyor.
**Öneri:** IN_WAREHOUSE/RECEIVED kalemde miktar değişimini engelle ya da düzeltme
hareketi (fark kadar IN/OUT) + received_qty senkronu iste.

### O4. Satın alma kalemi silme korumasız
`PurchaseItemService.delete` guard'sız: depoda olan (IN hareketi yazılmış) veya
sipariş grubuna bağlı kalem silinebiliyor; hareketlerdeki `purchase_item_id`
SET NULL olup kayıt "münferit stok" gibi kalıyor.
**Öneri:** IN_WAREHOUSE / ORDERED(grupta) kalemlerde silmeyi engelle
("önce depodan geri al / gruptan çıkar").

### O5. Hareket defterinden kayıt silme tutarlılık kontrolü yok
`whmDeleteMovement` her hareketi silebiliyor. PURCHASE_TRANSFER/GOODS_RECEIPT IN
kaydı silinirse kalem hâlâ IN_WAREHOUSE görünür ama stok özetinde yoktur; DELIVERY
OUT'u silinirse sevk edilmiş irsaliye stoğu geri şişer.
**Öneri:** purchase_item/delivery bağlı hareketlerde silmeyi engelle (yalnız MANUAL
ve WAREHOUSE_TRANSFER silinebilsin) ya da silerken bağlı kaydın durumunu düzeltmeyi öner.

### O6. İrsaliye sevkinde stok yeterlilik kontrolü yok
`dnShip` depodan düşerken mevcut net stoğa bakmıyor — elde 3 varken 10 sevk
edilebiliyor (eksi stok, uyarısız). Manuel çıkışta uyarı var, sevkte yok.
**Öneri:** Sevk öncesi `_whItemStock` kontrolü; eksiye düşecekse manueldeki gibi
onaylı uyarı.

### O7. Yeniden yayınlamada adet, üretim ilerlemesini ezebilir (4. turda eklendi)
Republish artık parça adedini BOM toplamına eşitliyor. Üretim 5/5 bitmişken BOM
2'ye düşürülüp yeniden yayınlanırsa `qty_done(5) > qty(2)` olur; yüzde/istatistik
tutarsızlaşır.
**Öneri:** `qty_done+qty_reject > yeni adet` ise güncellemeyi atla + mesajda belirt.

### O8. Eski proje ağaçlarında hiyerarşi bağları kopuk (veri onarımı gerekli)
Bugfix (f688fc6) yeni kopyalamaları düzeltti; ama daha önce şablondan kopyalanan
`project_bom_parts` satırlarının `parent_custom_id`'si hiç yazılamadığı için DÜZ
duruyor. Yayınlama şablon hiyerarşisine düştüğü için çalışıyor; ancak proje ağacı
editörü bu ağaçları düz liste gösteriyor.
**Öneri:** Tek seferlik onarım scripti: bom_part_id + şablon parent'ından
parent_custom_id'yi geri kur.

## 🟡 KÜÇÜK / İYİLEŞTİRME

- **U1. Kullanıcı silme 500 veriyor:** `part_logs.user_id` ve `orders.approved_by`
  FK'larında ON DELETE kuralı yok → log'u/onayı olan kullanıcı silinince ham
  INTERNAL_ERROR. Dostça mesaj ("kayıtları var, pasife alın") yok. (Pasife alma da yok.)
- **U2. Bölüm silme sessiz:** parçaların bölümü sessizce NULL'a düşüyor; "X parça
  bölümsüz kalacak" uyarısı yok.
- **U3. İş emri silme:** durum ne olursa olsun (devam eden dahil) tek confirm.
- **U4. QR ilerleme girişinde yarış:** iki kişi aynı parçaya aynı anda giriş yaparsa
  son yazan kazanır (oku-hesapla-yaz deseni). Tek tezgâhta düşük risk; log tablosu
  gerçeği tuttuğu için onarılabilir.
- **U5. Login'de hız sınırı yok + zayıf test hesabı:** tünel dışarı açıkken
  `testdev/test1234` tahmin edilebilir. Test hesabının şifresi güçlendirilmeli
  ya da tünel açıkken pasife alınmalı; login'e basit rate-limit iyi olur.
- **U6. Kısmi kabul/bölme kopyaları grup bağını taşımıyor:** bölünen yeni kalemde
  `purchase_order_id`, `ordered_at`, `project_bom_part_id` boş — sipariş grubu
  raporunda bölünen parça görünmez (bilinçli sadelik, ama not edilmeli).
- **U7. Yayınlama atomik değil:** parça parça INSERT; ortada hata olursa yarım
  yayın kalır (tekrar yayınla telafi ediyor — mesaj zaten hatayı sayıyor).
- **U8. `ea()` çift tırnak kaçırmıyor:** isimlerde `"` varsa onclick attribute'ları
  bozulabilir (proje/malzeme adlarında tırnak kullanılırsa). Genel HTML-escape
  yardımcı fonksiyonuna geçiş düşünülebilir.

## Zaten iyi olan korkuluklar (bilgi)
BOM/ürün/operasyon silme zincir korumaları; depo silme koruması (backend);
teklif/grup kilitleri; manuel çıkışta eksi stok uyarısı; QR mal kabulde çift
kabul yarışı kontrolü; ilerleme girişinde adet aşımı engeli; Excel import satır
limiti + üst basamak doğrulaması; irsaliyede sevk edilmişi silme engeli.

## Önerilen ele alma sırası
K1 → K2 → K3 (birlikte "veri güvenliği turu") → O1..O5 (silme/tutarlılık turu)
→ O6..O8 → U'lar fırsat buldukça.

---

# EK — 2. Tarama Bulguları (aynı gün, derin tarama)

### E1 (🔴/🟠 Orta-Yüksek). XSS: kullanıcı girdileri innerHTML'e ham basılıyor
Parça/proje/tedarikçi/depo adları, notlar vb. yüzlerce noktada `${p.name}` gibi
kaçışsız innerHTML'e gidiyor. Adına `<img src=x onerror=...>` yazılan bir kayıt,
o ekranı açan HERKESİN tarayıcısında kod çalıştırır. "Beni hatırla" JWT'yi
localStorage'da tuttuğu için (E7) oturum çalınabilir. Tünelle dışa açılan ve
birden çok kişinin kullandığı ortamda ciddi.
**Öneri:** Merkezi `esc()` yardımcı fonksiyonu + innerHTML enterpolasyonlarında
kullanım. Büyük ama mekanik iş; en azından ad/not alanlarından başlanmalı.

### E2 (🟠 Orta). Ondalık adetler yayınlamada sessizce kesiliyor
BOM adetleri `numeric(15,4)` (2.5 kg, 1.2 m olabilir); üretim tablosu
`parts.total_qty` ise INTEGER. Yayınlamada 2.5 → 2'ye sessizce yuvarlanır
(Jackson float→int), birim bilgisi de parts'a hiç taşınmaz. kg/m/m² birimli
hammaddelerde üretim adedi yanlış görünür.
**Öneri:** `total_qty`'yi numeric'e migrate et (+ parts'a birim kolonu) YA DA
yayınlamada tam sayı olmayan adetlerde uyarı verip yukarı yuvarla.

### E3 (🟠 Orta). Basılı QR fişlerin linkleri kalıcı değil
QR içerikleri `location.origin` ile üretiliyor (mal kabul fişi, parça fişi).
Tünel adresi her açılışta değiştiği için dün trycloudflare'den basılan fişin
QR'ı bugün ÖLÜ; localhost'tan basılan fiş yalnız o makinede çalışır.
**Öneri:** Kalıcı adres (adlandırılmış cloudflare tüneli / sabit domain) ya da
QR içeriğini salt kalem-id yapıp uygulama içinden "QR okut" akışı.

### E4 (🟡 Küçük-Orta). Parça kodu değişince kod-eşleşmeli zeka kopuyor
"Malzemesi hazır/siparişte" şeridi, planlama ağacındaki malzeme çipleri ve
yayınlamadaki "zaten var" kontrolü proje+kod eşleşmesiyle çalışıyor. Üretim
parçasının ya da satın alma kaleminin kodu düzenlenirse bu bağlar sessizce
kopar (iki taraf ayrı ayrı düzenlenebilir, senkron yok).
**Öneri:** Kod düzenlenirken karşı tarafta eşleşen kayıt varsa kullanıcıya
"satın alma kalemi de güncellensin mi?" diye sor.

### E5 (🟡 Küçük). İptal edilen plakanın kaynak kalemleri havuza dönmüyor
MRP onayında kaynak kalemlere `stock_plan_id` bağlanıp `needs_planning=false`
yapılıyor. Plaka kalemi ✖ İptal edilirse (normal durum butonuyla mümkün)
kaynaklar iptal plakaya bağlı kalır; plaka silinirse bağ NULL olur ama
needs_planning false kaldığı için parçalar planlama havuzuna OTOMATİK dönmez —
plakası iptal edilen parçalar planlamadan kaybolur.
**Öneri:** Plaka kalemi iptal/silinirken kaynakları çöz ve needs_planning=true
ile havuza geri koy (ya da kullanıcıya sor).

### E6 (🟡 Küçük). Yayınla çift tetiklenmeye açık
`openAndPublishPbom` çalışırken buton disable edilmiyor. parts DB'deki unique
indeksle korunuyor ama purchase_items için benzersizlik YOK — üst üste iki
yayınlama koşarsa mükerrer satın alma kalemi oluşabilir.
**Öneri:** Yayınla/Bağlantı Oluştur gibi async butonlarda çalışırken disable
deseni (bazı formlarda zaten var).

### E7 (bilgi). "Beni hatırla" JWT'yi localStorage'da tutuyor
Şifre saklanmıyor (doğru tasarım); ama E1'deki XSS ile birleşince token
çalınabilir. E1 çözülürse bu kabul edilebilir risk.

### 2. taramada temiz çıkanlar (bilgi)
Proje tarihlerinde başlangıç>bitiş kontrolü var; kayıt butonlarının çoğunda
async sırasında disable var; "beni hatırla" şifreyi değil token'ı saklıyor;
modallı onay akışlarında (aktarım/kabul) overlay ilk satırda kaldırıldığı için
çift tıklama riski düşük; proje↔ürün bağında DB unique kısıtı çift kaydı
engelliyor.
