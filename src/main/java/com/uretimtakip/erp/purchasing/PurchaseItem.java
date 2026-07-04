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
 *   notes               text          NULL
 *   ordered_at          timestamp     NULL
 *   received_at         timestamp     NULL
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

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "created_by", length = 150)
    private String createdBy;
}
