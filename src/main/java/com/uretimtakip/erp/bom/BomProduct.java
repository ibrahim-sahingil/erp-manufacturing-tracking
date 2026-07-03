package com.uretimtakip.erp.bom;

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
 * bom_products tablosunun Java karsiligi.
 *
 * Urun sablonu - hangi parcalardan olusur (BomPart hierarchy'si ile).
 * Ornek: "X Tipi Vinc Kabini", "Y Tipi Sasi", vb.
 *
 * Bir BomProduct daha sonra ProjectBom ile bir projeye atanir
 * ve ProjectBomPart'larla ozellestirilir (Faz 2).
 *
 * DB Sema:
 *   id          uuid          (BaseEntity'den)
 *   name        varchar(200)  NOT NULL
 *   code        varchar(100)  NULL (UNIQUE DEGIL - bom_operations'tan farkli)
 *   unit        varchar(20)   DEFAULT 'adet'
 *   description text          NULL
 *   created_at  timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "bom_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomProduct extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "adet";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}