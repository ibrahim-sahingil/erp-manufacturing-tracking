package com.uretimtakip.erp.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * WarehouseReservation CREATE (talep) request DTO'su.
 *
 * Ornek (MIP ekranindan):
 *   { "project_name": "PROJE-X", "warehouse_id": "uuid", "item_name": "M12 civata",
 *     "item_code": "CIV-M12", "requested_qty": 30, "unit": "adet",
 *     "requested_by": "Ibrahim Sahingil" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseReservationRequest {

    @NotBlank(message = "Proje adi bos olamaz")
    @Size(max = 100, message = "Proje adi en fazla 100 karakter olabilir")
    private String projectName;

    @NotNull(message = "Depo secilmeli")
    private UUID warehouseId;

    /** Toplama deposu (istege bagli): onaylanan miktar once buraya aktarilir. */
    private UUID targetWarehouseId;

    @NotBlank(message = "Malzeme adi bos olamaz")
    @Size(max = 200, message = "Malzeme adi en fazla 200 karakter olabilir")
    private String itemName;

    @Size(max = 100, message = "Malzeme kodu en fazla 100 karakter olabilir")
    private String itemCode;

    @NotNull(message = "Istenen miktar bos olamaz")
    @Positive(message = "Istenen miktar pozitif olmali")
    private BigDecimal requestedQty;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;

    @Size(max = 150, message = "Talep eden en fazla 150 karakter olabilir")
    private String requestedBy;

    private String notes;
}
