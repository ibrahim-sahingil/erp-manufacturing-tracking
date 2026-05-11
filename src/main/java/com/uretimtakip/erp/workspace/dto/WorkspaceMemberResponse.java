package com.uretimtakip.erp.workspace.dto;

import com.uretimtakip.erp.workspace.WorkspaceMember;
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
public class WorkspaceMemberResponse {

    private UUID id;
    private UUID workspaceId;
    private UUID userId;
    private String role;
    private LocalDateTime createdAt;

    public static WorkspaceMemberResponse fromEntity(WorkspaceMember m) {
        return WorkspaceMemberResponse.builder()
                .id(m.getId())
                .workspaceId(m.getWorkspaceId())
                .userId(m.getUserId())
                .role(m.getRole())
                .createdAt(m.getCreatedAt())
                .build();
    }
}