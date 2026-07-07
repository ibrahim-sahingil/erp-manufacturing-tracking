package com.uretimtakip.erp.security;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.security.dto.AuthResponse;
import com.uretimtakip.erp.security.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoint'leri.
 *
 * Endpoint'ler:
 *   POST /api/auth/login
 *     Body: { "username": "admin", "password": "1234" }
 *     Response: { "success": true, "data": { "token": "...", ...user bilgileri... } }
 *
 * Bu endpoint /api/auth/** altinda oldugu icin SecurityConfig'te
 * permitAll() olarak isaretlendi - token GEREKMIYOR.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // (U5) Brute-force yavaslatma: IP bloklu ise dene bile deme
        String ip = http.getRemoteAddr();
        loginRateLimiter.assertNotBlocked(ip);
        try {
            AuthResponse response = authService.login(request);
            loginRateLimiter.reset(ip); // basarili giris sayaci temizler
            return ResponseEntity.ok(ApiResponse.success("Giris basarili", response));
        } catch (RuntimeException e) {
            loginRateLimiter.recordFailure(ip);
            throw e;
        }
    }
}