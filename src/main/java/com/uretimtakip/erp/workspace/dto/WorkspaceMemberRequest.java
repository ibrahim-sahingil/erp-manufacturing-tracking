package com.uretimtakip.erp.workspace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberRequest {

    @NotNull(message = "Workspace id bos olamaz")
    private UUID workspaceId;

    @NotNull(message = "User id bos olamaz")
    private UUID userId;

    @Size(max = 100)
    private String role;
}