package com.uretimtakip.erp.orderdocument;

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
 * (12. tur m1) Siparis/teklif dosya ekleri — bom_documents deseninin
 * orders'a uyarlanmasi (teklif PDF'leri, sozlesme, revizyon dosyalari).
 *
 * DB Sema (schema.sql):
 *   id           uuid         PK DEFAULT gen_random_uuid()
 *   order_id     uuid         NOT NULL (FK -> orders, ON DELETE CASCADE)
 *   category     varchar(20)  NOT NULL DEFAULT 'QUOTE' (CHECK: QUOTE/ORDER)
 *   filename     varchar(300) NOT NULL
 *   content_type varchar(150) NULL
 *   size_bytes   bigint       NULL
 *   data         bytea        NOT NULL
 *   uploaded_by  varchar(150) NULL (goruntuleme adi — users FK degil)
 *   created_at   timestamp    DEFAULT CURRENT_TIMESTAMP
 */
@Entity
@Table(name = "order_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDocument extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** UCLU KURAL: DB CHECK + service validateCategory ayni listeyi tutar. */
    @Column(name = "category", nullable = false, length = 20)
    @Builder.Default
    private String category = "QUOTE";

    @Column(name = "filename", nullable = false, length = 300)
    private String filename;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Dosya icerigi — byte[] Postgres bytea (@Lob YOK; bom_documents deseni). */
    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;
}
