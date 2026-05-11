package com.uretimtakip.erp.part.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Yeni parca olusturma veya guncelleme istegi.
 *
 * Ornek JSON:
 *   {
 *     "name": "Ana Tabla",
 *     "code": "TAB-001",
 *     "drawingNo": "DWG-2026-001",
 *     "material": "Celik",
 *     "departmentId": "uuid-...",
 *     "totalQty": 10,
 *     "status": "PENDING"
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartRequest {

    @NotBlank(message = "Parca adi bos olamaz")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Parca kodu bos olamaz")
    @Size(max = 100)
    private String code;

    @Size(max = 100)
    private String drawingNo;

    @Size(max = 100)
    private String material;

    private UUID orderId;

    private UUID departmentId;

    @PositiveOrZero(message = "Toplam adet negatif olamaz")
    private Integer totalQty;

    @Size(max = 20)
    private String status;

    private String description;

    @PositiveOrZero(message = "qtyDone negatif olamaz")
    private Integer qtyDone;

    @PositiveOrZero(message = "qtyPending negatif olamaz")
    private Integer qtyPending;

    @PositiveOrZero(message = "qtyReject negatif olamaz")
    private Integer qtyReject;
}