package com.uretimtakip.erp.shipment;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * shipment_packages tablosunun Java karsiligi (13. tur madde 4 — Sevkiyat).
 *
 * Sevkiyat paketleme kaydi: sevkiyatci paket kutusu acar (ad + olculer +
 * agirlik), urun agacindan parcalari surukleyip icine koyar
 * (shipment_package_items), paketi kapatir, araca yukler, sevk eder.
 *
 * DURUM AKISI (komsu gecisler iki yonlu — geri alma icin):
 *   OPEN    - acik: satir eklenip cikarilabilir, olculer duzenlenebilir
 *   CLOSED  - kapatildi: packed_by ZORUNLU, packed_at damgalanir; icerik kilitli
 *   LOADED  - araca yuklendi: delivery_note_id dolu olmali (irsaliyeye bagli)
 *   SHIPPED - sevk edildi (irsaliye SHIPPED olunca frontend toplu gecirir)
 * DB CHECK ayni listeyi zorlar; gecis whitelist'i ShipmentPackageService'te.
 *
 * PACKAGE_NO: "PKT-<yil>-<sira>" backend'de uretilir (create), degistirilemez;
 * "hicbir paket mukerrer olmayacak" sarti DB UNIQUE ile garanti.
 *
 * DB Sema:
 *   id               uuid          (BaseEntity'den)
 *   package_no       varchar(30)   NOT NULL UNIQUE
 *   project_name     varchar(100)  NOT NULL (orders.project_name deseni)
 *   name             varchar(150)  NULL (pakete verilen ad, ör. "Çelik Konstrüksiyon")
 *   length_cm        numeric(10,2) NULL — uzunluk
 *   width_cm         numeric(10,2) NULL — genislik
 *   height_cm        numeric(10,2) NULL — yukseklik
 *   weight_kg        numeric(12,3) NULL — BRUT agirlik
 *   net_weight_kg    numeric(12,3) NULL — NET agirlik (14. tur S3, ceki listesi)
 *   package_type     varchar(30)   DEFAULT 'PACKAGE' (CHECK: PACKAGE/BOX/PALLET/CRATE —
 *                                  14. tur S3, arkadasin ceki sablonundaki PACKAGE TYPE)
 *   warehouse_id     uuid          NULL (FK -> warehouses SET NULL; 14. tur S2:
 *                                  paketlenen parcalar istenilen depoda durur)
 *   status           varchar(20)   DEFAULT 'OPEN' (CHECK: OPEN/CLOSED/LOADED/SHIPPED)
 *   delivery_note_id uuid          NULL (FK -> delivery_notes SET NULL; araca yukleme bagi)
 *   packed_by        varchar(150)  NULL (paketleyen personel — CLOSED'da zorunlu)
 *   packed_at        timestamp     NULL (CLOSED'a gecince service damgalar)
 *   notes            text          NULL
 *   created_by       varchar(150)  NULL
 *   created_at       timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "shipment_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackage extends BaseEntity {

    @Column(name = "package_no", nullable = false, length = 30, unique = true)
    private String packageNo;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 10, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 12, scale = 3)
    private BigDecimal weightKg;

    /** (14. tur S3) NET agirlik — ceki listesinde brut/net ayrimi. */
    @Column(name = "net_weight_kg", precision = 12, scale = 3)
    private BigDecimal netWeightKg;

    /** (14. tur S3) Paket tipi (arkadasin sablonu: PACKAGE/BOX/...). */
    @Column(name = "package_type", nullable = false, length = 30)
    @Builder.Default
    private String packageType = "PACKAGE";

    /** (14. tur S2) Paketin durdugu depo — arkadas karari: "istenilen depoda dursun". */
    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "delivery_note_id")
    private UUID deliveryNoteId;

    @Column(name = "packed_by", length = 150)
    private String packedBy;

    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 150)
    private String createdBy;
}
