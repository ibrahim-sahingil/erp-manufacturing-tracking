package com.uretimtakip.erp.shipment.dto;

import com.uretimtakip.erp.shipment.ShipmentPackageItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** ShipmentPackageItem response DTO — JSON global snake_case ile doner. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackageItemResponse {

    private UUID id;
    private UUID packageId;
    private UUID partId;
    private UUID projectBomPartId;
    private String itemName;
    private String itemCode;
    private BigDecimal quantity;
    private String unit;
    private LocalDateTime createdAt;

    public static ShipmentPackageItemResponse fromEntity(ShipmentPackageItem e) {
        return ShipmentPackageItemResponse.builder()
                .id(e.getId())
                .packageId(e.getPackageId())
                .partId(e.getPartId())
                .projectBomPartId(e.getProjectBomPartId())
                .itemName(e.getItemName())
                .itemCode(e.getItemCode())
                .quantity(e.getQuantity())
                .unit(e.getUnit())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
