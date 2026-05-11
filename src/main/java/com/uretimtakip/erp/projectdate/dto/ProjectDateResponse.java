package com.uretimtakip.erp.projectdate.dto;

import com.uretimtakip.erp.projectdate.ProjectDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDateResponse {

    private UUID id;
    private UUID orderId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public static ProjectDateResponse fromEntity(ProjectDate p) {
        return ProjectDateResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .createdAt(p.getCreatedAt())
                .build();
    }
}