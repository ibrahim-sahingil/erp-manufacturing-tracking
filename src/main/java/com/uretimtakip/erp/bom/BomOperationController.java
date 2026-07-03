package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomOperationRequest;
import com.uretimtakip.erp.bom.dto.BomOperationResponse;
import com.uretimtakip.erp.common.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * BomOperation REST API.
 *
 * Endpoint'ler:
 *   GET    /api/bom-operations         - tum operasyonlari listele
 *   GET    /api/bom-operations/{id}    - tek operasyon
 *   POST   /api/bom-operations         - yeni operasyon
 *   PUT    /api/bom-operations/{id}    - guncelle
 *   DELETE /api/bom-operations/{id}    - sil (kullanimda degilse)
 */
@RestController
@RequestMapping("/api/bom-operations")
@RequiredArgsConstructor
public class BomOperationController {

    private final BomOperationService bomOperationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BomOperationResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(bomOperationService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BomOperationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bomOperationService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BomOperationResponse>> create(
            @Valid @RequestBody BomOperationRequest request) {
        BomOperationResponse created = bomOperationService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operasyon olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BomOperationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BomOperationRequest request) {
        BomOperationResponse updated = bomOperationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Operasyon guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        bomOperationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Operasyon silindi", null));
    }
}