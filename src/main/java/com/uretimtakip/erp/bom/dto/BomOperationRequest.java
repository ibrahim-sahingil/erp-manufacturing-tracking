package com.uretimtakip.erp.bom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BomOperation create/update icin request DTO.
 *
 * Ornek JSON:
 *   {
 *     "name": "Kaynak",
 *     "code": "WLD",
 *     "description": "TIG/MIG kaynak operasyonu",
 *     "sortOrder": 1
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomOperationRequest {

    @NotBlank(message = "Ad bos olamaz")
    @Size(max = 150, message = "Ad en fazla 150 karakter olabilir")
    private String name;

    @NotBlank(message = "Kod bos olamaz")
    @Size(max = 50, message = "Kod en fazla 50 karakter olabilir")
    private String code;

    @Size(max = 5000, message = "Aciklama cok uzun")
    private String description;

    /**
     * (7. tur #1) Bu islemin yapildigi bolum ADI (or. "Kaynak").
     * Bolumler projeye ozeldir (departments.order_id), bu yuzden UUID degil
     * ISIM saklanir; parcaya uygulanirken o projenin ayni isimli bolumu
     * bulunur, yoksa olusturulur.
     */
    @Size(max = 100, message = "Bolum adi en fazla 100 karakter olabilir")
    private String departmentName;

    private Integer sortOrder;
}