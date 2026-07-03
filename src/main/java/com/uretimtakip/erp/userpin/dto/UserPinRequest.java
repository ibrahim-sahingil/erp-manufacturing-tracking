package com.uretimtakip.erp.userpin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * UserPin create icin request DTO.
 *
 * Ornek JSON:
 *   { "user_id": "uuid", "pin_type": "project", "pin_key": "X Vinc Projesi" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPinRequest {

    @NotNull(message = "user_id zorunlu")
    private UUID userId;

    @NotBlank(message = "pin_type bos olamaz")
    @Size(max = 50, message = "pin_type en fazla 50 karakter olabilir")
    private String pinType;

    @NotBlank(message = "pin_key bos olamaz")
    @Size(max = 100, message = "pin_key en fazla 100 karakter olabilir")
    private String pinKey;
}
