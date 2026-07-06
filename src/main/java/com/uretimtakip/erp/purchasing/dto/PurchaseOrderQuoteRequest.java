package com.uretimtakip.erp.purchasing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * PurchaseOrderQuote CREATE icin request DTO.
 *
 * Ornek:
 *   { "purchase_order_id": "uuid", "supplier_name": "X Metal A.S.",
 *     "total_price": 12500, "currency": "TRY", "delivery_date": "2026-08-01" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderQuoteRequest {

    @NotNull(message = "Siparis grubu secilmeli")
    private UUID purchaseOrderId;

    @NotBlank(message = "Firma adi bos olamaz")
    @Size(max = 150, message = "Firma adi en fazla 150 karakter olabilir")
    private String supplierName;

    @Size(max = 200, message = "Iletisim bilgisi en fazla 200 karakter olabilir")
    private String contactInfo;

    @PositiveOrZero(message = "Toplam fiyat 0 veya pozitif olmali")
    private BigDecimal totalPrice;

    @Size(max = 10, message = "Para birimi en fazla 10 karakter olabilir")
    private String currency;

    private LocalDate deliveryDate;

    private String notes;
}
