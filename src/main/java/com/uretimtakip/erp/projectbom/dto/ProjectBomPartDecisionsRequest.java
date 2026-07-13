package com.uretimtakip.erp.projectbom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * (9. tur M4) TOPLU MIP karari request DTO'su.
 * POST /api/project-bom-parts/decisions
 *
 * Ornek:
 *   { "items": [ {"id":"...","decision":"PURCHASE"},
 *                {"id":"...","decision":"PRODUCE"} ],
 *     "decided_by": "İbrahim" }
 *
 * decision "" gonderilirse karar GERI ALINIR (karar bekliyor).
 * MIP "Tumunu Oneriyle Onayla" butonu bunu kullanir — tek transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectBomPartDecisionsRequest {

    @NotEmpty(message = "En az bir karar kalemi gerekli")
    @Valid
    private List<Item> items;

    @Size(max = 150, message = "Karar veren en fazla 150 karakter olabilir")
    private String decidedBy;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotNull(message = "Parca id'si bos olamaz")
        private UUID id;

        // (10. tur M5) POOL = kesim planlama havuzuna aktar
        @Pattern(regexp = "^(PURCHASE|PRODUCE|POOL)?$",
                message = "Karar PURCHASE, PRODUCE veya POOL olmali")
        private String decision;
    }
}
