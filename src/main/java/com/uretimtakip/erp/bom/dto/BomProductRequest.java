package com.uretimtakip.erp.bom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BomProduct create/update icin request DTO.
 *
 * Ornek JSON:
 *   {
 *     "name": "X Tipi Vinc Kabini",
 *     "code": "VNC-X-001",
 *     "unit": "adet",
 *     "description": "Standart 5 ton vinc kabini sablonu"
 *   }
 *
 * NOT: code zorunlu degil (DB'de nullable).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomProductRequest {

    @NotBlank(message = "Ad bos olamaz")
    @Size(max = 200, message = "Ad en fazla 200 karakter olabilir")
    private String name;

    @Size(max = 100, message = "Kod en fazla 100 karakter olabilir")
    private String code;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;

    @Size(max = 5000, message = "Aciklama cok uzun")
    private String description;
}