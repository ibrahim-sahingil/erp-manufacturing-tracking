package com.uretimtakip.erp.bom.dto;

import com.uretimtakip.erp.bom.BomPart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GET /api/bom-parts/by-code cevabi (5. tur #3: kod akilli doldurma).
 *
 * Ayni kod baska urun agaclarinda kullanilmissa parca bilgisi + hangi urunde
 * oldugu + kac alt parcasi oldugu doner. Frontend adi/malzemeyi otomatik
 * doldurur; child_count > 0 ise alt parcalari listeleyip klonlamayi onerir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomPartCodeMatchResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String productCode;
    private String name;
    private String code;
    private String material;
    private String materialKind;
    private String materialForm;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal weightKg;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private BigDecimal thicknessMm;
    private BigDecimal lengthMm;
    private BigDecimal diameterMm;
    private long childCount;

    public static BomPartCodeMatchResponse fromEntity(
            BomPart e, String productName, String productCode, long childCount) {
        return BomPartCodeMatchResponse.builder()
                .id(e.getId())
                .productId(e.getProductId())
                .productName(productName)
                .productCode(productCode)
                .name(e.getName())
                .code(e.getCode())
                .material(e.getMaterial())
                .materialKind(e.getMaterialKind())
                .materialForm(e.getMaterialForm())
                .quantity(e.getQuantity())
                .unit(e.getUnit())
                .weightKg(e.getWeightKg())
                .widthMm(e.getWidthMm())
                .heightMm(e.getHeightMm())
                .thicknessMm(e.getThicknessMm())
                .lengthMm(e.getLengthMm())
                .diameterMm(e.getDiameterMm())
                .childCount(childCount)
                .build();
    }
}
