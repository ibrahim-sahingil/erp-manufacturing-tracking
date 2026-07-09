# Üretim Takip ERP — Proje Rehberi

QR kod destekli üretim takip sistemi. Tek geliştirici (İbrahim);
ikinci kişi fikir/test notu sağlar, geliştirme yapmaz.

## Çalıştırma

```
./mvnw spring-boot:run        # http://localhost:8080/ (frontend de buradan servis edilir)
./mvnw clean package -DskipTests
```

- PostgreSQL yerelde çalışır olmalı (DB: `uretim_takip`, şema: `db/schema.sql`).
- Proje kökünde `secrets.properties` gerekir (git'e girmez): `DB_PASSWORD=...` ve `APP_JWT_SECRET=...`
- Uzaktan demo: `cloudflared tunnel --url http://localhost:8080` (adres her açılışta değişir; CORS `*.trycloudflare.com`a açık).

## Kritik Bilgiler (bunları bozma!)

- **pom.xml'deki `<proc>full</proc>` ZORUNLU** — JDK 22+ annotation processing'i
  varsayılan kapatır; bu ayar olmadan Lombok hiç çalışmaz ve yüzlerce
  "cannot find symbol" hatası gelir.
- **`spring.jpa.hibernate.ddl-auto=none`** — şema elle yönetilir (DBeaver).
  Şema değişikliği yapılırsa `db/schema.sql` yeniden dump'lanmalı:
  `pg_dump -U postgres --schema-only --no-owner --no-privileges uretim_takip > db/schema.sql`
- **JSON global SNAKE_CASE** (`spring.jackson.property-naming-strategy`) —
  DTO'daki `fullName` JSON'da `full_name` olur. Frontend buna göre yazılmıştır.
- **`server.forwarded-headers-strategy=framework`** — kaldırılırsa tünel/proxy
  arkasından login 403 verir.

## Yedekleme (2026-07-06'da eklendi — bir daha veri kaybı yaşanmasın)

- Günlük otomatik yedek: Görev Zamanlayıcı görevi **`ERP-DB-Yedek`** her gün 21:00'de
  `C:\erp-backup\erp-backup.cmd` çalıştırır → `%USERPROFILE%\erp-backups\uretim_takip_<tarih>.dump`
  (pg_dump -Fc, 14 gün saklanır). Parola script içinde DEĞİL, `secrets.properties`ten okunur.
- Elle yedek: `cmd /c C:\erp-backup\erp-backup.cmd`. Geri yükleme:
  `pg_restore -U postgres -d uretim_takip --clean --if-exists <dump>`.
- NOT: Task Scheduler'a `powershell -File` verme — kullanıcı profilindeki Türkçe "İ"
  harfi argümanda bozuluyor ve görev sessizce çalışmıyor; bu yüzden özel-karaktersiz
  `C:\erp-backup\` yolunda **.cmd** kullanılıyor. Görev değişikliğinde `Set-ScheduledTask`
  yerine `Unregister`+`Register` ile temiz kur.
- DBeaver'da toplu DELETE/TRUNCATE öncesi mutlaka yedek al (WAL wal_level=replica,
  arşiv yok — kurtarma zor).
- NOT (2026-07-08): Teknik resimler (bom_documents.data bytea, 50MB/dosya)
  DB'de saklanır — dump boyutu dosyalarla birlikte büyür; pg_dump -Fc
  sıkıştırması var ama disk alanını ara ara kontrol et.

## Mimari

- **Backend**: Spring Boot 3.4, Java 21. Her modül aynı desen:
  Entity → Repository → Service → Controller → dto/Request+Response.
  Tüm cevaplar `ApiResponse{success, message, data}` sarmalayıcısında.
  Hatalar `common/exception/GlobalExceptionHandler`'da toplanır.
  Auth: JWT (`/api/auth/login`, 24 saat). `PasswordMigrationRunner` açılışta
  düz metin şifreleri BCrypt'e çevirir.
- **Frontend**: TEK DOSYA — `src/main/resources/static/index.html` (~6700 satır,
  vanilla JS). Supabase'den taşındı; **API adapter katmanı** ~1280-1700 satırları
  arasında: `TABLE_ENDPOINTS` (tablo adı → REST endpoint), `parsePgQuery`
  (PostgREST sorgu sözdizimini client-side filtre/sıralamaya çevirir),
  `FIELD_XLATE` (alan adı çevirileri + UUID↔isim join'leri).
  **Backend'de endpoint/alan değişirse bu adapter da güncellenmeli.**
- Entity'lerdeki doc comment'ler DB şemasını belgeler — şema sorusu olursa önce oraya bak.
- **XSS / innerHTML kaçırma (E1 turu):** Kullanıcı verisi taşıyan HER yeni
  veya değişen innerHTML şablonunda `h\`\`` tagged-template kullan (index.html
  ~2050'de tanımlı): `h\`<div>${x}</div>\`` interpolasyonları OTOMATİK
  esc'ler. Ham HTML için `${raw(html)}`, iç içe listelerde `${arr.map(x=>h\`…\`)}`
  (`.join('')` YOK — h array'i kendi birleştirir), `onclick="fn('${…}')"` gibi
  JS-in-attribute bağlamında `${raw(ea(x))}` (h'in HTML-esc'i JS'i bozar; sabit/
  UUID argümanlar güvenli). Elle `esc()` hâlâ geçerli ama yeni kodda `h\`\``
  tercih edilir (esc-unutma riskini yapısal olarak kaldırır). Geçiş TAMAM
  (2026-07-08): tüm liste/kart/ağaç render'ları h``'de. Bilinçli esc'li kalanlar:
  renderMrp/renderMrpParams (onclick içinde `=>` — h'e alma), modal formlar ve
  PDF/print (window.open + document.write, ayrı doküman). Dönüştürülen render'lar
  `node scripts/verify-h-render.js` ile XSS-regresyona karşı korunur (yeni
  fonksiyon dönüştükçe oraya senaryo ekle); `esc`/`raw`/`h` mekanizması
  e2e-test.js'te de birim-testli (ikisi index.html ile AYNI tutulmalı).
  Dikkat: h`` içinde boolean interpolasyonu `''` basar — onclick'e boolean
  geçerken `${raw(String(!!v))}` kullan.

## Sunucuyu Yeniden Başlatma (TUZAK — 2026-07-09)

`./mvnw spring-boot:run` bir ALT java süreci fork eder. Maven'i kapatmak (Ctrl+C
veya görevi durdurmak) bu java'yı ÖLDÜRMEZ; 8080'i tutmaya devam eder ve yeni
sunucu "Port 8080 was already in use" ile düşer. Bu durumda `mvnw compile` yeni
`index.html`'i `target/classes`'a kopyaladığından **FRONTEND YENİ ama BACKEND
ESKİ** olur — testler yanıltıcı sonuç verir (backend düzeltmesi "çalışmıyor"
görünür). Java'yı mutlaka öldür:

```
MSYS_NO_PATHCONV=1 taskkill /F /IM java.exe   # Git Bash: /F'i F:/ yapmasın
```

## Çalışma Düzeni

- Tek geliştirici → doğrudan `master`'a commit edilir; büyük/riskli işlerde
  feature branch açılabilir.
- Fikir/test notları ikinci kişiden not olarak gelir (veya GitHub Issues);
  hata bildirimlerinde ekran + adımlar + Console hatası istenir.
- Commit'lerden önce `./mvnw compile` geçmeli; frontend değişikliğinde
  `node` ile script bloğu syntax kontrolü yapılabilir.
- Backend'de endpoint/alan değişince index.html'deki adapter (TABLE_ENDPOINTS /
  FIELD_XLATE) aynı commit'te güncellenmeli.

## Test

- Test kullanıcısı: `testdev` / `test1234` (developer rolü).
- API smoke: login → token → `GET /api/parts` vb. (`Authorization: Bearer <token>`).
- DB bütünlük taraması örnekleri git geçmişinde (sahipsiz kayıt/şifre kontrolleri).
- **Üç katmanlı otomatik test** (hepsi sunucu 8080'de çalışırken):
  1. `node scripts/e2e-test.js <token>` — iş kuralı senaryoları CANLI backend'e
     karşı (yayınlama/adet, mal kabul, silme guard'ları K1/O1/O4/O5/U1, rol
     denetimi K3, atomik sayaç U4 vb.). index.html'den saf fonksiyonları `grab`
     ile çıkarıp Node'da koşar; izole `E2E-TEST-<ts>` projesi kurup temizler.
  2. `node scripts/verify-h-render.js` — dönüştürülmüş `h\`\`` render'larını DOM
     shim ile çalıştırıp XSS payload'unun kaçırıldığını doğrular (sunucu
     GEREKMEZ). Yeni fonksiyon `h\`\``'ye dönüştükçe buraya senaryo eklenir.
     Kardeşi `node scripts/verify-whxfer.js`: depolar arası aktarım çekirdeğini
     (whXferConfirm loose/tam/kısmi + B6 rollback) in-memory db shim'iyle koşar.
  3. `npx playwright test` (veya `npm run test:e2e`) — gerçek Chromium'da login
     akışı + XSS render kanıtı (`tests/critical.spec.js`), çift tıklama /
     mükerrer kayıt (`audit-doubleclick`), reddedilen silmede yalan başarı
     mesajı (`audit-delete-feedback`), sekme turu + uç girdiler
     (`audit-explore`). `@playwright/test` devDependency; `node_modules`
     gitignore'da, `npx playwright install chromium` ile tarayıcı kurulur.
     Config çalışan 8080'e bağlanır (webServer başlatmaz).
- **5. denetim turunun bekçileri** (2026-07-09) — değişiklik sonrası bunlar da koşsun:
  - `node scripts/verify-no-dup-fn.js` (sunucusuz) — aynı adlı iki üst-seviye
    fonksiyon sessizce birbirini EZER. Gerçek örnek: plaka kataloğunun
    `ssRenderList`'i searchSelect'inkini eziyor, TÜM aranabilir kutuların listesi
    hiç açılmıyordu. Testlerden kaçmıştı: `grab()` İLK tanımı alır, tarayıcı SONuncuyu
    çalıştırır. Bu yüzden `ss*` öneki searchSelect'e ait — katalog `sheet*` kullanır.
  - `node scripts/audit-refs.js` (sunucusuz) — mükerrer HTML id, inline handler'da
    tanımsız fonksiyon, aynı etikette mükerrer attribute (gerçek örnek: "Ürünü Sil"
    düğmesinde iki `style=`, ikincisi yok sayılıyordu).
  - `node scripts/audit-schema.js` (sunucusuz) — `db/schema.sql` ↔ `@Entity` kolon
    uyumu. `ddl-auto=none` olduğundan uyumsuzluk ÇALIŞMA ANINDA patlar.
  - `node scripts/audit-orphans.js` (sunucu gerekir, SALT OKUNUR) — canlı veride
    sahipsiz kayıt: projeye ADIYLA bağlı tablolar (FK yok, K2 riski) + UUID bağları
    + ağaç bütünlüğü.
  - `node scripts/verify-authz.js` (sunucu gerekir) — kısıtlı kullanıcı yıkıcı
    uçlara erişemiyor mu (403), okuma açık mı, developer yazabiliyor mu.
  - `node scripts/verify-disabled-user.js` (sunucu gerekir) — pasife alınan
    kullanıcının token'ı ANINDA geçersiz mi.
- **7. tur bekçileri** (2026-07-10):
  - `node scripts/verify-mip.js` (sunucusuz) — MİP hesap çekirdeği: eşleştirme
    (kod > ad), tür filtresi, `custom_*`/`resolved_*` override, aynı kodun adet
    toplamı, durum önceliği (DONE > RESERVED > FROM_STOCK > WAITING > SUPPLY >
    MISSING), sızıntı (başka proje / CANCELLED), Aşama 2 rezervasyon muhasebesi
    (APPROVED/PARTIAL approved_qty karşılanmış sayılır — OUT stoğu zaten
    düşürdüğünden çifte sayım yok; REQUESTED missing'i değiştirmez) +
    `mipReservePlan` dağıtımı.
  - `node scripts/verify-opdef-cascade.js` (sunucu gerekir) — işlem tanımının
    bölüm alanı + kod değişince ağaç kodlarının yeniden inşası.
  - `node scripts/audit-test-leftovers.js` (sunucu gerekir, SALT OKUNUR) — test
    öneki (`E2E`, `AUDIT`, `MIP`…) taşıyan artık kayıt kalmış mı. Bir test
    scriptinin temizliği sızdırıyorsa bunu yakalar.
- **Test temizliği tuzağı:** `dbDelete(...).catch(()=>{})` ile yutulan hatalar
  sessiz sızıntı yapar. `BomProductService` parçası olan ürünü SİLMEZ → şablon
  parçaları önce yapraktan köke silinmeli. Temizlik doğrulaması silinen HER
  tabloyu saymalı; e2e-test.js `bom_products`/`bom_parts` saymadığı için
  "0 artık kayıt" derken 33 ürün + 160 parça biriktirmişti (2026-07-10).
- **Ağaç parçalarında override kuralı:** `project_bom_parts.custom_code`/`custom_qty`
  birer OVERRIDE'dır; boşsa etkin değer bağlı şablon parçasından türetilir
  (`ProjectBomPartService.effectiveCode`). **Boş `custom_code`'un üzerine yazma** —
  parçanın taban kodu yok olur. Okurken `custom_x || resolved_x` deseni kullan
  (`pbomPublishParts`, `mipCodeOf`).
- **İşlem kodu soneki:** işlem kodları parça kodunun sonuna sırayla eklenir
  (`GP-001` + `WLD` + `PNT`). Bir işlem kaldırılır/değişirse kodu `rebuildCodeWithOps`
  (frontend) / `BomOperationService.rebuildCode` (backend) ile yeniden inşa et —
  "sonekle bitiyorsa kes" yanlıştır, ortadaki işlemde kalıntı bırakır. İkisi senkron.
- **Çift tıklama:** yeni bir kaydetme butonu eklerken `btnBusy(id)` helper'ını
  kullan (index.html ~2175). Kilit yoksa ikinci tıklama birinci istek dönmeden
  gider ve MÜKERRER kayıt oluşur; `findSiblingDup` gibi yerel-dizi kontrolleri
  bunu yakalayamaz.
- **Silme sonucu:** `dbDelete`/`dbUpdate` başarısızlıkta `false` döner ve hata
  toast'ını KENDİ gösterir. Dönüşü mutlaka kontrol et (`const ok = ...; if(!ok) return;`)
  — yoksa reddedilen silmede UI "silindi" deyip kaydı listeden kaldırır.
- `esc`/`raw`/`h` mekanizması index.html + e2e-test.js + verify-h-render.js'te
  üç yerde tanımlı; değişirse ÜÇÜNÜ de senkron tut.
