package com.uretimtakip.erp.part.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Part PARTIAL update icin request DTO.
 *
 * Create'ten (PartRequest) farki: hicbir alan zorunlu degil, sadece
 * gonderilen (non-null) alanlar degisir. KRITIK KULLANIM: QR/ilerleme
 * kaydi sadece sunu yollar:
 *   {"status":"done","qty_done":5,"qty_pending":0,"qty_reject":1}
 *
 * Onceki hali create DTO'sunu kullaniyordu (name+code @NotBlank);
 * bu yuzden uretimdeki adet girisi her kayitta 400 aliyordu
 * (bom-parts/work-orders'taki ile ayni hata sinifi).
 */
@Getter
@Setter
@NoArgsConstructor
public class PartUpdateRequest {

    @Size(max = 150)
    private String name;

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
