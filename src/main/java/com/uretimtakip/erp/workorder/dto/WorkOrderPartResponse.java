package com.uretimtakip.erp.workorder.dto;

import com.uretimtakip.erp.workorder.WorkOrderPart;
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
public class WorkOrderPartResponse {

    private UUID id;
    private UUID workOrderId;
    private UUID partId;
    private Integer qty;
    private LocalDateTime createdAt;

    public static WorkOrderPartResponse fromEntity(WorkOrderPart wp) {
        return WorkOrderPartResponse.builder()
                .id(wp.getId())
                .workOrderId(wp.getWorkOrderId())
                .partId(wp.getPartId())
                .qty(wp.getQty())
                .createdAt(wp.getCreatedAt())
                .build();
    }
}