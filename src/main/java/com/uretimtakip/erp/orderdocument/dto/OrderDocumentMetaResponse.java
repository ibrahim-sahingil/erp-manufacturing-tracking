package com.uretimtakip.erp.orderdocument.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** (12. tur m1) Siparis dosyasi META'si — data icermez (indirme ayri uc). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDocumentMetaResponse {
    private UUID id;
    private UUID orderId;
    private String category;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private String uploadedBy;
    private LocalDateTime createdAt;
}
