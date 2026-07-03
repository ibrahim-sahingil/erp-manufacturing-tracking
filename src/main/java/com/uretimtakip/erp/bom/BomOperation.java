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
 * bom_operations tablosunun Java karsiligi.
 *
 * Operasyon kutuphanesi - bir parcanin gecebilecegi uretim
 * operasyonlari (Kaynak, Boya, Tornalama, Montaj vb.).
 *
 * DB Sema:
 *   id          uuid          (BaseEntity'den)
 *   name        varchar(150)  NOT NULL
 *   code        varchar(50)   NOT NULL UNIQUE
 *   description text
 *   sort_order  int4          DEFAULT 0
 *   created_at  timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "bom_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomOperation extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}