package com.uretimtakip.erp.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Yeni revize kaydi olusturma istegi.
 *
 * Ornek JSON:
 * {
 *   "workOrderId": "uuid-...",
 *   "fieldChanged": "status",
 *   "oldValue": "planned",
 *   "newValue": "in_progress",
 *   "reason": "Uretime baslandi",
 *   "revisedBy": "uuid-..."
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderRevisionRequest {

    @NotNull(message = "Is emri ID bos olamaz")
    private UUID workOrderId;

    @NotBlank(message = "Degisen alan bos olamaz")
    @Size(max = 100)
    private String fieldChanged;

    private String oldValue;

    private String newValue;

    @NotBlank(message = "Sebep bos olamaz")
    private String reason;

    private UUID revisedBy;
}