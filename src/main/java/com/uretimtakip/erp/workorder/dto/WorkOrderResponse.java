package com.uretimtakip.erp.workorder.dto;

import com.uretimtakip.erp.workorder.WorkOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderResponse {

    private UUID id;
    private UUID orderId;
    private UUID departmentId;
    private UUID workspaceId;
    private UUID assignedUserId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String status;
    private String notes;
    private String code;
    private LocalDateTime createdAt;

    public static WorkOrderResponse fromEntity(WorkOrder w) {
        return WorkOrderResponse.builder()
                .id(w.getId())
                .orderId(w.getOrderId())
                .departmentId(w.getDepartmentId())
                .workspaceId(w.getWorkspaceId())
                .assignedUserId(w.getAssignedUserId())
                .startDatetime(w.getStartDatetime())
                .endDatetime(w.getEndDatetime())
                .status(w.getStatus())
                .notes(w.getNotes())
                .code(w.getCode())
                .createdAt(w.getCreatedAt())
                .build();
    }
}