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
