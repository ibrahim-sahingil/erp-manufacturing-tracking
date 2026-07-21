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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * bom_parts tablosunun Java karsiligi.
 *
 * Bir BomProduct'in alt parcalari. Hierarchy: parent_id self-referential.
 * "Gövde" altinda "Sol kapak", "Sag kapak" gibi.
 *
 * SELF-RELATION KARARI:
 *   @ManyToOne self KULLANMIYORUZ.
 *   Sadece UUID parentId tutuyoruz. Lookup gerektiginde service'te
 *   manuel sorgu yapilir. Bu sayede lazy loading, infinite recursion
 *   (JSON serialize), cascade davranisi gibi problemlerden kaciniyoruz.
 *
 * OPERATIONS jsonb:
 *   List<Map<String, Object>> formatinda. Esnek - frontend her item'da
 *   farkli alanlar gonderebilir: operationId, code, duration, notes, vb.
 *   User.permissions ile ayni pattern (@JdbcTypeCode + columnDefinition).
 *
 * DB Sema:
 *   id          uuid          (BaseEntity'den)
 *   product_id  uuid          NOT NULL (FK -> bom_products, CASCADE)
 *   parent_id   uuid          NULL    (FK -> bom_parts, self CASCADE)
 *   name        varchar(200)  NOT NULL
 *   code        varchar(100)  NOT NULL (UNIQUE DEGIL)
 *   quantity    numeric(15,4) DEFAULT 1
 *   unit        varchar(20)   DEFAULT 'adet'
 *   weight_kg   numeric(15,4) NULL
 *   material    varchar(150)  NULL
 *   width_mm     numeric(15,4) NULL (olcu - opsiyonel; anlami material_form'a gore degisir)
 *   height_mm    numeric(15,4) NULL
 *   thickness_mm numeric(15,4) NULL (BORU'da et kalinligi, DELRIN'de yukseklik)
 *   length_mm    numeric(15,4) NULL (PROFIL uzunluk / MIL uzunluk / BORU boy — 5. tur #4)
 *   diameter_mm  numeric(15,4) NULL (MIL cap / BORU dis cap — 5. tur #4)
 *   material_kind varchar(20)  NULL (CHECK: TEDARIK/HAMMADDE/YARI_MAMUL/MAMUL/SARF)
 *   material_form varchar(20)  NULL (CHECK: SAC/PROFIL/MIL/BORU/DELRIN/COK_KOMPONENTLI — 5. tur #4)
 *   department_name varchar(100) NULL (16. tur M2 — SABLON bolum ADI; departments
 *                   PROJE kapsamli oldugundan UUID degil AD tasinir
 *                   (bom_operations.department_name emsali). Projeye baglanirken
 *                   autoPopulateBomParts adla cozer, yoksa OLUSTURUR — K3 karari)
 *   operations  jsonb         DEFAULT '[]'
 *   level       int4          DEFAULT 0
 *   sort_order  int4          DEFAULT 0
 *   created_at  timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "bom_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomPart extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "quantity", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "adet";

    @Column(name = "weight_kg", precision = 15, scale = 4)
    private BigDecimal weightKg;

    @Column(name = "material", length = 150)
    private String material;

    /** Sac/profil olculeri (mm) - nesting/plaka hesabi icin, hepsi opsiyonel. */
    @Column(name = "width_mm", precision = 15, scale = 4)
    private BigDecimal widthMm;

    @Column(name = "height_mm", precision = 15, scale = 4)
    private BigDecimal heightMm;

    @Column(name = "thickness_mm", precision = 15, scale = 4)
    private BigDecimal thicknessMm;

    /** Uzunluk/boy (mm) — PROFIL/MIL/BORU formlarinda sorulur (5. tur #4). */
    @Column(name = "length_mm", precision = 15, scale = 4)
    private BigDecimal lengthMm;

    /** Cap / dis cap (mm) — MIL/BORU formlarinda sorulur (5. tur #4). */
    @Column(name = "diameter_mm", precision = 15, scale = 4)
    private BigDecimal diameterMm;

    /**
     * Malzeme turu (#7 arkadas istegi): TEDARIK / HAMMADDE / YARI_MAMUL /
     * MAMUL / SARF. NULL = belirtilmedi (eski kayitlar). Yayinla/satin alma
     * aktariminda yonlendirme icin kullanilir.
     */
    @Column(name = "material_kind", length = 20)
    private String materialKind;

    /**
     * Malzeme formu (5. tur #4): SAC / PROFIL / MIL / BORU / DELRIN /
     * COK_KOMPONENTLI. NULL = belirtilmedi. Frontend forma gore farkli olcu
     * alanlari sorar (FORM_DIMS haritasi); olculerin anlami forma baglidir.
     */
    @Column(name = "material_form", length = 20)
    private String materialForm;

    // (16. tur M2) sablon bolum ADI — projeye baglanista adla cozulur/olusturulur
    @Column(name = "department_name", length = 100)
    private String departmentName;

    /**
     * Operasyonlar listesi (PostgreSQL jsonb -> Java List<Map<String, Object>>).
     * Ornek: [{"operationId":"uuid", "code":"WLD", "duration":30, "notes":"..."}]
     */
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