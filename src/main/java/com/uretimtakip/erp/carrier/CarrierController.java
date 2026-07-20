package com.uretimtakip.erp.carrier;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.carrier.dto.CarrierRequest;
import com.uretimtakip.erp.carrier.dto.CarrierResponse;
import com.uretimtakip.erp.carrier.dto.CarrierUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Carrier REST API (nakliye firmasi kartoteki - arkadas istegi 3. tur #12).
 *
 * Endpoint'ler:
 *   GET    /api/carriers        - tum nakliye firmalari (?active=true ile sadece aktifler)
 *   GET    /api/carriers/{id}   - tek nakliye firmasi
 *   POST   /api/carriers        - yeni nakliye firmasi
 *   PUT    /api/carriers/{id}   - PARTIAL guncelle (pasife alma dahil)
 *   DELETE /api/carriers/{id}   - sil (FK bagi yok, serbest)
 */
@RestController
@RequestMapping("/api/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierService carrierService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarrierResponse>>> list(
            @RequestParam(name = "active", required = false, defaultValue = "false") boolean active) {
        return ResponseEntity.ok(ApiResponse.success(carrierService.listAll(active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarrierResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(carrierService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarrierResponse>> create(
            @Valid @RequestBody CarrierRequest request) {
        CarrierResponse created = carrierService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nakliye firmasi olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CarrierResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CarrierUpdateRequest request) {
        CarrierResponse updated = carrierService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Nakliye firmasi guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        carrierService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Nakliye firmasi silindi", null));
    }
}
