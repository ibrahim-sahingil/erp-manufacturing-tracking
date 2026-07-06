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
 * WarehouseMovement CREATE icin request DTO. (PUT yok - hareket duzeltilmez,
 * yanlis kayit silinip yeniden girilir.)
 *
 * Ornek (elle giris):
 *   { "warehouse_id": "uuid", "item_name": "M12 civata", "movement_type": "IN",
 *     "quantity": 200, "unit": "adet", "performed_by": "Ibrahim Sahingil" }
 *
 * Ornek (satin almadan aktarim - frontend whTransfer yazar):
 *   { "warehouse_id": "uuid", "purchase_item_id": "uuid", "item_name": "...",
 *     "movement_type": "IN", "quantity": 2, "source_type": "PURCHASE_TRANSFER" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseMovementRequest {

    @NotNull(message = "Depo secilmeli")
    private UUID warehouseId;

    private UUID purchaseItemId;

    private UUID deliveryNoteId;

    @NotBlank(message = "Malzeme adi bos olamaz")
    @Size(max = 200, message = "Malzeme adi en fazla 200 karakter olabilir")
    private String itemName;

    @Size(max = 100, message = "Malzeme kodu en fazla 100 karakter olabilir")
    private String itemCode;

    @NotBlank(message = "Hareket tipi bos olamaz (IN/OUT)")
    @Size(max = 10, message = "Hareket tipi en fazla 10 karakter olabilir")
    private String movementType;

    @NotNull(message = "Miktar bos olamaz")
    @Positive(message = "Miktar pozitif olmali")
    private BigDecimal quantity;

    @Size(max = 20, message = "Birim en fazla 20 karakter olabilir")
    private String unit;

    @Size(max = 30, message = "Kaynak tipi en fazla 30 karakter olabilir")
    private String sourceType;

    @Size(max = 150, message = "Islemi yapan en fazla 150 karakter olabilir")
    private String performedBy;

    private String notes;
}
