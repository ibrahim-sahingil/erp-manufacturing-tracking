package com.uretimtakip.erp.shipment.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ShipmentPackage PARTIAL update DTO (13. tur madde 4).
 *
 * DURUM GECISLERI (service whitelist — komsu gecisler iki yonlu):
 *   OPEN <-> CLOSED <-> LOADED <-> SHIPPED
 *   CLOSED'a gecis packed_by ister (istekte veya kayitta dolu olmali);
 *   LOADED'a gecis delivery_note_id ister; packed_at service'te damgalanir.
 * @Pattern hem burada hem CREATE tarafinda dogrulanir (UCLU kural; ancak
 * CREATE status kabul etmez — her paket OPEN baslar).
 *
 * delivery_note_id PRESENCE takipli: explicit null = aractan cikar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPackageUpdateRequest {

    @Size(max = 150, message = "Paket adi en fazla 150 karakter olabilir")
    private String name;

    @PositiveOrZero(message = "Uzunluk 0 veya pozitif olmali")
    private BigDecimal lengthCm;

    @PositiveOrZero(message = "Genislik 0 veya pozitif olmali")
    private BigDecimal widthCm;

    @PositiveOrZero(message = "Yukseklik 0 veya pozitif olmali")
    private BigDecimal heightCm;

    @PositiveOrZero(message = "Agirlik 0 veya pozitif olmali")
    private BigDecimal weightKg;

    @Pattern(regexp = "OPEN|CLOSED|LOADED|SHIPPED",
            message = "Durum OPEN, CLOSED, LOADED veya SHIPPED olabilir")
    private String status;

    @Setter(lombok.AccessLevel.NONE)
    private UUID deliveryNoteId;

    @Setter(lombok.AccessLevel.NONE)
    private boolean deliveryNoteIdPresent;

    public void setDeliveryNoteId(UUID deliveryNoteId) {
        this.deliveryNoteId = deliveryNoteId;
        this.deliveryNoteIdPresent = true;
    }

    @Size(max = 150, message = "Paketleyen en fazla 150 karakter olabilir")
    private String packedBy;

    private String notes;
}
