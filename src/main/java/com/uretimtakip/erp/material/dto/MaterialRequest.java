package com.uretimtakip.erp.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Material CREATE icin request DTO.
 *
 * Ornek: { "name": "ST52" }
 * is_active verilmezse true baslar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialRequest {

    @NotBlank(message = "Malzeme adi bos olamaz")
    @Size(max = 150, message = "Malzeme adi en fazla 150 karakter olabilir")
    private String name;

    private Boolean isActive;
}
