package com.uretimtakip.erp.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DeliveryNoteItem CREATE icin request DTO.
 * warehouse_id dolu = depodan kalem (sevkte OUT hareketi yazilir);
 * null = serbest satir (stok takibi yok).
 * Kalem yalniz DRAFT irsaliyeye eklenebilir (service dogrular).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNoteItemRequest {

    @NotNull(message = "deliveryNoteId zorunlu")
    private UUID deliveryNoteId;

    private UUID warehouseId;

    @NotBlank(message = "Kalem adi bos olamaz")
    @Size(max = 200, message = "Kalem adi en fazla 200 karakter olabilir")
    private String itemName;

    @Size(max = 100, message = "Kod en fazla 100 karakter olabilir")
    private String itemCode;

    @Positive(message = "Miktar pozitif olmali")
    private BigDecimal quantity;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;

    @Size(max = 300, message = "Not en fazla 300 karakter olabilir")
    private String notes;
}
