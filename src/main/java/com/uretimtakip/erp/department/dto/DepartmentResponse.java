package com.uretimtakip.erp.department.dto;

import com.uretimtakip.erp.department.Department;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Frontend'e donulen departman verisi.
 *
 * Ornek JSON:
 *   {
 *     "id": "uuid-...",
 *     "name": "Kaynak",
 *     "orderId": "uuid-...",
 *     "sortOrder": 1,
 *     "createdAt": "2026-05-05T..."
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    private UUID id;
    private String name;
    private UUID orderId;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    /**
     * Entity'den DTO'ya cevirir.
     * Boylece controller/service icinde tekrar tekrar yazmana gerek kalmaz.
     */
    public static DepartmentResponse fromEntity(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .orderId(department.getOrderId())
                .sortOrder(department.getSortOrder())
                .createdAt(department.getCreatedAt())
                .build();
    }
}