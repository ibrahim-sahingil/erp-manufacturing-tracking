package com.uretimtakip.erp.workorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Work order olusturma/guncelleme istegi.
 *
 * Ornek JSON:
 * {
 *   "orderId": "uuid-...",
 *   "departmentId": "uuid-...",
 *   "workspaceId": "uuid-...",
 *   "assignedUserId": "uuid-...",
 *   "startDatetime": "2026-05-10T08:00:00",
 *   "endDatetime": "2026-06-10T17:00:00",
 *   "status": "planned",
 *   "notes": "..."
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderRequest {

    @NotNull(message = "Siparis ID bos olamaz")
    private UUID orderId;

    private UUID departmentId;

    private UUID workspaceId;

    private UUID assignedUserId;

    @NotNull(message = "Baslangic zamani bos olamaz")
    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    @Size(max = 50)
    private String status;

    private String notes;
}