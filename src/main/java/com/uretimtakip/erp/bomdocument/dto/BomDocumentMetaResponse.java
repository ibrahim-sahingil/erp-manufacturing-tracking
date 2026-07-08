package com.uretimtakip.erp.bomdocument.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BomDocument META cevabi — data (bytea) ICERMEZ; listeleme bunu kullanir.
 * partIds servis katmaninda join tablodan doldurulur.
 * Iceri indirme ayri endpoint: GET /api/bom-documents/{id}/download
 */
@Getter
@Setter
@NoArgsConstructor
public class BomDocumentMetaResponse {

    private UUID id;
    private UUID productId;
    private String category;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private List<UUID> partIds = new ArrayList<>();

    /** JPQL constructor-projection bu imzayi kullanir (data kolonu cekilmez). */
    public BomDocumentMetaResponse(UUID id, UUID productId, String category,
                                   String filename, String contentType,
                                   Long sizeBytes, String uploadedBy,
                                   LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.category = category;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }
}
