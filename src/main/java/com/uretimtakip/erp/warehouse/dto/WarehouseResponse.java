package com.uretimtakip.erp.warehouse.dto;

import com.uretimtakip.erp.warehouse.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Warehouse API cevabi. JSON'da global SNAKE_CASE ile doner.
 * Sorumlu kullanicinin ADI frontend'de cozulur (FIELD_XLATE userIdToName).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {

    private UUID id;
    private String name;
    private String location;
    private UUID responsibleUserId;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static WarehouseResponse fromEntity(Warehouse e) {
        return WarehouseResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .location(e.getLocation())
                .responsibleUserId(e.getResponsibleUserId())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
