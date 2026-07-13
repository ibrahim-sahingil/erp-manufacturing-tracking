package com.uretimtakip.erp.projectbom.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ProjectBomPart PARTIAL update icin request DTO.
 *
 * Create'ten (ProjectBomPartRequest) farki: hicbir alan zorunlu degil.
 * Frontend proje hiyerarsi editoru sadece degisen alanlari gonderir:
 *   {"parent_custom_id": "uuid", "level": 1}  - hiyerarsi tasima
 *   {"parent_custom_id": null, "level": 0}    - kok seviyeye cikar
 *   {"dept_id": "uuid"} / {"dept_id": null}   - departman ata / kaldir
 *   {"custom_qty": 3, "custom_unit": "adet"}  - miktar duzenleme
 *   {"operations": [...], "custom_code": "X"} - islem ekleme/cikarma
 *
 * PRESENCE TAKIBI (parent_custom_id ve dept_id):
 *   "alan gonderilmedi" ile "alan: null gonderildi" farkli anlamlar tasir
 *   (null = kok yap / departmani kaldir). Jackson explicit null'da da
 *   setter'i cagirdigi icin setter'da present bayragi kaldiriyoruz.
 *
 * projectBomId ve bomPartId IMMUTABLE kalir - gelse de service ignore eder.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectBomPartUpdateRequest {

    @Setter(lombok.AccessLevel.NONE)
    private UUID parentCustomId;

    @Setter(lombok.AccessLevel.NONE)
    private boolean parentCustomIdPresent;

    public void setParentCustomId(UUID parentCustomId) {
        this.parentCustomId = parentCustomId;
        this.parentCustomIdPresent = true;
    }

    @Setter(lombok.AccessLevel.NONE)
    private UUID deptId;

    @Setter(lombok.AccessLevel.NONE)
    private boolean deptIdPresent;

    public void setDeptId(UUID deptId) {
        this.deptId = deptId;
        this.deptIdPresent = true;
    }

    @PositiveOrZero(message = "Seviye 0 veya pozitif olmali")
    private Integer level;

    private Boolean isExcluded;

    @Size(max = 200, message = "Ad en fazla 200 karakter olabilir")
    private String customName;

    @Size(max = 100, message = "Kod en fazla 100 karakter olabilir")
    private String customCode;

    @PositiveOrZero(message = "Miktar 0 veya pozitif olmali")
    private BigDecimal customQty;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String customUnit;

    @PositiveOrZero(message = "Agirlik 0 veya pozitif olmali")
    private BigDecimal customWeight;

    @Size(max = 150, message = "Materyal en fazla 150 karakter olabilir")
    private String customMaterial;

    @PositiveOrZero(message = "En 0 veya pozitif olmali")
    private BigDecimal customWidthMm;

    @PositiveOrZero(message = "Boy 0 veya pozitif olmali")
    private BigDecimal customHeightMm;

    @PositiveOrZero(message = "Kalinlik 0 veya pozitif olmali")
    private BigDecimal customThicknessMm;

    @PositiveOrZero(message = "Uzunluk 0 veya pozitif olmali")
    private BigDecimal customLengthMm;

    @PositiveOrZero(message = "Cap 0 veya pozitif olmali")
    private BigDecimal customDiameterMm;

    /** Bos string ("") gonderilirse tur TEMIZLENIR (sablon degeri gecerli olur). */
    @jakarta.validation.constraints.Pattern(
            regexp = "^(TEDARIK|HAMMADDE|YARI_MAMUL|MAMUL|SARF)?$",
            message = "Malzeme turu TEDARIK/HAMMADDE/YARI_MAMUL/MAMUL/SARF olmali")
    private String materialKind;

    /** Bos string ("") gonderilirse form TEMIZLENIR (sablon degeri gecerli olur). */
    @jakarta.validation.constraints.Pattern(
            regexp = "^(SAC|PROFIL|MIL|BORU|DELRIN|COK_KOMPONENTLI)?$",
            message = "Malzeme formu SAC/PROFIL/MIL/BORU/DELRIN/COK_KOMPONENTLI olmali")
    private String materialForm;

    /** Esnek jsonb yapisi - ProjectBomPartRequest.operations ile ayni format. */
    private List<Map<String, Object>> operations;

    @PositiveOrZero(message = "Sira 0 veya pozitif olmali")
    private Integer sortOrder;

    /**
     * (9. tur M4) MIP karari. Bos string ("") gonderilirse karar GERI ALINIR
     * (null = karar bekliyor). Karar degistiginde service decided_at/by damgalar.
     */
    @jakarta.validation.constraints.Pattern(
            regexp = "^(PURCHASE|PRODUCE)?$",
            message = "Karar PURCHASE veya PRODUCE olmali")
    private String procurementDecision;

    @Size(max = 150, message = "Karar veren en fazla 150 karakter olabilir")
    private String decidedBy;
}
