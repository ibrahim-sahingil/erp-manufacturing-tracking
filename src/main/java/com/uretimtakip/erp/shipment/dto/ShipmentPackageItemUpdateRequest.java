package com.uretimtakip.erp.shipment.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * ShipmentPackageItem PARTIAL update DTO (13. tur madde 4).
 * Yalniz miktar duzeltilebilir (satirin kimligi/snapshot'i sabit);
 * paket OPEN degilse service reddeder.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackageItemUpdateRequest {

    @Positive(message = "Miktar pozitif olmali")
    private BigDecimal quantity;
}
