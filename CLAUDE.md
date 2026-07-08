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
     akışı + XSS render kanıtı (`tests/critical.spec.js`). `@playwright/test`
     devDependency; `node_modules` gitignore'da, `npx playwright install chromium`
     ile tarayıcı kurulur. Config çalışan 8080'e bağlanır (webServer başlatmaz).
- `esc`/`raw`/`h` mekanizması index.html + e2e-test.js + verify-h-render.js'te
  üç yerde tanımlı; değişirse ÜÇÜNÜ de senkron tut.
