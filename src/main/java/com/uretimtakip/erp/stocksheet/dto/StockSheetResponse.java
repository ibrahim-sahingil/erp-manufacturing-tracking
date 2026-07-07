package com.uretimtakip.erp.stocksheet.dto;

import com.uretimtakip.erp.stocksheet.StockSheet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** StockSheet API cevabi. JSON'da global SNAKE_CASE ile doner. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSheetResponse {

    private UUID id;
    private String kind;
    private String name;
    private String material;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private BigDecimal thicknessMm;
    private BigDecimal lengthMm;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static StockSheetResponse fromEntity(StockSheet e) {
        return StockSheetResponse.builder()
                .id(e.getId())
                .kind(e.getKind())
                .name(e.getName())
                .material(e.getMaterial())
                .widthMm(e.getWidthMm())
                .heightMm(e.getHeightMm())
                .thicknessMm(e.getThicknessMm())
                .lengthMm(e.getLengthMm())
                .notes(e.getNotes())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
