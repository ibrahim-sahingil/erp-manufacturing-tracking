package com.uretimtakip.erp.shipment.dto;

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
 * ShipmentPackageItem CREATE request DTO (13. tur madde 4).
 *
 * item_name/item_code SNAPSHOT olarak frontend'den gelir (surukle-birak
 * aninda agac dugumunden kopyalanir). Miktar paketler arasi bolunebilir —
 * ayni parca icin farkli paketlerde ayri satirlar acilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackageItemRequest {

    @NotNull(message = "Paket id bos olamaz")
    private UUID packageId;

    private UUID partId;

    private UUID projectBomPartId;

    @NotBlank(message = "Parca adi bos olamaz")
    @Size(max = 200, message = "Parca adi en fazla 200 karakter olabilir")
    private String itemName;

    @Size(max = 100, message = "Parca kodu en fazla 100 karakter olabilir")
    private String itemCode;

    @NotNull(message = "Miktar bos olamaz")
    @Positive(message = "Miktar pozitif olmali")
    private BigDecimal quantity;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;
}
