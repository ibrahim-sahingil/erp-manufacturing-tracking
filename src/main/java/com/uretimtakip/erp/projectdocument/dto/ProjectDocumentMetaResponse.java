package com.uretimtakip.erp.projectdocument.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** (16. tur M3.2) Proje dosyasi META'si — data icermez (indirme ayri uc). */
@Getter
@Setter
@NoArgsConstructor
public class ProjectDocumentMetaResponse {
    private UUID id;
    private String projectName;
    private String category;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private List<UUID> partIds = new ArrayList<>();

    public ProjectDocumentMetaResponse(UUID id, String projectName, String category,
                                       String filename, String contentType, Long sizeBytes,
                                       String uploadedBy, LocalDateTime createdAt) {
        this.id = id;
        this.projectName = projectName;
        this.category = category;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }
}
