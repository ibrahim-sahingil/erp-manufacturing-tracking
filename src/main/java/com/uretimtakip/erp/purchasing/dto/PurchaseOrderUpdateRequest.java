package com.uretimtakip.erp.purchasing.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * PurchaseOrder PARTIAL update icin request DTO.
 *
 * Tipik kullanim:
 *   {"name": "Yeni ad"}                                   - ad duzeltme
 *   {"status":"APPROVED","selected_quote_id":"uuid",
 *    "approval_note":"En uygun fiyat","approved_by":"X"}  - onay
 *   {"status":"ORDERED"}                                  - siparis ver
 *   {"status":"APPROVED"} / {"status":"DRAFT"}            - geri alma
 *   {"status":"CANCELLED"}                                - iptal
 *
 * Durum gecis kurallari ve kalem toplu guncellemeleri service'tedir.
 */
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderUpdateRequest {

    @Size(max = 200, message = "Grup adi en fazla 200 karakter olabilir")
    private String name;

    @Size(max = 20, message = "Durum en fazla 20 karakter olabilir")
    private String status;

    private UUID selectedQuoteId;

    private String approvalNote;

    @Size(max = 150, message = "Onaylayan en fazla 150 karakter olabilir")
    private String approvedBy;
}
