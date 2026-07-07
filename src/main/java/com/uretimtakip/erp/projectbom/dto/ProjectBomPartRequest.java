package com.uretimtakip.erp.projectbom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ProjectBomPart create/update icin request DTO.
 *
 * Senaryolar:
 *
 * 1) AUTO-POPULATE'TEN GELEN PARCAYI OZELLESTIR (UPDATE):
 *    - bomPartId zaten dolu, dokunma
 *    - custom_qty, custom_material gibi alanlar doldur
 *    - is_excluded ile gostermek/gizlemek istersen
 *
 * 2) SABLONDAN BIR PARCAYI MANUEL EKLE (CREATE):
 *    - bomPartId: var olan bir BomPart'in id'sini ver
 *    - custom_X alanlari null - sablondan okur
 *
 * 3) PROJEYE OZGU YENI PARCA EKLE (CREATE, sablon yok):
 *    - bomPartId: null
 *    - custom_name, custom_code zorunlu (cunku coalesce edecek baska kaynak yok)
 *    - Bu durumda service ek validation yapar
 *
 * NOT (UPDATE icin):
 *   projectBomId, parentCustomId, bomPartId, level IMMUTABLE.
 *   Hierarchy degisiklikleri icin sil + yeni olustur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBomPartRequest {

    @NotNull(message = "projectBomId zorunlu")
    private UUID projectBomId;

    /**
     * Var olan bir BomPart'a referans (opsiyonel).
     * null ise sadece custom_X alanlarindan okur (projeye ozgu parca).
     */
    private UUID bomPartId;

    /**
     * Root parca icin null, alt parca icin parent ProjectBomPart'in id'si.
     */
    private UUID parentCustomId;

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

    @jakarta.validation.constraints.Pattern(
            regexp = "^(TEDARIK|HAMMADDE|YARI_MAMUL|MAMUL|SARF)?$",
            message = "Malzeme turu TEDARIK/HAMMADDE/YARI_MAMUL/MAMUL/SARF olmali")
    private String materialKind;

    /**
     * Bu parcayi hangi departman uretecek.
     * Department FK - service'te varlik kontrolu yapilir.
     */
    private UUID deptId;

    /**
     * Esnek jsonb yapisi (BomPart.operations ile ayni format).
     */
    private List<Map<String, Object>> operations;

    @PositiveOrZero(message = "Sira 0 veya pozitif olmali")
    private Integer sortOrder;
}