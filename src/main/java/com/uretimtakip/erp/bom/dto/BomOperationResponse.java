package com.uretimtakip.erp.bom.dto;

import com.uretimtakip.erp.bom.BomOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BomOperation icin response DTO.
 *
 * fromEntity static method'u Entity -> DTO donusumu yapar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomOperationResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    /** (7. tur #1) Islemin bolum adi — JSON'da department_name. */
    private String departmentName;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public static BomOperationResponse fromEntity(BomOperation e) {
        return BomOperationResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .description(e.getDescription())
                .departmentName(e.getDepartmentName())
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt())
                .build();
    }
}