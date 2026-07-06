package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * warehouses tablosunun Java karsiligi.
 *
 * Depo tanimlari: fabrikadaki fiziksel/mantiksal depolar. Satin alma
 * kalemleri "Depoya Aktar" ile bir depoya baglanir (purchase_items.warehouse_id),
 * serbest malzemeler warehouse_movements uzerinden takip edilir.
 *
 * Silme kurali: uzerinde hareket veya bagli satin alma kalemi olan depo
 * SILINEMEZ (WAREHOUSE_IN_USE) - onun yerine pasife alinir (is_active=false).
 * Pasif depolar aktarim hedefi olarak listelenmez ama gecmisi korunur.
 *
 * DB Sema:
 *   id                  uuid         (BaseEntity'den)
 *   name                varchar(150) NOT NULL UNIQUE
 *   location            varchar(200) NULL
 *   responsible_user_id uuid         NULL (FK -> users, ON DELETE SET NULL)
 *   is_active           boolean      DEFAULT true NOT NULL
 *   created_at          timestamp    (BaseEntity'den)
 */
@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "responsible_user_id")
    private UUID responsibleUserId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
