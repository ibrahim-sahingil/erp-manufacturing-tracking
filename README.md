# Manufacturing Tracking (ERP) System - Backend

Bu proje, endüstriyel üretim süreçlerinin dijital ortamda uçtan uca izlenebilirliğini sağlamak amacıyla geliştirilmiş, ölçeklenebilir bir backend mimarisidir.

## 🚀 Teknolojiler
Dil:** Java 17+
Framework:** Spring Boot 3.x
Veritabanı:** PostgreSQL
Güvenlik:** Spring Security & JWT (JSON Web Token)
Araçlar:** Maven, Hibernate, DBeaver, IntelliJ IDEA

## 🛠️ Temel Özellikler
Üretim Takibi:** İş emirlerinin, operasyonların ve üretim aşamalarının gerçek zamanlı yönetimi.
Veri Güvenliği:** Spring Security entegrasyonu ile rol tabanlı erişim kontrolü ve güvenli yetkilendirme.
Gelişmiş Veri Yönetimi:** Katı UUID (Universally Unique Identifier) stratejisi ile yüksek veri bütünlüğü ve izlenebilirlik.
Hata Yönetimi:** Global Exception Handling mimarisi ile standardize edilmiş API yanıtları.

## 🏗️ Mimari Yapı
Proje, temiz kod prensiplerine uygun olarak **Katmanlı Mimari (Layered Architecture)** kullanılarak geliştirilmiştir:
Controller:** API uç noktalarının (endpoints) yönetimi.
Service:** İş mantığının (business logic) yürütülmesi.
Repository:** Spring Data JPA ile veritabanı etkileşimi.
DTO:** Veri transfer nesneleri ile güvenli veri iletimi.

