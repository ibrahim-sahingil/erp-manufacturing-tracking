package com.uretimtakip.erp.warehouse.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * WarehouseReservation ONAY request DTO'su (POST /{id}/approve).
 *
 * approved_qty == requested_qty -> APPROVED (tam onay)
 * 0 < approved_qty < requested  -> PARTIAL  (shortage_reason ZORUNLU)
 * approved_qty == 0             -> REJECTED (shortage_reason ZORUNLU)
 *
 * write_adjustment: kismi onayda kayip miktar icin envanter duzeltme
 * OUT'u (RESERVATION_ADJUST) da yazilsin mi. Frontend default'u ISARETLI —
 * kayit gercege cekilmezse MIP ayni hayalet stoga tekrar rezervasyon onerir.
 *
 * Ornek (30 istendi, sayimda 15 cikti):
 *   { "approved_qty": 15, "shortage_reason": "Sayimda 15 cikti - kayip/envanter yanlis",
 *     "write_adjustment": true, "approved_by": "Depocu Ali" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseReservationApproveRequest {

    @NotNull(message = "Onaylanan miktar bos olamaz")
    @PositiveOrZero(message = "Onaylanan miktar negatif olamaz")
    private BigDecimal approvedQty;

    private String shortageReason;

    /** Kayip icin envanter duzeltme hareketi yazilsin mi (default false; frontend true gonderir). */
    private boolean writeAdjustment;

    @Size(max = 150, message = "Onaylayan en fazla 150 karakter olabilir")
    private String approvedBy;
}
