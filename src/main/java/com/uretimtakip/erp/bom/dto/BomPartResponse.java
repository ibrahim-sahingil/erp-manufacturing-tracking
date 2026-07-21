package com.uretimtakip.erp.bom.dto;

import com.uretimtakip.erp.bom.BomPart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BomPart icin response DTO.
 * Tum entity alanlari + createdAt donulur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomPartResponse {

    private UUID id;
    private UUID productId;
    private UUID parentId;
    private String name;
    private String code;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal weightKg;
    private String material;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private BigDecimal thicknessMm;
    private BigDecimal lengthMm;
    private BigDecimal diameterMm;
    private String materialKind;
    private String materialForm;
    private String departmentName;   // (16. tur M2)
    private List<Map<String, Object>> operations;
    private Integer level;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public static BomPartResponse fromEntity(BomPart e) {
        return BomPartResponse.builder()
                .id(e.getId())
                .productId(e.getProductId())
                .parentId(e.getParentId())
                .name(e.getName())
                .code(e.getCode())
                .quantity(e.getQuantity())
                .unit(e.getUnit())
                .weightKg(e.getWeightKg())
                .material(e.getMaterial())
                .widthMm(e.getWidthMm())
                .heightMm(e.getHeightMm())
                .thicknessMm(e.getThicknessMm())
                .lengthMm(e.getLengthMm())
                .diameterMm(e.getDiameterMm())
                .materialKind(e.getMaterialKind())
                .materialForm(e.getMaterialForm())
                .departmentName(e.getDepartmentName())
                .operations(e.getOperations())
                .level(e.getLevel())
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt())
                .build();
    }
}