package com.uretimtakip.erp.company.dto;

import com.uretimtakip.erp.company.CompanySettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/** CompanySettings API cevabi — logo base64 olarak doner (JSON snake_case). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettingsResponse {

    private UUID id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String taxOffice;
    private String taxNumber;
    private String logoBase64;
    private String logoContentType;
    private LocalDateTime updatedAt;

    public static CompanySettingsResponse fromEntity(CompanySettings e) {
        return CompanySettingsResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .address(e.getAddress())
                .phone(e.getPhone())
                .email(e.getEmail())
                .taxOffice(e.getTaxOffice())
                .taxNumber(e.getTaxNumber())
                .logoBase64(e.getLogo() != null
                        ? Base64.getEncoder().encodeToString(e.getLogo()) : null)
                .logoContentType(e.getLogoContentType())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
