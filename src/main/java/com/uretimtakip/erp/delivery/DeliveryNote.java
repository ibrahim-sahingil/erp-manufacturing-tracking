package com.uretimtakip.erp.delivery;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * delivery_notes tablosunun Java karsiligi.
 *
 * DAHILI sevk irsaliyesi kaydi + yazdirilabilir PDF. GIB e-Irsaliye
 * ENTEGRASYONU DEGILDIR (kullanici karari 2026-07-07); alan adlari
 * ileride bir entegratore baglanabilecek sekilde e-irsaliye formuyla
 * uyumlu tutuldu (TCKN/VKN, vergi dairesi, senaryo, tur).
 *
 * DURUM AKISI:
 *   DRAFT --(sevk et)--> SHIPPED --(geri al)--> DRAFT
 *   DRAFT --> CANCELLED (yalniz taslak iptal edilebilir)
 *   SHIPPED aninda frontend, depo bagli kalemler icin warehouse_movements'e
 *   OUT (source_type=DELIVERY, delivery_note_id dolu) yazar; geri alista
 *   ayni delivery_note_id'li hareketler silinir (sil+yeniden gir deseni).
 *
 * NOTE_NO: "IRS-<yil>-<sira>" backend'de uretilir (create), degistirilemez.
 *
 * DB Sema:
 *   id             uuid          (BaseEntity'den)
 *   note_no        varchar(30)   NOT NULL UNIQUE
 *   order_id       uuid          NULL (FK -> orders SET NULL, proje baglantisi)
 *   recipient_name varchar(200)  NOT NULL (Alici)
 *   tax_number     varchar(20)   NULL (TCKN/VKN)
 *   tax_office     varchar(100)  NULL (Vergi dairesi)
 *   address        text          NULL
 *   city           varchar(50)   NULL
 *   district       varchar(50)   NULL
 *   scenario       varchar(30)   DEFAULT 'TEMEL'
 *   note_type      varchar(30)   DEFAULT 'SEVK'
 *   carrier        varchar(150)  NULL (tasiyici / nakliye firmasi)
 *   vehicle_plate  varchar(20)   NULL (13. tur m4/F: arac plakasi)
 *   driver_name    varchar(150)  NULL (sofor)
 *   container_no   varchar(50)   NULL (konteyner no)
 *   tir_no         varchar(50)   NULL (TIR no)
 *   cargo_tracking_no varchar(100) NULL (kargo takip no)
 *   eta_date       date          NULL (tahmini varis)
 *   delivery_terms varchar(100)  NULL (15. tur Y2a: teslim kosulu, orn. "DPU - Dnipro")
 *   origin_country varchar(100)  NULL (15. tur Y2a: mensei, orn. "Turkey")
 *   status         varchar(20)   DEFAULT 'DRAFT' (CHECK: DRAFT/SHIPPED/CANCELLED)
 *   ship_date      date          NULL (fiili sevk tarihi)
 *   notes          text          NULL
 *   created_by     varchar(100)  NULL
 *   created_at     timestamp     (BaseEntity'den)
 *   shipped_at     timestamp     NULL (SHIPPED'a gecince damgalanir)
 */
@Entity
@Table(name = "delivery_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNote extends BaseEntity {

    @Column(name = "note_no", nullable = false, length = 30, unique = true)
    private String noteNo;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(name = "tax_number", length = 20)
    private String taxNumber;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "scenario", length = 30)
    @Builder.Default
    private String scenario = "TEMEL";

    @Column(name = "note_type", length = 30)
    @Builder.Default
    private String noteType = "SEVK";

    @Column(name = "carrier", length = 150)
    private String carrier;

    // (13. tur m4/F) Arac/yukleme bilgileri — Sistem.pdf "5. Arac bilgileri girilir"
    @Column(name = "vehicle_plate", length = 20)
    private String vehiclePlate;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "container_no", length = 50)
    private String containerNo;

    @Column(name = "tir_no", length = 50)
    private String tirNo;

    @Column(name = "cargo_tracking_no", length = 100)
    private String cargoTrackingNo;

    @Column(name = "eta_date")
    private LocalDate etaDate;

    // (15. tur Y2a) ceki listesi ust blogu: teslim kosulu + mensei irsaliye
    // basina girilir (Gonderen bloku company_settings'ten gelir)
    @Column(name = "delivery_terms", length = 100)
    private String deliveryTerms;

    @Column(name = "origin_country", length = 100)
    private String originCountry;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "ship_date")
    private LocalDate shipDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
}
