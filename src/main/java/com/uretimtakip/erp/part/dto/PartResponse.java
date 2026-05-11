package com.uretimtakip.erp.part.dto;

import com.uretimtakip.erp.part.Part;
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
public class PartResponse {

    private UUID id;
    private String name;
    private String code;
    private UUID orderId;
    private UUID departmentId;
    private String drawingNo;
    private String material;
    private Integer totalQty;
    private String status;
    private String description;
    private Integer qtyDone;
    private Integer qtyPending;
    private Integer qtyReject;
    private LocalDateTime createdAt;

    public static PartResponse fromEntity(Part p) {
        return PartResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .orderId(p.getOrderId())
                .departmentId(p.getDepartmentId())
                .drawingNo(p.getDrawingNo())
                .material(p.getMaterial())
                .totalQty(p.getTotalQty())
                .status(p.getStatus())
                .description(p.getDescription())
                .qtyDone(p.getQtyDone())
                .qtyPending(p.getQtyPending())
                .qtyReject(p.getQtyReject())
                .createdAt(p.getCreatedAt())
                .build();
    }
}