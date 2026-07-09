package com.uretimtakip.erp.warehouse.dto;

import com.uretimtakip.erp.warehouse.WarehouseReservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WarehouseReservation API cevabi. JSON'da global SNAKE_CASE ile doner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseReservationResponse {

    private UUID id;
    private String projectName;
    private UUID warehouseId;
    private String itemName;
    private String itemCode;
    private BigDecimal requestedQty;
    private BigDecimal approvedQty;
    private String unit;
    private String status;
    private String shortageReason;
    private String requestedBy;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String notes;
    private LocalDateTime createdAt;

    public static WarehouseReservationResponse fromEntity(WarehouseReservation e) {
        return WarehouseReservationResponse.builder()
                .id(e.getId())
                .projectName(e.getProjectName())
                .warehouseId(e.getWarehouseId())
                .itemName(e.getItemName())
                .itemCode(e.getItemCode())
                .requestedQty(e.getRequestedQty())
                .approvedQty(e.getApprovedQty())
                .unit(e.getUnit())
                .status(e.getStatus())
                .shortageReason(e.getShortageReason())
                .requestedBy(e.getRequestedBy())
                .approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
