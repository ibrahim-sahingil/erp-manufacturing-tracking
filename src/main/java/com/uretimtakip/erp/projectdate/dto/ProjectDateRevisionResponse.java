package com.uretimtakip.erp.projectdate.dto;

import com.uretimtakip.erp.projectdate.ProjectDateRevision;
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
public class ProjectDateRevisionResponse {

    private UUID id;
    private UUID projectDateId;
    private LocalDate oldStart;
    private LocalDate oldEnd;
    private LocalDate newStart;
    private LocalDate newEnd;
    private String reason;
    private UUID revisedBy;
    private LocalDateTime createdAt;

    public static ProjectDateRevisionResponse fromEntity(ProjectDateRevision r) {
        return ProjectDateRevisionResponse.builder()
                .id(r.getId())
                .projectDateId(r.getProjectDateId())
                .oldStart(r.getOldStart())
                .oldEnd(r.getOldEnd())
                .newStart(r.getNewStart())
                .newEnd(r.getNewEnd())
                .reason(r.getReason())
                .revisedBy(r.getRevisedBy())
                .createdAt(r.getCreatedAt())
                .build();
    }
}