package com.uretimtakip.erp.projectdocument;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * project_documents tablosunun Java karsiligi (16. tur M3.2 — arkadas istegi:
 * "teknik resimleri urunlere degil her bir projeye ozgu olarak depolanacak").
 *
 * PROJE BAZLI teknik resim katmani — urun(sablon) bazli bom_documents AYNEN
 * KALIR (K4 karari: ikisi yan yana). PROJE = STRING (projects tablosu yok;
 * orders.project_name UNIQUE) — FK yerine ad tasinir, orders'ta varligi
 * service dogrular.
 *
 * KATEGORILER (arkadas): SIPARIS (Siparis Resimleri) / IMALAT (Imalat
 * Resimleri) / DIGER (Diger Resimler). UCLU KURAL: DB CHECK + service seti.
 *
 * Parca baglari project_document_parts join tablosunda (bom_document_parts
 * deseni) — project_bom_part_id'ye baglanir; dokuman VE pbp silinince CASCADE.
 * item snapshot GEREKMEZ: bag kopunca dosya projede durmaya devam eder.
 *
 * DB Sema:
 *   id           uuid          (BaseEntity'den)
 *   project_name varchar(100)  NOT NULL (index'li)
 *   category     varchar(20)   DEFAULT 'DIGER' NOT NULL (CHECK)
 *   filename     varchar(300)  NOT NULL
 *   content_type varchar(150)  NULL
 *   size_bytes   bigint        NULL
 *   data         bytea         NOT NULL (icerik DB'de — pg_dump yedegine girer)
 *   uploaded_by  varchar(150)  NULL
 *   created_at   timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "project_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDocument extends BaseEntity {

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "category", nullable = false, length = 20)
    @Builder.Default
    private String category = "DIGER";

    @Column(name = "filename", nullable = false, length = 300)
    private String filename;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    // Liste sorgularinda ASLA cekilmez (meta JPQL) — 50MB'a kadar icerik
    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_document_parts",
            joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "project_bom_part_id")
    @Builder.Default
    private Set<UUID> partIds = new HashSet<>();
}
