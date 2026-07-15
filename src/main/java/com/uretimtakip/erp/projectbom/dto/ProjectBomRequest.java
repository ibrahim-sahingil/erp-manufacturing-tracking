package com.uretimtakip.erp.projectbom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ProjectBom create/update icin request DTO.
 *
 * Ornek JSON (CREATE):
 *   {
 *     "projectName": "2026-001 Musteri ABC",
 *     "bomProductId": "uuid",
 *     "status": "draft",
 *     "createdBy": "admin"
 *   }
 *
 * NOT (CREATE icin):
 *   Bu istek atildiginda backend:
 *   1. ProjectBom kaydi olusur
 *   2. BomProduct'un tum BomPart'lari otomatik kopyalanir
 *      (ProjectBomPart kayitlari, bomPartId referansli, custom_X null)
 *
 * NOT (UPDATE icin):
 *   bomProductId IMMUTABLE - degisirse alttaki kopyalanmis parcalar
 *   tutarsiz hale gelir. Service tarafindan IGNORE edilir.
 *   Sadece projectName, status, createdBy degisebilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBomRequest {

    /**
     * CREATE'te zorunlu (service kontrol eder). UPDATE'te opsiyonel:
     * frontend yayinlama sirasinda sadece {status, published_at} gonderir,
     * bos gelen alanlar mevcut degerleriyle korunur.
     */
    @Size(max = 100, message = "Proje adi en fazla 100 karakter olabilir")
    private String projectName;

    /** CREATE'te zorunlu (service kontrol eder). UPDATE'te zaten IMMUTABLE. */
    private UUID bomProductId;

    @Size(max = 20, message = "Status en fazla 20 karakter olabilir")
    private String status;

    @Size(max = 150, message = "createdBy en fazla 150 karakter olabilir")
    private String createdBy;

    /** Yayinlanma zamani (opsiyonel; frontend published_at olarak gonderir). */
    private LocalDateTime publishedAt;

    /**
     * (12. tur m2) Urun adedi carpani. CREATE'te bos gelirse 1; UPDATE'te
     * bos gelirse mevcut deger korunur (partial update deseni — service).
     */
    @Min(value = 1, message = "Urun adedi en az 1 olmalidir")
    private Integer productQty;
}