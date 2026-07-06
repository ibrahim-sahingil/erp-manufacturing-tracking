package com.uretimtakip.erp.purchasing.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PurchaseOrderQuote PARTIAL update icin request DTO.
 *
 * PRESENCE TAKIPLI ALANLAR (explicit null = temizle):
 *   total_price, delivery_date, contact_info, notes, rejection_reason
 *
 * Tipik kullanim:
 *   {"total_price": 11800}                       - fiyat guncelleme
 *   {"rejection_reason": "Termin cok gec"}       - onayda elenme gerekcesi
 *   {"rejection_reason": null}                   - onay geri alininca temizleme
 */
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderQuoteUpdateRequest {

    @Size(max = 150, message = "Firma adi en fazla 150 karakter olabilir")
    private String supplierName;

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 200, message = "Iletisim bilgisi en fazla 200 karakter olabilir")
    private String contactInfo;

    @Setter(lombok.AccessLevel.NONE)
    private boolean contactInfoPresent;

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
        this.contactInfoPresent = true;
    }

    @Setter(lombok.AccessLevel.NONE)
    @PositiveOrZero(message = "Toplam fiyat 0 veya pozitif olmali")
    private BigDecimal totalPrice;

    @Setter(lombok.AccessLevel.NONE)
    private boolean totalPricePresent;

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
        this.totalPricePresent = true;
    }

    @Size(max = 10, message = "Para birimi en fazla 10 karakter olabilir")
    private String currency;

    @Setter(lombok.AccessLevel.NONE)
    private LocalDate deliveryDate;

    @Setter(lombok.AccessLevel.NONE)
    private boolean deliveryDatePresent;

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
        this.deliveryDatePresent = true;
    }

    @Setter(lombok.AccessLevel.NONE)
    private String notes;

    @Setter(lombok.AccessLevel.NONE)
    private boolean notesPresent;

    public void setNotes(String notes) {
        this.notes = notes;
        this.notesPresent = true;
    }

    @Setter(lombok.AccessLevel.NONE)
    private String rejectionReason;

    @Setter(lombok.AccessLevel.NONE)
    private boolean rejectionReasonPresent;

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        this.rejectionReasonPresent = true;
    }
}
