package com.uretimtakip.erp.material.dto;

import com.uretimtakip.erp.material.Material;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Material API cevabi. JSON'da global SNAKE_CASE ile doner. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialResponse {

    private UUID id;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static MaterialResponse fromEntity(Material e) {
        return MaterialResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
