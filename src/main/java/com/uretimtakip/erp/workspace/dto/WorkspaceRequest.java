package com.uretimtakip.erp.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceRequest {

    @NotBlank(message = "Workspace adi bos olamaz")
    @Size(max = 150)
    private String name;

    @Size(max = 50)
    private String type;

    private String description;

    @PositiveOrZero(message = "Sira numarasi negatif olamaz")
    private Integer sortOrder;
}