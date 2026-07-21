package com.uretimtakip.erp.bom.dto;

import jakarta.validation.constraints.NotBlank;
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
 * BomPart create/update icin request DTO.
 *
 * CREATE ornegi:
 *   {
 *     "productId": "uuid",
 *     "parentId": null,
 *     "name": "Govde Plakasi",
 *     "code": "GP-001",
 *     "quantity": 1.0,
 *     "unit": "adet",
 *     "weightKg": 12.5,
 *     "material": "S235",
 *     "operations": [
 *       {"operationId":"uuid","code":"WLD","duration":30},
 *       {"code":"PNT","notes":"toz boya"}
 *     ],
 *     "sortOrder": 1
 *   }
 *
 * UPDATE'TE DIKKAT:
 *   - productId, parentId, level alanlari gelse bile service tarafindan
 *     IGNORE edilir. Bu alanlari degistirmek icin sil+yeni olustur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomPartRequest {

    @NotNull(message = "productId zorunlu")
    private UUID productId;

    /**
     * Root parca icin null gonder. Alt parca icin parent BomPart'in id'si.
     */
    private UUID parentId;

    @NotBlank(message = "Ad bos olamaz")
    @Size(max = 200, message = "Ad en fazla 200 karakter olabilir")
    private String name;

    @NotBlank(message = "Kod bos olamaz")
    @Size(max = 100, message = "Kod en fazla 100 karakter olabilir")
    private String code;

    @PositiveOrZero(message = "Miktar 0 veya pozitif olmali")
    private BigDecimal quantity;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;

    @PositiveOrZero(message = "Agirlik 0 veya pozitif olmali")
    private BigDecimal weightKg;

    @Size(max = 150, message = "Materyal en fazla 150 karakter olabilir")
    private String material;

    @PositiveOrZero(message = "En 0 veya pozitif olmali")
    private BigDecimal widthMm;

    @PositiveOrZero(message = "Boy 0 veya pozitif olmali")
    private BigDecimal heightMm;

    @PositiveOrZero(message = "Kalinlik 0 veya pozitif olmali")
    private BigDecimal thicknessMm;

    @PositiveOrZero(message = "Uzunluk 0 veya pozitif olmali")
    private BigDecimal lengthMm;

    @PositiveOrZero(message = "Cap 0 veya pozitif olmali")
    private BigDecimal diameterMm;

    @jakarta.validation.constraints.Pattern(
            regexp = "^(TEDARIK|HAMMADDE|YARI_MAMUL|MAMUL|SARF)?$",
            message = "Malzeme turu TEDARIK/HAMMADDE/YARI_MAMUL/MAMUL/SARF olmali")
    private String materialKind;

    @jakarta.validation.constraints.Pattern(
            regexp = "^(SAC|PROFIL|MIL|BORU|DELRIN|COK_KOMPONENTLI)?$",
            message = "Malzeme formu SAC/PROFIL/MIL/BORU/DELRIN/COK_KOMPONENTLI olmali")
    private String materialForm;

    // (16. tur M2) sablon bolum adi (opsiyonel)
    @Size(max = 100, message = "Bolum adi en fazla 100 karakter olabilir")
    private String departmentName;

    /**
     * Esnek jsonb yapisi. Her item icindeki anahtarlar serbest.
     * Tipik: operationId (UUID), code (String), duration (Number), notes (String).
     */
    private List<Map<String, Object>> operations;

    @PositiveOrZero(message = "Sirayla 0 veya pozitif olmali")
    private Integer sortOrder;
}