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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * delivery_note_items tablosunun Java karsiligi.
 *
 * Irsaliyenin kalemleri. Iki tur kalem olabilir:
 *   - DEPODAN: warehouse_id dolu -> irsaliye SEVK edilince frontend o
 *     depodan OUT hareketi yazar (stok duser)
 *   - SERBEST: warehouse_id null -> stok takibi olmayan satir
 *     (hizmet, ambalaj, stoklanmayan malzeme vb.)
 *
 * item_name/item_code SNAPSHOT'tir (irsaliye belgesi sabit kalmali).
 * Kalemler yalniz DRAFT irsaliyede eklenip cikartilabilir.
 *
 * DB Sema:
 *   id               uuid          (BaseEntity'den)
 *   delivery_note_id uuid          NOT NULL (FK -> delivery_notes CASCADE)
 *   warehouse_id     uuid          NULL (FK -> warehouses SET NULL)
 *   item_name        varchar(200)  NOT NULL
 *   item_code        varchar(100)  NULL
 *   quantity         numeric(15,4) NOT NULL CHECK (> 0)
 *   unit             varchar(20)   DEFAULT 'adet'
 *   notes            varchar(300)  NULL
 *   created_at       timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "delivery_note_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNoteItem extends BaseEntity {

    @Column(name = "delivery_note_id", nullable = false)
    private UUID deliveryNoteId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "adet";

    @Column(name = "notes", length = 300)
    private String notes;
}
