package com.uretimtakip.erp.order.dto;

import com.uretimtakip.erp.order.Order;
import com.uretimtakip.erp.order.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Frontend'e donulen siparis verisi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private String projectName;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String location;
    private Integer deliveryDays;
    private BigDecimal totalPrice;
    private String currency;
    private String status;
    private UUID approvedBy;
    private String notes;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .projectName(order.getProjectName())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .location(order.getLocation())
                .deliveryDays(order.getDeliveryDays())
                .totalPrice(order.getTotalPrice())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .approvedBy(order.getApprovedBy())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .items(order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Inner class - OrderItem icin response.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private UUID id;
        private String itemName;
        private String description;
        private Integer quantity;
        private LocalDateTime createdAt;

        public static OrderItemResponse fromEntity(OrderItem item) {
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .itemName(item.getItemName())
                    .description(item.getDescription())
                    .quantity(item.getQuantity())
                    .createdAt(item.getCreatedAt())
                    .build();
        }
    }
}