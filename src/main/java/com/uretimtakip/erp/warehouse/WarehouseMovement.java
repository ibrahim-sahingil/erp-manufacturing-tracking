package com.uretimtakip.erp.warehouse;

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
 * warehouse_movements tablosunun Java karsiligi.
 *
 * Depo giris/cikis hareket defteri. Stok AYRI TABLODA TUTULMAZ:
 * bir deponun stogu = SUM(IN) - SUM(OUT) (frontend hesaplar).
 * Hareket kaydi DUZELTILMEZ (PUT yok) - yanlis kayit silinip yeniden girilir.
 *
 * KAYNAK TIPLERI (source_type):
 *   MANUAL            - depo yonetimi ekranindan elle giris/cikis
 *   PURCHASE_TRANSFER - satin alma kaleminin "Depoya Aktar" adimi
 *                       (purchase_item_id dolu; geri alinirsa OUT yazilir)
 *   GOODS_RECEIPT     - QR mal kabul (GELECEK MODUL icin rezerve, henuz kullanilmiyor)
 *
 * item_name/item_code SNAPSHOT'tir: kaynak satin alma kalemi silinse de
 * (purchase_item_id SET NULL olur) hareket gecmisi anlamli kalir.
 *
 * DB Sema:
 *   id               uuid          (BaseEntity'den)
 *   warehouse_id     uuid          NOT NULL (FK -> warehouses, ON DELETE RESTRICT)
 *   purchase_item_id uuid          NULL (FK -> purchase_items, ON DELETE SET NULL)
 *   item_name        varchar(200)  NOT NULL (snapshot)
 *   item_code        varchar(100)  NULL (snapshot)
 *   movement_type    varchar(10)   NOT NULL CHECK (IN/OUT)
 *   quantity         numeric(15,4) NOT NULL CHECK (> 0)
 *   unit             varchar(20)   DEFAULT 'adet'
 *   source_type      varchar(30)   DEFAULT 'MANUAL' NOT NULL (CHECK)
 *   performed_by     varchar(150)  NULL (kullanici adi, join'siz)
 *   notes            text          NULL
 *   created_at       timestamp     (BaseEntity'den)
 * Index: warehouse_id, created_at DESC.
 */
@Entity
@Table(name = "warehouse_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseMovement extends BaseEntity {

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "purchase_item_id")
    private UUID purchaseItemId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "movement_type", nullable = false, length = 10)
    private String movementType;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "adet";

    @Column(name = "source_type", nullable = false, length = 30)
    @Builder.Default
    private String sourceType = "MANUAL";

    @Column(name = "performed_by", length = 150)
    private String performedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
