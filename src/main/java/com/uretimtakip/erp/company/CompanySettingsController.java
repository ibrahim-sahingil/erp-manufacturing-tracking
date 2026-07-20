package com.uretimtakip.erp.company;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.company.dto.CompanySettingsRequest;
import com.uretimtakip.erp.company.dto.CompanySettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CompanySettings REST API (15. tur Y2a — sabit firma ayarlari, TEK SATIR).
 *
 * Endpoint'ler (id'siz — tek satir semantigi):
 *   GET /api/company-settings - ayarlar (satir yoksa bos yaratilir)
 *   PUT /api/company-settings - PARTIAL guncelle (yazma: delivery/shipping yetkisi)
 */
@RestController
@RequestMapping("/api/company-settings")
@RequiredArgsConstructor
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<CompanySettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(companySettingsService.get()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CompanySettingsResponse>> update(
            @Valid @RequestBody CompanySettingsRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Firma ayarlari guncellendi",
                        companySettingsService.update(request)));
    }
}
