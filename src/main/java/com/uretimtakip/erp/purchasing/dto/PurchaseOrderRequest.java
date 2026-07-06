package com.uretimtakip.erp.purchasing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * PurchaseOrder CREATE icin request DTO.
 *
 * Ornek:
 *   { "name": "Toplu Siparis 06.07.2026", "item_ids": ["uuid1","uuid2"],
 *     "created_by": "Ibrahim" }
 *
 * item_ids'deki her kalem: mevcut + PLANNED + baska gruba bagli degil olmali;
 * uyelik service'te AYNI transaksiyonda atanir (yarim grup olusmaz).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequest {

    @NotBlank(message = "Grup adi bos olamaz")
    @Size(max = 200, message = "Grup adi en fazla 200 karakter olabilir")
    private String name;

    @NotEmpty(message = "En az bir kalem secilmeli")
    private List<UUID> itemIds;

    @Size(max = 150, message = "Olusturan en fazla 150 karakter olabilir")
    private String createdBy;
}
