package com.uretimtakip.erp.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Istemci yapilandirmasi (E3).
 *
 * GET /api/config -> { base_url } (JSON SNAKE_CASE ile baseUrl -> base_url).
 * Frontend acilis QR fis linklerinin KALICI temel adresini buradan alir; bos
 * ise location.origin'e duser. permitAll (SecurityConfig) — hassas veri yok,
 * login oncesi de erisilebilir.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${app.base-url:}")
    private String baseUrl;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        // Map anahtarina Jackson SNAKE_CASE UYGULANMAZ (yalniz POJO alanlarina);
        // frontend base_url okudugu icin anahtar elle snake_case yazilir.
        Map<String, Object> data = new HashMap<>();
        data.put("base_url", baseUrl != null ? baseUrl : "");
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
