package com.uretimtakip.erp.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Excel'den okunan TEK satirin parse sonucu.
 *
 * Hiyerarsi Excel'deki "Basamak" kolonundan gelir:
 *   1        -> kok parca (level 0)
 *   1.1      -> 1'in alti (level 1)
 *   1.1.2    -> 1.1'in alti (level 2)
 *
 * parentLevelNo backend'de hesaplanir (son segment atilir); frontend
 * bunu kullanarak olusturma sirasinda parent id eslemesi yapar.
 *
 * error null ise satir aktarilabilir; doluysa onizlemede kirmizi
 * gosterilir ve secilemez.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomImportRowResponse {

    /** Excel'deki satir numarasi (1 tabanli, basliktan sonra). */
    private int rowNum;

    /** Basamak numarasi: "1", "1.2", "1.2.3"... */
    private String levelNo;

    /** Ust parcanin basamak numarasi (kok icin null). */
    private String parentLevelNo;

    /** Hiyerarsi derinligi (kok = 0). */
    private int level;

    private String name;
    private String code;
    private String material;
    private BigDecimal quantity;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private BigDecimal thicknessMm;

    /** Malzeme turu (#7): TEDARIK/HAMMADDE/YARI_MAMUL/MAMUL/SARF (null = yok). */
    private String materialKind;

    /** Satir hatasi (null = sorun yok). Ornek: "Ust basamak (1.2) dosyada yok" */
    private String error;
}
