package com.uretimtakip.erp.bom.dto;

import com.uretimtakip.erp.bom.BomProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BomProduct icin response DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomProductResponse {

    private UUID id;
    private String name;
    private String code;
    private String unit;
    private String description;
    private LocalDateTime createdAt;

    public static BomProductResponse fromEntity(BomProduct e) {
        return BomProductResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .unit(e.getUnit())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .build();
    }
}