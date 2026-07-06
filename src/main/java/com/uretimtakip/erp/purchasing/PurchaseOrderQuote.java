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
import java.util.UUID;

/**
 * purchase_order_quotes tablosunun Java karsiligi.
 *
 * Bir toplu siparis grubuna (purchase_orders) girilen firma teklifi.
 * Onay adiminda biri secilir (purchase_orders.selected_quote_id);
 * SECILMEYEN tekliflere rejection_reason yazilir (arkadas istegi:
 * "diger firmalar neden tercih edilmedigi yazilsin").
 *
 * Grubun SECILI teklifi silinemez (once onay geri alinmali).
 *
 * DB Sema:
 *   id                uuid          (BaseEntity'den)
 *   purchase_order_id uuid          NOT NULL (FK -> purchase_orders, ON DELETE CASCADE)
 *   supplier_name     varchar(150)  NOT NULL
 *   contact_info      varchar(200)  NULL (telefon/e-posta/yetkili)
 *   total_price       numeric(15,2) NULL (grubun toplam teklif fiyati)
 *   currency          varchar(10)   DEFAULT 'TRY'
 *   delivery_date     date          NULL (taahhut edilen teslim)
 *   notes             text          NULL
 *   rejection_reason  text          NULL (elenme gerekcesi - onayda doldurulur)
 *   created_at        timestamp     (BaseEntity'den)
 * Index: purchase_order_id.
 */
@Entity
@Table(name = "purchase_order_quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderQuote extends BaseEntity {

    @Column(name = "purchase_order_id", nullable = false)
    private UUID purchaseOrderId;

    @Column(name = "supplier_name", nullable = false, length = 150)
    private String supplierName;

    @Column(name = "contact_info", length = 200)
    private String contactInfo;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
