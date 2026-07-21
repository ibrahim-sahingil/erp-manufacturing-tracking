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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * purchase_orders tablosunun Java karsiligi.
 *
 * Toplu siparis grubu: birden fazla satin alma kalemi (purchase_items.purchase_order_id)
 * gruplanir, gruba firma teklifleri (purchase_order_quotes) girilir, biri secilerek
 * onaylanir ve grup TEK siparis olarak verilir.
 *
 * DURUM AKISI:
 *   DRAFT     - grup olusturuldu, kalem eklenip cikarilabilir, teklifler girilir
 *   APPROVED  - kazanan teklif secildi (approval_note zorunlu, elenen tekliflere
 *               rejection_reason yazilir); DRAFT'a geri alinabilir
 *   ORDERED   - siparis verildi; uye PLANNED kalemler topluca ORDERED yapilir ve
 *               supplier = kazanan firma atanir. APPROVED'a geri alinabilir
 *               (kalemler PLANNED'a doner, damgalar korunur)
 *   CANCELLED - iptal; uyelik serbest birakilir. ORDERED grup once geri alinmali.
 *
 * DB Sema:
 *   id                uuid         (BaseEntity'den)
 *   name              varchar(200) NOT NULL
 *   status            varchar(20)  DEFAULT 'DRAFT' (CHECK)
 *   selected_quote_id uuid         NULL (FK -> purchase_order_quotes, ON DELETE SET NULL)
 *   approval_note     text         NULL (onay aciklamasi - onayda zorunlu)
 *   approved_by       varchar(150) NULL
 *   approved_at       timestamp    NULL
 *   ordered_at        timestamp    NULL
 *   created_by        varchar(150) NULL
 *   created_at        timestamp    (BaseEntity'den)
 */
@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // (16. tur M1b — arkadas: "her siparise otomatik mukerrersiz kod") backend
    // uretir: SIP-<yil>-<sira>; asil garanti DB UNIQUE (nextNoteNo deseni)
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "selected_quote_id")
    private UUID selectedQuoteId;

    @Column(name = "approval_note", columnDefinition = "TEXT")
    private String approvalNote;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "created_by", length = 150)
    private String createdBy;
}
