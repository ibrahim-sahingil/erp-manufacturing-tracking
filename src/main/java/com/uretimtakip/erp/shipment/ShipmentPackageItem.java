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
import java.util.UUID;

/**
 * shipment_package_items tablosunun Java karsiligi (13. tur madde 4).
 *
 * Paket icerik satiri: urun agacindan pakete surukle-birakla konan parca.
 * Ayni parcanin adedi paketler arasinda BOLUNEBILIR (3 adet -> 2'si Paket 1,
 * 1'i Paket 2 = iki ayri satir). item_name/item_code SNAPSHOT'tir — agac
 * sonradan degisse de paket tarihcesi bozulmaz (delivery_note_items deseni).
 *
 * Satir ekleme/silme yalniz paket OPEN iken (ShipmentPackageItemService guard).
 *
 * DB Sema:
 *   id                  uuid          (BaseEntity'den)
 *   package_id          uuid          NOT NULL (FK -> shipment_packages CASCADE)
 *   part_id             uuid          NULL (uretim parcasi bagi, parts SET NULL)
 *   project_bom_part_id uuid          NULL (agac dugumu bagi, SET NULL)
 *   item_name           varchar(200)  NOT NULL (SNAPSHOT)
 *   item_code           varchar(100)  NULL (SNAPSHOT)
 *   quantity            numeric(15,4) NOT NULL CHECK (> 0)
 *   unit                varchar(20)   DEFAULT 'adet'
 *   created_at          timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "shipment_package_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackageItem extends BaseEntity {

    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    @Column(name = "part_id")
    private UUID partId;

    @Column(name = "project_bom_part_id")
    private UUID projectBomPartId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "adet";
}
