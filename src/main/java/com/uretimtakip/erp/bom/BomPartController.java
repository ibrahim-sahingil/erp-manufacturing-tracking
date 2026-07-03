package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomPartRequest;
import com.uretimtakip.erp.bom.dto.BomPartResponse;
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
 * BomPart REST API.
 *
 * Endpoint'ler:
 *   GET    /api/bom-parts                       - tum parcalar (level/sort sirali)
 *   GET    /api/bom-parts/by-product/{productId} - bir urunun parcalari
 *   GET    /api/bom-parts/by-parent/{parentId}   - bir parent'in alt parcalari
 *   GET    /api/bom-parts/{id}                   - tek parca
 *   POST   /api/bom-parts                        - yeni parca (parent+product validation)
 *   PUT    /api/bom-parts/{id}                   - guncelle (productId/parentId/level IGNORE)
 *   DELETE /api/bom-parts/{id}                   - sil (child'lar kontrolu)
 */
@RestController
@RequestMapping("/api/bom-parts")
@RequiredArgsConstructor
public class BomPartController {

    private final BomPartService bomPartService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BomPartResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(bomPartService.listAll()));
    }

    @GetMapping("/by-product/{productId}")
    public ResponseEntity<ApiResponse<List<BomPartResponse>>> listByProduct(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(
                ApiResponse.success(bomPartService.listByProduct(productId)));
    }

    @GetMapping("/by-parent/{parentId}")
    public ResponseEntity<ApiResponse<List<BomPartResponse>>> listByParent(
            @PathVariable UUID parentId) {
        return ResponseEntity.ok(
                ApiResponse.success(bomPartService.listByParent(parentId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BomPartResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bomPartService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BomPartResponse>> create(
            @Valid @RequestBody BomPartRequest request) {
        BomPartResponse created = bomPartService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Parca olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BomPartResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BomPartRequest request) {
        BomPartResponse updated = bomPartService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Parca guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        bomPartService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Parca silindi", null));
    }
}