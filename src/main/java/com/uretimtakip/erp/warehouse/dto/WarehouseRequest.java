package com.uretimtakip.erp.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Warehouse CREATE icin request DTO.
 *
 * Ornek:
 *   { "name": "Ana Depo", "location": "Fabrika zemin kat",
 *     "responsible_user_id": "uuid" }
 *
 * is_active verilmezse true baslar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRequest {

    @NotBlank(message = "Depo adi bos olamaz")
    @Size(max = 150, message = "Depo adi en fazla 150 karakter olabilir")
    private String name;

    @Size(max = 200, message = "Konum en fazla 200 karakter olabilir")
    private String location;

    private UUID responsibleUserId;

    private Boolean isActive;
}
