package com.uretimtakip.erp.workorder.dto;

import com.uretimtakip.erp.workorder.WorkOrderRevision;
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
public class WorkOrderRevisionResponse {

    private UUID id;
    private UUID workOrderId;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private String reason;
    private UUID revisedBy;
    private LocalDateTime createdAt;

    public static WorkOrderRevisionResponse fromEntity(WorkOrderRevision r) {
        return WorkOrderRevisionResponse.builder()
                .id(r.getId())
                .workOrderId(r.getWorkOrderId())
                .fieldChanged(r.getFieldChanged())
                .oldValue(r.getOldValue())
                .newValue(r.getNewValue())
                .reason(r.getReason())
                .revisedBy(r.getRevisedBy())
                .createdAt(r.getCreatedAt())
                .build();
    }
}