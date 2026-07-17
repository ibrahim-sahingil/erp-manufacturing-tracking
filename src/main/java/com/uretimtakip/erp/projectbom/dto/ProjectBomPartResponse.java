package com.uretimtakip.erp.projectbom.dto;

import com.uretimtakip.erp.bom.BomPart;
import com.uretimtakip.erp.projectbom.ProjectBomPart;
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
 * ProjectBomPart icin response DTO.
 *
 * Hem RAW (custom_X) hem RESOLVED alanlar doner:
 *   - raw: custom_name, custom_qty... - DB'de ne varsa
 *   - resolved: resolvedName, resolvedQty... - BomPart sablonuyla coalesce
 *
 * Frontend ya raw'a ya resolved'a bakar. resolved alanlar shortcut'tir:
 *   "Eger bomPart referans verilmis ve custom_X null ise, sablondan oku.
 *    Yoksa custom_X kullan. Hicbiri yoksa null."
 *
 * Bu sayede frontend "?? coalesce" mantigi yazmaz, backend hazirlar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBomPartResponse {

    // ============ RAW ALANLAR (DB'den okundugu gibi) ============
    private UUID id;
    private UUID projectBomId;
    private UUID bomPartId;
    private Boolean isExcluded;

    private String customName;
    private String customCode;
    private BigDecimal customQty;
    private String customUnit;
    private BigDecimal customWeight;
    private String customMaterial;
    private BigDecimal customWidthMm;
    private BigDecimal customHeightMm;
    private BigDecimal customThicknessMm;
    private BigDecimal customLengthMm;
    private BigDecimal customDiameterMm;
    private String materialKind;
    private String materialForm;

    private UUID deptId;
    private UUID parentCustomId;
    private List<Map<String, Object>> operations;
    private Integer level;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    /** (9. tur M4) MIP karari: PURCHASE / PRODUCE / null (karar bekliyor). */
    private String procurementDecision;
    private String decidedBy;
    private LocalDateTime decidedAt;

    /** (13. tur madde 4) Paket Planlamasi isareti. */
    private Boolean shipPlanned;

    /** (14. tur S1) Plan adedi (null = belirtilmemis, tamami sayilir). */
    private BigDecimal shipPlannedQty;

    // ============ RESOLVED ALANLAR (coalesce edilmis) ============
    private String resolvedName;
    private String resolvedCode;
    private BigDecimal resolvedQty;
    private String resolvedUnit;
    private BigDecimal resolvedWeight;
    private String resolvedMaterial;
    private BigDecimal resolvedWidthMm;
    private BigDecimal resolvedHeightMm;
    private BigDecimal resolvedThicknessMm;
    private BigDecimal resolvedLengthMm;
    private BigDecimal resolvedDiameterMm;
    private String resolvedMaterialKind;
    private String resolvedMaterialForm;

    /**
     * BomPart referansi YOK - sadece custom_X'lerden olusturulur.
     * Coalesce yapacak sablon olmadigi icin resolved == custom.
     */
    public static ProjectBomPartResponse fromEntity(ProjectBomPart e) {
        return fromEntity(e, null);
    }

    /**
     * BomPart referansi VAR - resolved alanlar coalesce edilir.
     * bomPart parametresi null gelirse coalesce yapilmaz (sadece custom kullanilir).
     */
    public static ProjectBomPartResponse fromEntity(
            ProjectBomPart e, BomPart bomPart) {

        ProjectBomPartResponse r = ProjectBomPartResponse.builder()
                .id(e.getId())
                .projectBomId(e.getProjectBomId())
                .bomPartId(e.getBomPartId())
                .isExcluded(e.getIsExcluded())
                .customName(e.getCustomName())
                .customCode(e.getCustomCode())
                .customQty(e.getCustomQty())
                .customUnit(e.getCustomUnit())
                .customWeight(e.getCustomWeight())
                .customMaterial(e.getCustomMaterial())
                .customWidthMm(e.getCustomWidthMm())
                .customHeightMm(e.getCustomHeightMm())
                .customThicknessMm(e.getCustomThicknessMm())
                .customLengthMm(e.getCustomLengthMm())
                .customDiameterMm(e.getCustomDiameterMm())
                .materialKind(e.getMaterialKind())
                .materialForm(e.getMaterialForm())
                .deptId(e.getDeptId())
                .parentCustomId(e.getParentCustomId())
                .operations(e.getOperations())
                .level(e.getLevel())
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt())
                .procurementDecision(e.getProcurementDecision())
                .decidedBy(e.getDecidedBy())
                .decidedAt(e.getDecidedAt())
                .shipPlanned(e.getShipPlanned())
                .shipPlannedQty(e.getShipPlannedQty())
                .build();

        // RESOLVED alanlar - custom oncelikli, yoksa sablondan
        r.setResolvedName(coalesceString(e.getCustomName(),
                bomPart != null ? bomPart.getName() : null));
        r.setResolvedCode(coalesceString(e.getCustomCode(),
                bomPart != null ? bomPart.getCode() : null));
        r.setResolvedQty(coalesceBigDecimal(e.getCustomQty(),
                bomPart != null ? bomPart.getQuantity() : null));
        r.setResolvedUnit(coalesceString(e.getCustomUnit(),
                bomPart != null ? bomPart.getUnit() : null));
        r.setResolvedWeight(coalesceBigDecimal(e.getCustomWeight(),
                bomPart != null ? bomPart.getWeightKg() : null));
        r.setResolvedMaterial(coalesceString(e.getCustomMaterial(),
                bomPart != null ? bomPart.getMaterial() : null));
        r.setResolvedWidthMm(coalesceBigDecimal(e.getCustomWidthMm(),
                bomPart != null ? bomPart.getWidthMm() : null));
        r.setResolvedHeightMm(coalesceBigDecimal(e.getCustomHeightMm(),
                bomPart != null ? bomPart.getHeightMm() : null));
        r.setResolvedThicknessMm(coalesceBigDecimal(e.getCustomThicknessMm(),
                bomPart != null ? bomPart.getThicknessMm() : null));
        r.setResolvedLengthMm(coalesceBigDecimal(e.getCustomLengthMm(),
                bomPart != null ? bomPart.getLengthMm() : null));
        r.setResolvedDiameterMm(coalesceBigDecimal(e.getCustomDiameterMm(),
                bomPart != null ? bomPart.getDiameterMm() : null));
        r.setResolvedMaterialKind(coalesceString(e.getMaterialKind(),
                bomPart != null ? bomPart.getMaterialKind() : null));
        r.setResolvedMaterialForm(coalesceString(e.getMaterialForm(),
                bomPart != null ? bomPart.getMaterialForm() : null));

        return r;
    }

    private static String coalesceString(String custom, String fallback) {
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return fallback;
    }

    private static BigDecimal coalesceBigDecimal(BigDecimal custom, BigDecimal fallback) {
        return custom != null ? custom : fallback;
    }
}