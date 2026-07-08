package com.uretimtakip.erp.material;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.material.dto.MaterialRequest;
import com.uretimtakip.erp.material.dto.MaterialResponse;
import com.uretimtakip.erp.material.dto.MaterialUpdateRequest;
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
 * Material REST API (malzeme kartoteki — arkadas istegi 5. tur #1).
 *
 * Endpoint'ler:
 *   GET    /api/materials        - tum malzemeler (?active=true ile sadece aktifler)
 *   GET    /api/materials/{id}   - tek malzeme
 *   POST   /api/materials        - yeni malzeme
 *   PUT    /api/materials/{id}   - PARTIAL guncelle (pasife alma dahil)
 *   DELETE /api/materials/{id}   - sil (FK bagi yok, serbest)
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> list(
            @RequestParam(name = "active", required = false, defaultValue = "false") boolean active) {
        return ResponseEntity.ok(ApiResponse.success(materialService.listAll(active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialResponse>> create(
            @Valid @RequestBody MaterialRequest request) {
        MaterialResponse created = materialService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Malzeme olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MaterialUpdateRequest request) {
        MaterialResponse updated = materialService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Malzeme guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        materialService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Malzeme silindi", null));
    }
}
