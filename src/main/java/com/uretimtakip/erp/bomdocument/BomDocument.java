package com.uretimtakip.erp.bomdocument;

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
 * bom_documents tablosunun Java karsiligi (5. tur arkadas istegi #7:
 * teknik resim / dosya sistemi — EN KRITIK OZELLIK).
 *
 * Urun agacina bagli dosyalar (DWG, resim, PDF...). Iki kategori:
 * URETIM (uretim resimleri) / ARGE (ar-ge resimleri). Dosya icerigi
 * DB'de bytea saklanir — gunluk pg_dump yedegine otomatik girer,
 * ayri klasor yedegi gerekmez. Limit: 50MB (multipart ayari).
 *
 * PARCA BAGLARI (bom_document_parts): dokumanin urun agacinda hangi
 * parcalari kapsadigi. Join tablo FK CASCADE'li — parca ya da dokuman
 * silinince bag kendiliginden temizlenir. Kapsam kurali (frontend'te
 * hesaplanir): her YARI_MAMUL ve MAMUL parcanin en az bir URETIM
 * dokumani olmali; eksikler Teknik Resimler sekmesinde listelenir.
 *
 * DIKKAT: listelemede data kolonu ASLA cekilmez — repository'deki
 * constructor-projection meta sorgusu kullanilir (50MB'lik satirlar
 * listede tasinmasin).
 *
 * DB Sema:
 *   id           uuid         (BaseEntity'den; DEFAULT gen_random_uuid())
 *   product_id   uuid         NOT NULL (FK -> bom_products, CASCADE)
 *   category     varchar(20)  NOT NULL (CHECK: URETIM/ARGE)
 *   filename     varchar(255) NOT NULL
 *   content_type varchar(100) NULL
 *   size_bytes   bigint       NOT NULL
 *   data         bytea        NOT NULL
 *   uploaded_by  varchar(150) NULL (goruntuleme adi — users FK degil,
 *                             warehouse_movements.performed_by deseni)
 *   created_at   timestamp    (BaseEntity'den)
 *   + bom_document_parts(document_id, bom_part_id) join tablosu
 */
@Entity
@Table(name = "bom_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomDocument extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** Dosya icerigi — Hibernate 6'da byte[] Postgres bytea'ya maplenir (@Lob YOK; oid istemiyoruz). */
    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;

    /** Dokumanin kapsadigi parcalar (bom_parts.id). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "bom_document_parts",
            joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "bom_part_id")
    @Builder.Default
    private Set<UUID> partIds = new HashSet<>();
}
