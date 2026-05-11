package com.uretimtakip.erp.workspace.dto;

import com.uretimtakip.erp.workspace.Workspace;
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
public class WorkspaceResponse {

    private UUID id;
    private String name;
    private String type;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public static WorkspaceResponse fromEntity(Workspace w) {
        return WorkspaceResponse.builder()
                .id(w.getId())
                .name(w.getName())
                .type(w.getType())
                .description(w.getDescription())
                .sortOrder(w.getSortOrder())
                .createdAt(w.getCreatedAt())
                .build();
    }
}