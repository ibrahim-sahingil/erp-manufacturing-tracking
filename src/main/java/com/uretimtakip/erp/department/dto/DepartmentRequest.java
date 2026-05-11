package com.uretimtakip.erp.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Departman olusturma veya guncelleme istegi.
 *
 * Frontend bu format'ta JSON gonderir:
 *   { "name": "Kaynak", "sortOrder": 1 }
 *   { "name": "Boyama", "orderId": "uuid-...", "sortOrder": 2 }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {

    @NotBlank(message = "Departman adi bos olamaz")
    @Size(max = 100, message = "Departman adi en fazla 100 karakter olabilir")
    private String name;

    private UUID orderId;

    @PositiveOrZero(message = "Sira numarasi negatif olamaz")
    private Integer sortOrder;
}