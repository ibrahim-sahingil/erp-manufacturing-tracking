package com.uretimtakip.erp.purchasing.dto;

import com.uretimtakip.erp.purchasing.PurchaseItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PurchaseItem API cevabi. JSON'da global SNAKE_CASE ile doner
 * (projectName -> project_name vb.).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItemResponse {

    private UUID id;
    private String projectName;
    private UUID projectBomPartId;
    private String name;
    private String code;
    private BigDecimal quantity;
    private String unit;
    private String material;
    private String supplier;
    private BigDecimal unitPrice;
    private String currency;
    private LocalDate expectedDate;
    private String status;
    private UUID warehouseId;
    private UUID purchaseOrderId;
    private String notes;
    private Boolean needsPlanning;
    private Boolean sentToPurchasing;
    private UUID stockPlanId;
    private LocalDateTime orderedAt;
    private LocalDateTime receivedAt;
    private String receivedBy;
    private BigDecimal receivedQty;
    private BigDecimal returnedQty;
    private String createdBy;
    private LocalDateTime createdAt;

    public static PurchaseItemResponse fromEntity(PurchaseItem e) {
        return PurchaseItemResponse.builder()
                .id(e.getId())
                .projectName(e.getProjectName())
                .projectBomPartId(e.getProjectBomPartId())
                .name(e.getName())
                .code(e.getCode())
                .quantity(e.getQuantity())
                .unit(e.getUnit())
                .material(e.getMaterial())
                .supplier(e.getSupplier())
                .unitPrice(e.getUnitPrice())
                .currency(e.getCurrency())
                .expectedDate(e.getExpectedDate())
                .status(e.getStatus())
                .warehouseId(e.getWarehouseId())
                .purchaseOrderId(e.getPurchaseOrderId())
                .notes(e.getNotes())
                .needsPlanning(e.getNeedsPlanning())
                .sentToPurchasing(e.getSentToPurchasing())
                .stockPlanId(e.getStockPlanId())
                .orderedAt(e.getOrderedAt())
                .receivedAt(e.getReceivedAt())
                .receivedBy(e.getReceivedBy())
                .receivedQty(e.getReceivedQty())
                .returnedQty(e.getReturnedQty())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
