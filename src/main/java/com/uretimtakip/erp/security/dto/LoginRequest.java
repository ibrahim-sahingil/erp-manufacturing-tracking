package com.uretimtakip.erp.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Login formundan gelen veri.
 * Frontend bu format'ta JSON gonderir:
 *   { "username": "admin", "password": "1234" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Kullanici adi bos olamaz")
    private String username;

    @NotBlank(message = "Sifre bos olamaz")
    private String password;
}