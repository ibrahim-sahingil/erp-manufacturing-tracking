package com.uretimtakip.erp.material.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Material PARTIAL update icin request DTO — sadece gonderilen alanlar degisir.
 *
 * Tipik kullanim:
 *   {"name": "ST52-K"}     - yeniden adlandir
 *   {"is_active": false}   - pasife al
 */
@Getter
@Setter
@NoArgsConstructor
public class MaterialUpdateRequest {

    @Size(max = 150, message = "Malzeme adi en fazla 150 karakter olabilir")
    private String name;

    private Boolean isActive;
}
