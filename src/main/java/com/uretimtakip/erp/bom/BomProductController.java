package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomProductRequest;
import com.uretimtakip.erp.bom.dto.BomProductResponse;
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
 * BomProduct REST API.
 *
 * Endpoint'ler:
 *   GET    /api/bom-products         - tum urun sablonlari
 *   GET    /api/bom-products/{id}    - tek urun
 *   POST   /api/bom-products         - yeni urun sablonu
 *   PUT    /api/bom-products/{id}    - guncelle
 *   DELETE /api/bom-products/{id}    - sil (kullanimda degilse)
 */
@RestController
@RequestMapping("/api/bom-products")
@RequiredArgsConstructor
public class BomProductController {

    private final BomProductService bomProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BomProductResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(bomProductService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BomProductResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bomProductService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BomProductResponse>> create(
            @Valid @RequestBody BomProductRequest request) {
        BomProductResponse created = bomProductService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Urun sablonu olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BomProductResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BomProductRequest request) {
        BomProductResponse updated = bomProductService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Urun sablonu guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        bomProductService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Urun sablonu silindi", null));
    }
}