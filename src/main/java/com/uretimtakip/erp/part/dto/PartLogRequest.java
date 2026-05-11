package com.uretimtakip.erp.part.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * QR scan veya manuel kayit ile uretim girisi.
 *
 * Ornek:
 *   { "partId": "uuid", "userId": "uuid", "qtyDone": 5, "note": "Vardiya 1" }
 *
 * Bu istek geldiginde:
 *   - PartLog tablosuna yeni satir eklenir
 *   - parts tablosundaki qty_done/qty_pending/qty_reject SAYILAR ARTIRILIR
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartLogRequest {

    @NotNull(message = "Parca id bos olamaz")
    private UUID partId;

    @NotNull(message = "Kullanici id bos olamaz")
    private UUID userId;

    @PositiveOrZero(message = "Yapilan adet negatif olamaz")
    private Integer qtyDone;

    @PositiveOrZero(message = "Bekleyen adet negatif olamaz")
    private Integer qtyPending;

    @PositiveOrZero(message = "Red adet negatif olamaz")
    private Integer qtyReject;

    private String note;
}