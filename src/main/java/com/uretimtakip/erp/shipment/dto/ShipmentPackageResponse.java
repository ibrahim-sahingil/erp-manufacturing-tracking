package com.uretimtakip.erp.shipment.dto;

import com.uretimtakip.erp.shipment.ShipmentPackage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** ShipmentPackage response DTO — JSON global snake_case ile doner. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackageResponse {

    private UUID id;
    private String packageNo;
    private String projectName;
    private String name;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal netWeightKg;
    private String packageType;
    private UUID warehouseId;
    private String status;
    private UUID deliveryNoteId;
    private String packedBy;
    private LocalDateTime packedAt;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;

    public static ShipmentPackageResponse fromEntity(ShipmentPackage e) {
        return ShipmentPackageResponse.builder()
                .id(e.getId())
                .packageNo(e.getPackageNo())
                .projectName(e.getProjectName())
                .name(e.getName())
                .lengthCm(e.getLengthCm())
                .widthCm(e.getWidthCm())
                .heightCm(e.getHeightCm())
                .weightKg(e.getWeightKg())
                .netWeightKg(e.getNetWeightKg())
                .packageType(e.getPackageType())
                .warehouseId(e.getWarehouseId())
                .status(e.getStatus())
                .deliveryNoteId(e.getDeliveryNoteId())
                .packedBy(e.getPackedBy())
                .packedAt(e.getPackedAt())
                .notes(e.getNotes())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
