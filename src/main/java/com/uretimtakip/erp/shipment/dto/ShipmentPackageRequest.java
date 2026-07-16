package com.uretimtakip.erp.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * ShipmentPackage CREATE request DTO (13. tur madde 4).
 *
 * package_no BACKEND uretir (PKT-<yil>-<sira>) — istekte gonderilmez.
 * status CREATE'te kabul edilmez: her paket OPEN baslar; gecisler
 * UpdateRequest + service whitelist'inden yurur (UCLU kural: DB CHECK +
 * @Pattern + service).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackageRequest {

    @NotBlank(message = "Proje adi bos olamaz")
    @Size(max = 100, message = "Proje adi en fazla 100 karakter olabilir")
    private String projectName;

    @Size(max = 150, message = "Paket adi en fazla 150 karakter olabilir")
    private String name;

    @PositiveOrZero(message = "Uzunluk 0 veya pozitif olmali")
    private BigDecimal lengthCm;

    @PositiveOrZero(message = "Genislik 0 veya pozitif olmali")
    private BigDecimal widthCm;

    @PositiveOrZero(message = "Yukseklik 0 veya pozitif olmali")
    private BigDecimal heightCm;

    @PositiveOrZero(message = "Agirlik 0 veya pozitif olmali")
    private BigDecimal weightKg;

    private String notes;

    @Size(max = 150, message = "Olusturan en fazla 150 karakter olabilir")
    private String createdBy;
}
