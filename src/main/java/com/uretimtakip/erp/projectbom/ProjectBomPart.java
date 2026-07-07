package com.uretimtakip.erp.projectbom;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * project_bom_parts tablosunun Java karsiligi.
 *
 * Projeye atanmis BOM'un parcalari. BomPart sablonundaki bilgilerin
 * UZERINE proje bazinda ozellestirme yapilir.
 *
 * COALESCE MANTIGI (Service tarafinda resolved alanlari hesaplanir):
 *   - custom_name doluysa onu kullan, degilse bomPart.name'i kullan
 *   - custom_qty doluysa onu kullan, degilse bomPart.quantity'i kullan
 *   - ...
 *
 * IS_EXCLUDED:
 *   true = bu parca bu projede kullanilmayacak (musteri istemedi vs.)
 *   false = projede aktif (default)
 *
 * BOM_PART_ID NULLABLE:
 *   Sablona referans olmadan da ProjectBomPart eklenebilir (custom-only,
 *   sadece bu projeye ozgu). Bu durumda tum custom_X alanlari doldurulmali.
 *
 * SELF-RELATION:
 *   parent_custom_id - JPA self kullanmiyoruz, UUID olarak tutuyoruz
 *   (BomPart pattern'i gibi).
 *
 * DB Sema:
 *   id                uuid          (BaseEntity'den)
 *   project_bom_id    uuid          NOT NULL (FK -> project_bom CASCADE)
 *   bom_part_id       uuid          NULL     (FK -> bom_parts SET NULL)
 *   is_excluded       bool          DEFAULT false
 *   custom_name       varchar(200)  NULL
 *   custom_code       varchar(100)  NULL
 *   custom_qty        numeric(15,4) NULL
 *   custom_unit       varchar(20)   NULL
 *   custom_weight     numeric(15,4) NULL
 *   custom_material   varchar(150)  NULL
 *   dept_id           uuid          NULL (FK -> departments SET NULL)
 *   parent_custom_id  uuid          NULL (FK -> project_bom_parts self CASCADE)
 *   operations        jsonb         DEFAULT '[]'
 *   level             int4          DEFAULT 0
 *   sort_order        int4          DEFAULT 0
 *   created_at        timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "project_bom_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBomPart extends BaseEntity {

    @Column(name = "project_bom_id", nullable = false)
    private UUID projectBomId;

    @Column(name = "bom_part_id")
    private UUID bomPartId;

    @Column(name = "is_excluded")
    @Builder.Default
    private Boolean isExcluded = false;

    @Column(name = "custom_name", length = 200)
    private String customName;

    @Column(name = "custom_code", length = 100)
    private String customCode;

    @Column(name = "custom_qty", precision = 15, scale = 4)
    private BigDecimal customQty;

    @Column(name = "custom_unit", length = 20)
    private String customUnit;

    @Column(name = "custom_weight", precision = 15, scale = 4)
    private BigDecimal customWeight;

    @Column(name = "custom_material", length = 150)
    private String customMaterial;

    /** Sac/profil olcu override'lari (mm) - null ise sablondaki olcu gecerli. */
    @Column(name = "custom_width_mm", precision = 15, scale = 4)
    private BigDecimal customWidthMm;

    @Column(name = "custom_height_mm", precision = 15, scale = 4)
    private BigDecimal customHeightMm;

    @Column(name = "custom_thickness_mm", precision = 15, scale = 4)
    private BigDecimal customThicknessMm;

    /**
     * Malzeme turu override'i (#7): TEDARIK/HAMMADDE/YARI_MAMUL/MAMUL/SARF.
     * NULL ise sablondaki (bom_parts.material_kind) tur gecerli
     * (resolved_material_kind response'ta hesaplanir).
     */
    @Column(name = "material_kind", length = 20)
    private String materialKind;

    @Column(name = "dept_id")
    private UUID deptId;

    @Column(name = "parent_custom_id")
    private UUID parentCustomId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operations", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> operations = new ArrayList<>();

    @Column(name = "level")
    @Builder.Default
    private Integer level = 0;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}