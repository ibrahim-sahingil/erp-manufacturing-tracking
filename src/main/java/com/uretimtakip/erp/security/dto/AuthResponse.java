package com.uretimtakip.erp.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Login basarili olunca frontend'e donulen veri.
 *
 * Ornek JSON:
 * {
 *   "token": "eyJhbGc...",
 *   "tokenType": "Bearer",
 *   "userId": "31e32569-...",
 *   "username": "admin",
 *   "fullName": "Sistem Yoneticisi",
 *   "role": "developer",
 *   "permissions": ["all"]
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private UUID userId;
    private String username;
    private String fullName;
    private String role;
    private String department;
    private List<String> permissions;
    private Long expiresInMs;
}