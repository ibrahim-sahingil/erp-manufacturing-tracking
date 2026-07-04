package com.uretimtakip.erp.workorder.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WorkOrder PARTIAL update icin request DTO.
 *
 * Create'ten (WorkOrderRequest) farki: hicbir alan zorunlu degil,
 * sadece gonderilen (non-null) alanlar degisir. Frontend tipik olarak
 * tekil degisiklik yollar:
 *   {"status": "inprogress"}              - dashboard'dan durum ilerletme
 *   {"status": "done", "assigned_user_id": "uuid"} - revize modali
 *
 * NOT: Onceki hali create DTO'sunu kullaniyordu; orderId ve
 * startDatetime @NotNull oldugu icin planlama ekranindaki revize
 * ozelligi her kayitta 400 aliyordu (bom-parts'takiyle ayni hata
 * sinifi). Bu DTO o hatayi da duzeltir.
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkOrderUpdateRequest {

    private UUID orderId;

    private UUID departmentId;

    private UUID workspaceId;

    private UUID assignedUserId;

    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    @Size(max = 50)
    private String status;

    private String notes;
}
