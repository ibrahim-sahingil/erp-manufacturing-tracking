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

import java.util.UUID;

/**
 * project_bom tablosunun Java karsiligi.
 *
 * Bir BomProduct sablonunun bir projeye atanmis instance'i.
 * Ornek: "X Tipi Vinc Kabini" sablonunu "2026-001 Musteri ABC" projesine
 * atayinca burada bir kayit olusur.
 *
 * AKILLI OZELLIK (Service tarafinda):
 *   Create edildiginde, BomProduct'un tum BomPart'lari otomatik olarak
 *   ProjectBomPart olarak kopyalanir (bomPartId referansli, custom alanlar
 *   null, level/parent_custom_id mapping korunur). Boylece proje hazir
 *   gelir, kullanici sonra ozellestirme yapar.
 *
 * UNIQUE Constraint (DB seviyesinde):
 *   (project_name, bom_product_id) - ayni urunu ayni projeye iki kez
 *   atayamazsin. Service'te de kontrol edilir.
 *
 * DB Sema:
 *   id              uuid          (BaseEntity'den)
 *   project_name    varchar(100)  NOT NULL
 *   bom_product_id  uuid          NOT NULL (FK -> bom_products, CASCADE)
 *   status          varchar(20)   DEFAULT 'draft'
 *   created_by      varchar(150)  NULL  (string, FK degil!)
 *   created_at      timestamp     (BaseEntity'den)
 *
 *   UNIQUE (project_name, bom_product_id)
 */
@Entity
@Table(name = "project_bom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBom extends BaseEntity {

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "bom_product_id", nullable = false)
    private UUID bomProductId;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "draft";

    @Column(name = "created_by", length = 150)
    private String createdBy;
}