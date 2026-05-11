package com.uretimtakip.erp.part.dto;

import com.uretimtakip.erp.part.PartLog;
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
public class PartLogResponse {

    private UUID id;
    private UUID partId;
    private UUID userId;
    private Integer qtyDone;
    private Integer qtyPending;
    private Integer qtyReject;
    private String note;
    private LocalDateTime createdAt;

    public static PartLogResponse fromEntity(PartLog log) {
        return PartLogResponse.builder()
                .id(log.getId())
                .partId(log.getPartId())
                .userId(log.getUserId())
                .qtyDone(log.getQtyDone())
                .qtyPending(log.getQtyPending())
                .qtyReject(log.getQtyReject())
                .note(log.getNote())
                .createdAt(log.getCreatedAt())
                .build();
    }
}