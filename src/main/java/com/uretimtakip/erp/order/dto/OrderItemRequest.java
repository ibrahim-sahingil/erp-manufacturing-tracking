package com.uretimtakip.erp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Siparis kalemi olusturma/guncelleme istegi.
 *
 * Ornek:
 *   { "itemName": "Tabla", "description": "...", "quantity": 2 }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotBlank(message = "Urun adi bos olamaz")
    @Size(max = 150)
    private String itemName;

    private String description;

    @Positive(message = "Miktar 0'dan buyuk olmali")
    private Integer quantity;
}