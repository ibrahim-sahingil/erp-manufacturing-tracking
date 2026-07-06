package com.uretimtakip.erp.warehouse.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Warehouse PARTIAL update icin request DTO.
 *
 * purchase-items'taki desenle ayni: sadece gonderilen alanlar degisir.
 *
 * PRESENCE TAKIPLI ALANLAR (explicit null = temizle):
 *   location, responsible_user_id
 *
 * Tipik kullanim:
 *   {"is_active": false}              - depoyu pasife al
 *   {"responsible_user_id": null}     - sorumluyu kaldir
 */
@Getter
@Setter
@NoArgsConstructor
public class WarehouseUpdateRequest {

    @Size(max = 150, message = "Depo adi en fazla 150 karakter olabilir")
    private String name;

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 200, message = "Konum en fazla 200 karakter olabilir")
    private String location;

    @Setter(lombok.AccessLevel.NONE)
    private boolean locationPresent;

    public void setLocation(String location) {
        this.location = location;
        this.locationPresent = true;
    }

    @Setter(lombok.AccessLevel.NONE)
    private UUID responsibleUserId;

    @Setter(lombok.AccessLevel.NONE)
    private boolean responsibleUserIdPresent;

    public void setResponsibleUserId(UUID responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
        this.responsibleUserIdPresent = true;
    }

    private Boolean isActive;
}
