package com.uretimtakip.erp.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Yeni siparis olusturma veya guncelleme istegi.
 *
 * Ornek JSON:
 *   {
 *     "projectName": "Proje X",
 *     "customerName": "ABC Sirketi",
 *     "customerEmail": "abc@firma.com",
 *     "customerPhone": "0532...",
 *     "location": "Istanbul",
 *     "deliveryDays": 30,
 *     "totalPrice": 50000.00,
 *     "currency": "TRY",
 *     "status": "ACTIVE",
 *     "notes": "...",
 *     "items": [
 *       { "itemName": "Tabla", "quantity": 2 },
 *       { "itemName": "Plaka", "quantity": 5 }
 *     ]
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotBlank(message = "Proje adi bos olamaz")
    @Size(max = 100, message = "Proje adi en fazla 100 karakter olabilir")
    private String projectName;

    @Size(max = 150)
    private String customerName;

    @Email(message = "Gecerli bir email girin")
    @Size(max = 100)
    private String customerEmail;

    @Size(max = 50)
    private String customerPhone;

    @Size(max = 150)
    private String location;

    @PositiveOrZero(message = "Teslim suresi negatif olamaz")
    private Integer deliveryDays;

    @PositiveOrZero(message = "Toplam fiyat negatif olamaz")
    private BigDecimal totalPrice;

    @Size(max = 10)
    private String currency;

    @Size(max = 20)
    private String status;

    private UUID approvedBy;

    private String notes;

    /**
     * Siparis kalemleri (opsiyonel).
     */
    @Valid
    private List<OrderItemRequest> items = new ArrayList<>();
}