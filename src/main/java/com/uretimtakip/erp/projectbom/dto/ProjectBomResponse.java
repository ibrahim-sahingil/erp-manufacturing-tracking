package com.uretimtakip.erp.projectbom.dto;

import com.uretimtakip.erp.projectbom.ProjectBom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ProjectBom icin response DTO.
 *
 * Ek alan: autopopulatedPartCount
 *   CREATE response'unda kac adet ProjectBomPart otomatik kopyalandi
 *   bilgisini frontend'e dondurur. Listele/GetById'de null kalir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBomResponse {

    private UUID id;
    private String projectName;
    private UUID bomProductId;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    /** (12. tur m2) Urun adedi carpani. */
    private Integer productQty;

    /**
     * Sadece CREATE response'unda dolu olur (otomatik kopyalanan parca sayisi).
     */
    private Integer autopopulatedPartCount;

    public static ProjectBomResponse fromEntity(ProjectBom e) {
        return ProjectBomResponse.builder()
                .id(e.getId())
                .projectName(e.getProjectName())
                .bomProductId(e.getBomProductId())
                .status(e.getStatus())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .publishedAt(e.getPublishedAt())
                .productQty(e.getProductQty())
                .build();
    }

    public static ProjectBomResponse fromEntityWithPartCount(
            ProjectBom e, int partCount) {
        ProjectBomResponse response = fromEntity(e);
        response.setAutopopulatedPartCount(partCount);
        return response;
    }
}