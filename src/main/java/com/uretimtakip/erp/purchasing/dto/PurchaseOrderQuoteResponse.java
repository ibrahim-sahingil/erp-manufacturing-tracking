package com.uretimtakip.erp.purchasing.dto;

import com.uretimtakip.erp.purchasing.PurchaseOrderQuote;
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
 * PurchaseOrderQuote API cevabi. JSON'da global SNAKE_CASE ile doner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderQuoteResponse {

    private UUID id;
    private UUID purchaseOrderId;
    private String supplierName;
    private String contactInfo;
    private BigDecimal totalPrice;
    private String currency;
    private LocalDate deliveryDate;
    private String notes;
    private String rejectionReason;
    private LocalDateTime createdAt;

    public static PurchaseOrderQuoteResponse fromEntity(PurchaseOrderQuote e) {
        return PurchaseOrderQuoteResponse.builder()
                .id(e.getId())
                .purchaseOrderId(e.getPurchaseOrderId())
                .supplierName(e.getSupplierName())
                .contactInfo(e.getContactInfo())
                .totalPrice(e.getTotalPrice())
                .currency(e.getCurrency())
                .deliveryDate(e.getDeliveryDate())
                .notes(e.getNotes())
                .rejectionReason(e.getRejectionReason())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
