package com.uretimtakip.erp.material;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * materials tablosunun Java karsiligi (arkadas istegi 5. tur #1: malzeme kartoteki).
 *
 * Malzeme kartoteki: urun agaci parcalarindaki malzeme (bom_parts.material /
 * project_bom_parts.custom_material) bu listeden secilir — serbest metin
 * yazilamaz (frontend datalist + kayit oncesi dogrulama). Celik cesitleri
 * kurulumda seed'lendi; listede olmayan malzeme kartotek modalindan eklenir.
 *
 * bom_parts.material SERBEST METIN kalir (snapshot) — buraya FK YOKTUR;
 * kartotek yalnizca secim listesi. Eski kayitlardaki adlar kuruluma
 * INSERT..SELECT DISTINCT ile aktarildi. Silme serbest (FK yok);
 * yanlislikla eklenen kayit is_active=false ile de pasife alinabilir.
 *
 * DB Sema:
 *   id         uuid         (BaseEntity'den; DEFAULT gen_random_uuid())
 *   name       varchar(150) NOT NULL (service'te harf duyarsiz benzersiz)
 *   is_active  boolean      DEFAULT true NOT NULL
 *   created_at timestamp    (BaseEntity'den)
 */
@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
