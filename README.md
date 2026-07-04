# Manufacturing Tracking (ERP) System — Üretim Takip

Bu proje, endüstriyel üretim süreçlerinin dijital ortamda uçtan uca izlenebilirliğini
sağlamak amacıyla geliştirilmiş, QR kod destekli bir üretim takip sistemidir.
Sipariş → parça → bölüm → iş emri akışıyla imalat sürecini izler; BOM (ürün ağacı)
şablonları, proje bazlı BOM türetme, proje tarih planlama/revizyon ve yetki bazlı
kullanıcı yönetimi içerir.

## 🚀 Teknolojiler

- **Dil:** Java 21
- **Framework:** Spring Boot 3.4 (Web, Data JPA, Security + JWT, Validation), Lombok
- **Veritabanı:** PostgreSQL (şema elle yönetilir, `ddl-auto=none`)
- **Frontend:** Tek dosyalık SPA (`src/main/resources/static/index.html`) — backend ile aynı adresten servis edilir
- **Araçlar:** Maven, Hibernate, DBeaver, IntelliJ IDEA

## 🛠️ Temel Özellikler

- **Üretim Takibi:** İş emirlerinin, operasyonların ve üretim aşamalarının gerçek zamanlı yönetimi; QR kod ile sahadan üretim girişi.
- **Veri Güvenliği:** Spring Security + JWT ile rol/yetki tabanlı erişim kontrolü; şifreler BCrypt ile saklanır.
- **Gelişmiş Veri Yönetimi:** Katı UUID stratejisi ile yüksek veri bütünlüğü ve izlenebilirlik.
- **Hata Yönetimi:** Global Exception Handling ile standardize edilmiş API yanıtları (`{success, message, data}`, alan adları `snake_case`).

## 🏗️ Mimari Yapı

Proje, temiz kod prensiplerine uygun olarak **Katmanlı Mimari (Layered Architecture)** kullanılarak geliştirilmiştir:

- **Controller:** API uç noktalarının (endpoints) yönetimi.
- **Service:** İş mantığının (business logic) yürütülmesi.
- **Repository:** Spring Data JPA ile veritabanı etkileşimi.
- **DTO:** Veri transfer nesneleri ile güvenli veri iletimi.

## ⚙️ Kurulum

1. **PostgreSQL** kurun ve veritabanını oluşturun:
   ```sql
   CREATE DATABASE uretim_takip;
   ```
   Şemayı içe aktarın:
   ```
   psql -U postgres -d uretim_takip -f db/schema.sql
   ```

2. Proje kökünde **`secrets.properties`** dosyası oluşturun (git'e girmez):
   ```properties
   DB_PASSWORD=<postgres şifreniz>
   APP_JWT_SECRET=<en az 32 karakterlik rastgele anahtar>
   ```

3. Çalıştırın:
   ```
   ./mvnw spring-boot:run
   ```
   Uygulama: http://localhost:8080/ — ilk kullanıcıyı veritabanına ekleyin
   (düz metin şifre yazabilirsiniz; ilk açılışta otomatik BCrypt'e çevrilir):
   ```sql
   INSERT INTO users (id, full_name, role, is_active, username, password_hash, permissions)
   VALUES (gen_random_uuid(), 'Yönetici', 'developer', true, 'admin', 'sifreniz', '[]'::jsonb);
   ```

## 📌 Notlar

- **JDK 22+** ile derlerken `pom.xml`'deki `<proc>full</proc>` ayarı zorunludur
  (javac annotation processing'i varsayılan kapatır, Lombok çalışmaz).
- Kimlik doğrulama: `POST /api/auth/login` → Bearer token (24 saat geçerli).
