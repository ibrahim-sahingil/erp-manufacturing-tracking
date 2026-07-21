package com.uretimtakip.erp.bom.dto;

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
 * BomPart PARTIAL update icin request DTO.
 *
 * Create'ten (BomPartRequest) farki: hicbir alan zorunlu degil.
 * Frontend hiyerarsi editoru sadece degisen alanlari gonderir:
 *   {"parent_id": "uuid", "level": 1}      - parcayi baska parcanin altina tasi
 *   {"parent_id": null, "level": 0}        - parcayi kok seviyeye cikar
 *   {"level": 2}                            - alt parca seviye duzeltmesi
 *   {"quantity": 3, "unit": "adet"}         - miktar duzenleme
 *   {"operations": [...], "code": "X-WLD"}  - islem ekleme/cikarma
 *
 * PARENT_ID PRESENCE TAKIBI:
 *   "parent_id gonderilmedi" ile "parent_id: null gonderildi" (kok yap)
 *   farkli anlamlar tasir. Jackson explicit null'da da setter'i cagirdigi
 *   icin setter'da parentIdPresent bayragi kaldiriyoruz.
 *
 * productId IMMUTABLE kalir - gelse de service ignore eder.
 */
@Getter
@Setter
@NoArgsConstructor
public class BomPartUpdateRequest {

    /** Setter'i elle yazildi (presence takibi) - Lombok @Setter'dan haric. */
    @Setter(lombok.AccessLevel.NONE)
    private UUID parentId;

    @Setter(lombok.AccessLevel.NONE)
    private boolean parentIdPresent;

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
        this.parentIdPresent = true;
    }

    @PositiveOrZero(message = "Seviye 0 veya pozitif olmali")
    private Integer level;

    @Size(max = 200, message = "Ad en fazla 200 karakter olabilir")
    private String name;

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

    /** Bos string ("") gonderilirse tur TEMIZLENIR (null'a doner). */
    @jakarta.validation.constraints.Pattern(
            regexp = "^(TEDARIK|HAMMADDE|YARI_MAMUL|MAMUL|SARF)?$",
            message = "Malzeme turu TEDARIK/HAMMADDE/YARI_MAMUL/MAMUL/SARF olmali")
    private String materialKind;

    /** Bos string ("") gonderilirse form TEMIZLENIR (null'a doner). */
    @jakarta.validation.constraints.Pattern(
            regexp = "^(SAC|PROFIL|MIL|BORU|DELRIN|COK_KOMPONENTLI)?$",
            message = "Malzeme formu SAC/PROFIL/MIL/BORU/DELRIN/COK_KOMPONENTLI olmali")
    private String materialForm;

    // (16. tur M2) sablon bolum adi — null=dokunma, ""=temizle (service blankToNull)
    @Size(max = 100, message = "Bolum adi en fazla 100 karakter olabilir")
    private String departmentName;

    /** Esnek jsonb yapisi - BomPartRequest.operations ile ayni format. */
    private List<Map<String, Object>> operations;

    @PositiveOrZero(message = "Sira 0 veya pozitif olmali")
    private Integer sortOrder;
}
