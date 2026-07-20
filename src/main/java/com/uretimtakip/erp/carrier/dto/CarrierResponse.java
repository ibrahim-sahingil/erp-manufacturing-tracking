package com.uretimtakip.erp.carrier.dto;

import com.uretimtakip.erp.carrier.Carrier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Carrier API cevabi. JSON'da global SNAKE_CASE ile doner. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierResponse {

    private UUID id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String taxOffice;
    private String taxNumber;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static CarrierResponse fromEntity(Carrier e) {
        return CarrierResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .contactPerson(e.getContactPerson())
                .phone(e.getPhone())
                .email(e.getEmail())
                .address(e.getAddress())
                .taxOffice(e.getTaxOffice())
                .taxNumber(e.getTaxNumber())
                .notes(e.getNotes())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
