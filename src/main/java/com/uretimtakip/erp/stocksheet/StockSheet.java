package com.uretimtakip.erp.stocksheet;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * stock_sheets tablosunun Java karsiligi (#10 plaka MRP - arkadas istegi 3. tur).
 *
 * Ihtiyac planlamasinda kullanilan standart plaka/profil olculeri katalogu.
 * SAC: width_mm x height_mm x thickness_mm (orn. 1350x5000x3).
 * PROFIL: length_mm (orn. 6000/12000) + ad profil kesitini tasir (orn. 40x40x2 kutu).
 * Katalog secim listesidir; hesap ekraninda elle olcu girmek de mumkundur.
 *
 * DB Sema:
 *   id           uuid          (BaseEntity'den)
 *   kind         varchar(10)   NOT NULL (CHECK: SAC | PROFIL)
 *   name         varchar(150)  NOT NULL (orn. "DKP Sac 1350x5000x3", "40x40x2 Kutu Profil 6m")
 *   material     varchar(150)  NULL
 *   width_mm     numeric(15,4) NULL (SAC)
 *   height_mm    numeric(15,4) NULL (SAC)
 *   thickness_mm numeric(15,4) NULL (SAC)
 *   length_mm    numeric(15,4) NULL (PROFIL)
 *   notes        text          NULL
 *   is_active    boolean       DEFAULT true NOT NULL
 *   created_at   timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "stock_sheets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSheet extends BaseEntity {

    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "material", length = 150)
    private String material;

    @Column(name = "width_mm", precision = 15, scale = 4)
    private BigDecimal widthMm;

    @Column(name = "height_mm", precision = 15, scale = 4)
    private BigDecimal heightMm;

    @Column(name = "thickness_mm", precision = 15, scale = 4)
    private BigDecimal thicknessMm;

    @Column(name = "length_mm", precision = 15, scale = 4)
    private BigDecimal lengthMm;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
