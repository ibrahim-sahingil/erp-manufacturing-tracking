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
 * zero_stock (9. tur M8): "bu malzeme bu depoda HIC yok" — sayim yapilmis
 * sayilir, o deponun o malzeme kaydi SIFIRLANIR (adjust = stok - onay;
 * write_adjustment'in min(eksik, ...) formulunden farki: eksikten buyuk
 * hayalet kayit da temizlenir). Frontend tam redde bunu varsayilan secer.
 * Ikisi birden gelirse zero_stock kazanir.
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

    /** Depo kaydini sifirla — "bu malzeme bu depoda hic yok" (9. tur M8). */
    private boolean zeroStock;

    @Size(max = 150, message = "Onaylayan en fazla 150 karakter olabilir")
    private String approvedBy;
}
