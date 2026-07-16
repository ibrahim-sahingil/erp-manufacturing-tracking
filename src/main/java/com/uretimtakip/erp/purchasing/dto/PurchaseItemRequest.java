package com.uretimtakip.erp.purchasing.dto;

import jakarta.validation.constraints.NotBlank;
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
 * PurchaseItem CREATE icin request DTO.
 *
 * Ornek (elle giris):
 *   { "project_name": "Munih PracticaPark", "name": "M12 civata", "quantity": 200, "unit": "adet" }
 *
 * Ornek (urun agacindan ice aktarma - frontend parca basina bir POST atar):
 *   { "project_name": "Munih PracticaPark", "project_bom_part_id": "uuid",
 *     "name": "Asansor Direk Sabitleme Platin", "code": "PC-Sac-01",
 *     "quantity": 2, "unit": "adet", "material": "St-37" }
 *
 * status verilmezse PLANNED baslar. IN_STOCK ile de olusturulabilir
 * (fabrikada zaten var - satin alma gerektirmez).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItemRequest {

    @NotBlank(message = "Proje adi bos olamaz")
    @Size(max = 100, message = "Proje adi en fazla 100 karakter olabilir")
    private String projectName;

    /** Urun agacindan ice aktarilan kalemlerde kaynak parca referansi. */
    private UUID projectBomPartId;

    @NotBlank(message = "Ad bos olamaz")
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

    @Size(max = 150, message = "Tedarikci en fazla 150 karakter olabilir")
    private String supplier;

    @PositiveOrZero(message = "Birim fiyat 0 veya pozitif olmali")
    private BigDecimal unitPrice;

    @Size(max = 10, message = "Para birimi en fazla 10 karakter olabilir")
    private String currency;

    private LocalDate expectedDate;

    @Size(max = 20, message = "Durum en fazla 20 karakter olabilir")
    private String status;

    private String notes;

    @Size(max = 150, message = "Olusturan en fazla 150 karakter olabilir")
    private String createdBy;

    /**
     * (10. tur M5 hotfix) MIP "Havuza Aktar" karari kalemi dogrudan kesim
     * planlama havuzunda olusturur. Bu alan yalniz update DTO'sunda vardi;
     * create'te sessizce dusuyordu ve kalem havuz filtresine girmiyordu.
     */
    private Boolean needsPlanning;

    /**
     * (13. tur madde 2) Satin Alma listesi gorunurlugu. null/verilmezse TRUE
     * (elle giris eski davranis); MIP havuz/MRP plan akislari false gonderir.
     * CREATE ve UPDATE DTO'larinin IKISINDE de olmali (needs_planning dersi).
     */
    private Boolean sentToPurchasing;
}
