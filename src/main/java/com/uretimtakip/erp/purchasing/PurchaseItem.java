package com.uretimtakip.erp.purchasing;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * purchase_items tablosunun Java karsiligi.
 *
 * Satin alma planlamasi: siparise/projeye bagli urunler icin alinacak
 * malzemelerin listesi. Kalemler proje urun agacindan ice aktarilabilir
 * (projectBomPartId dolu) veya elle eklenebilir (null).
 *
 * DURUM AKISI:
 *   PLANNED      - planlandi (varsayilan)
 *   ORDERED      - tedarikciye siparis verildi (ordered_at damgalanir)
 *   RECEIVED     - teslim alindi (received_at damgalanir)
 *   IN_WAREHOUSE - uretim deposuna aktarildi (akisin sonu)
 *   IN_STOCK     - fabrikada zaten mevcut, satin alma gerektirmez
 *                  (PLANNED'den direkt gecilebilir, oradan IN_WAREHOUSE'a)
 *   CANCELLED    - iptal
 * DB'de CHECK constraint ayni listeyi zorlar.
 *
 * DB Sema:
 *   id                  uuid          (BaseEntity'den)
 *   project_name        varchar(100)  NOT NULL (orders.project_name / project_bom.project_name ile ayni desen)
 *   project_bom_part_id uuid          NULL (FK -> project_bom_parts, ON DELETE SET NULL)
 *   name                varchar(200)  NOT NULL
 *   code                varchar(100)  NULL
 *   quantity            numeric(15,4) DEFAULT 1
 *   unit                varchar(20)   DEFAULT 'adet'
 *   material            varchar(150)  NULL
 *   supplier            varchar(150)  NULL
 *   unit_price          numeric(15,2) NULL
 *   currency            varchar(10)   DEFAULT 'TRY'
 *   expected_date       date          NULL (termin)
 *   status              varchar(20)   DEFAULT 'PLANNED' (CHECK)
 *   warehouse_id        uuid          NULL (FK -> warehouses, ON DELETE SET NULL;
 *                                     IN_WAREHOUSE durumunda hangi depoda oldugu)
 *   purchase_order_id   uuid          NULL (FK -> purchase_orders, ON DELETE SET NULL;
 *                                     kalemin bagli oldugu toplu siparis grubu)
 *   notes               text          NULL
 *   needs_planning      boolean       DEFAULT false NOT NULL (#10 MRP havuzu isareti)
 *   stock_plan_id       uuid          NULL (FK -> purchase_items self, ON DELETE SET NULL;
 *                                     kaynak kalemin baglandigi plaka/profil kalemi)
 *   ordered_at          timestamp     NULL
 *   received_at         timestamp     NULL
 *   received_by         varchar(150)  NULL (mal kabulu yapan hesap, 4. tur #3)
 *   received_qty        numeric(15,4) DEFAULT 0 NOT NULL (depoya kabul edilen toplam adet)
 *   returned_qty        numeric(15,4) DEFAULT 0 NOT NULL (tedarikciye iade edilen toplam adet)
 *   created_by          varchar(150)  NULL
 *   created_at          timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "purchase_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItem extends BaseEntity {

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "project_bom_part_id")
    private UUID projectBomPartId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "quantity", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "adet";

    @Column(name = "material", length = 150)
    private String material;

    @Column(name = "supplier", length = 150)
    private String supplier;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PLANNED";

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "purchase_order_id")
    private UUID purchaseOrderId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Ihtiyac planlama havuzunda mi (#10 plaka MRP). */
    @Column(name = "needs_planning", nullable = false)
    @Builder.Default
    private Boolean needsPlanning = false;

    /** Bu kalemin malzemesinin cikacagi plaka/profil kalemi (self-referans). */
    @Column(name = "stock_plan_id")
    private UUID stockPlanId;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /** Mal kabulu yapan hesap (4. tur #3 — kim teslim aldi). */
    @Column(name = "received_by", length = 150)
    private String receivedBy;

    /** Depoya kabul edilen toplam adet (kismi mal kabul, 4. tur #3). */
    @Column(name = "received_qty", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal receivedQty = BigDecimal.ZERO;

    /** Tedarikciye iade edilen toplam adet (yeniden beklenir, 4. tur #3). */
    @Column(name = "returned_qty", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal returnedQty = BigDecimal.ZERO;

    @Column(name = "created_by", length = 150)
    private String createdBy;
}
