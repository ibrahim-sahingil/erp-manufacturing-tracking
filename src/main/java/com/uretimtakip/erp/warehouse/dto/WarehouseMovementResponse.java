package com.uretimtakip.erp.warehouse.dto;

import com.uretimtakip.erp.warehouse.WarehouseMovement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WarehouseMovement API cevabi. JSON'da global SNAKE_CASE ile doner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseMovementResponse {

    private UUID id;
    private UUID warehouseId;
    private UUID purchaseItemId;
    private UUID deliveryNoteId;
    private UUID reservationId;
    private UUID shipmentPackageId;
    private String itemName;
    private String itemCode;
    private String movementType;
    private BigDecimal quantity;
    private String unit;
    private String sourceType;
    private String performedBy;
    private String notes;
    private LocalDateTime createdAt;

    public static WarehouseMovementResponse fromEntity(WarehouseMovement e) {
        return WarehouseMovementResponse.builder()
                .id(e.getId())
                .warehouseId(e.getWarehouseId())
                .purchaseItemId(e.getPurchaseItemId())
                .deliveryNoteId(e.getDeliveryNoteId())
                .reservationId(e.getReservationId())
                .shipmentPackageId(e.getShipmentPackageId())
                .itemName(e.getItemName())
                .itemCode(e.getItemCode())
                .movementType(e.getMovementType())
                .quantity(e.getQuantity())
                .unit(e.getUnit())
                .sourceType(e.getSourceType())
                .performedBy(e.getPerformedBy())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
