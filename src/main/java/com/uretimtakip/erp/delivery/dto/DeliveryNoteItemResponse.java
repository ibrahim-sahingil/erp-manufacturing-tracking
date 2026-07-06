package com.uretimtakip.erp.delivery.dto;

import com.uretimtakip.erp.delivery.DeliveryNoteItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DeliveryNoteItem API cevabi. JSON'da global SNAKE_CASE ile doner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNoteItemResponse {

    private UUID id;
    private UUID deliveryNoteId;
    private UUID warehouseId;
    private String itemName;
    private String itemCode;
    private BigDecimal quantity;
    private String unit;
    private String notes;
    private LocalDateTime createdAt;

    public static DeliveryNoteItemResponse fromEntity(DeliveryNoteItem e) {
        return DeliveryNoteItemResponse.builder()
                .id(e.getId())
                .deliveryNoteId(e.getDeliveryNoteId())
                .warehouseId(e.getWarehouseId())
                .itemName(e.getItemName())
                .itemCode(e.getItemCode())
                .quantity(e.getQuantity())
                .unit(e.getUnit())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
