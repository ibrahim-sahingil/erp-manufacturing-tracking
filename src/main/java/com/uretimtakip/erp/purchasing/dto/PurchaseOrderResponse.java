package com.uretimtakip.erp.purchasing.dto;

import com.uretimtakip.erp.purchasing.PurchaseOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PurchaseOrder API cevabi. JSON'da global SNAKE_CASE ile doner.
 * Uye kalemler ve teklifler ayri endpoint'lerden okunur
 * (purchase-items.purchase_order_id / purchase-order-quotes?order=).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {

    private UUID id;
    private String name;
    private String code;   // (16. tur M1b)
    private String status;
    private UUID selectedQuoteId;
    private String approvalNote;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime orderedAt;
    private String createdBy;
    private LocalDateTime createdAt;

    public static PurchaseOrderResponse fromEntity(PurchaseOrder e) {
        return PurchaseOrderResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .status(e.getStatus())
                .selectedQuoteId(e.getSelectedQuoteId())
                .approvalNote(e.getApprovalNote())
                .approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt())
                .orderedAt(e.getOrderedAt())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
