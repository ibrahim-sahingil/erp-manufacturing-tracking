package com.uretimtakip.erp.projectbom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @NotBlank(message = "Proje adi bos olamaz")
    @Size(max = 100, message = "Proje adi en fazla 100 karakter olabilir")
    private String projectName;

    @NotNull(message = "bomProductId zorunlu")
    private UUID bomProductId;

    @Size(max = 20, message = "Status en fazla 20 karakter olabilir")
    private String status;

    @Size(max = 150, message = "createdBy en fazla 150 karakter olabilir")
    private String createdBy;
}