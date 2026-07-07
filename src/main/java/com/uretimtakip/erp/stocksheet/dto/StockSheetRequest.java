package com.uretimtakip.erp.stocksheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * StockSheet CREATE/UPDATE icin request DTO (PUT'ta tam govde gonderilir —
 * katalog kaydi kucuk oldugu icin partial update gerekmedi).
 *
 * Ornek (SAC):    {"kind":"SAC","name":"DKP 1350x5000x3","width_mm":1350,"height_mm":5000,"thickness_mm":3}
 * Ornek (PROFIL): {"kind":"PROFIL","name":"40x40x2 Kutu 6m","length_mm":6000}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSheetRequest {

    @NotBlank(message = "Tur zorunlu (SAC/PROFIL)")
    @Pattern(regexp = "^(SAC|PROFIL)$", message = "Tur SAC veya PROFIL olmali")
    private String kind;

    @NotBlank(message = "Ad bos olamaz")
    @Size(max = 150, message = "Ad en fazla 150 karakter olabilir")
    private String name;

    @Size(max = 150, message = "Malzeme en fazla 150 karakter olabilir")
    private String material;

    @PositiveOrZero(message = "En 0 veya pozitif olmali")
    private BigDecimal widthMm;

    @PositiveOrZero(message = "Boy 0 veya pozitif olmali")
    private BigDecimal heightMm;

    @PositiveOrZero(message = "Kalinlik 0 veya pozitif olmali")
    private BigDecimal thicknessMm;

    @PositiveOrZero(message = "Uzunluk 0 veya pozitif olmali")
    private BigDecimal lengthMm;

    private String notes;

    private Boolean isActive;
}
