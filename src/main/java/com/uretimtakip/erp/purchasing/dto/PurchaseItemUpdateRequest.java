package com.uretimtakip.erp.purchasing.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PurchaseItem PARTIAL update icin request DTO.
 *
 * bom-parts'taki desenle ayni: hicbir alan zorunlu degil, sadece
 * gonderilen (non-null) alanlar degisir. Frontend tipik olarak tekil
 * degisiklik yollar:
 *   {"status": "ORDERED"}                    - durum ilerletme
 *   {"supplier": "X Metal", "unit_price": 12.5} - ticari bilgi girisi
 *   {"expected_date": "2026-08-01"}          - termin guncelleme
 *
 * PRESENCE TAKIPLI ALANLAR (explicit null = temizle):
 *   expected_date, supplier, unit_price, notes
 *
 * projectName ve projectBomPartId IMMUTABLE - gelse de service ignore eder.
 * Durum gecis damgalari (ordered_at/received_at) service'te atilir.
 */
@Getter
@Setter
@NoArgsConstructor
public class PurchaseItemUpdateRequest {

    @Size(max = 200, message = "Ad en fazla 200 karakter olabilir")
    private String name;

    @Size(max = 100, message = "Kod en fazla 100 karakter olabilir")
    private String code;

    @PositiveOrZero(message = "Miktar 0 veya pozitif olmali")
    private BigDecimal quantity;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;

    @Size(max = 150, message = "Materyal en fazla 150 karakter olabilir")
    private String material;

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 150, message = "Tedarikci en fazla 150 karakter olabilir")
    private String supplier;

    @Setter(lombok.AccessLevel.NONE)
    private boolean supplierPresent;

    public void setSupplier(String supplier) {
        this.supplier = supplier;
        this.supplierPresent = true;
    }

    @Setter(lombok.AccessLevel.NONE)
    @PositiveOrZero(message = "Birim fiyat 0 veya pozitif olmali")
    private BigDecimal unitPrice;

    @Setter(lombok.AccessLevel.NONE)
    private boolean unitPricePresent;

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.unitPricePresent = true;
    }

    @Size(max = 10, message = "Para birimi en fazla 10 karakter olabilir")
    private String currency;

    @Setter(lombok.AccessLevel.NONE)
    private LocalDate expectedDate;

    @Setter(lombok.AccessLevel.NONE)
    private boolean expectedDatePresent;

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
        this.expectedDatePresent = true;
    }

    @Size(max = 20, message = "Durum en fazla 20 karakter olabilir")
    private String status;

    @Setter(lombok.AccessLevel.NONE)
    private String notes;

    @Setter(lombok.AccessLevel.NONE)
    private boolean notesPresent;

    public void setNotes(String notes) {
        this.notes = notes;
        this.notesPresent = true;
    }
}
