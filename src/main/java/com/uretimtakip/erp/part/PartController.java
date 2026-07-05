package com.uretimtakip.erp.part;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.part.dto.PartRequest;
import com.uretimtakip.erp.part.dto.PartResponse;
import com.uretimtakip.erp.part.dto.PartUpdateRequest;
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
 * Part REST endpoints. SADECE Part CRUD.
 *
 *   GET    /api/parts                        -> Tum parcalar
 *   GET    /api/parts/{id}                   -> Tek parca
 *   GET    /api/parts?orderId=xxx            -> Belirli siparis parcalari
 *   GET    /api/parts?departmentId=xxx       -> Belirli departman parcalari
 *   POST   /api/parts                        -> Yeni parca
 *   PUT    /api/parts/{id}                   -> Guncelle
 *   DELETE /api/parts/{id}                   -> Sil
 *
 * NOT: Uretim log'lari (PartLog) ayri endpoint'te:
 *   POST /api/part-logs  -> Uretim kaydi ekle (qty otomatik artar)
 */
@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PartResponse>>> list(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID departmentId) {

        List<PartResponse> parts;
        if (orderId != null) {
            parts = partService.listByOrder(orderId);
        } else if (departmentId != null) {
            parts = partService.listByDepartment(departmentId);
        } else {
            parts = partService.listAll();
        }

        return ResponseEntity.ok(ApiResponse.success(parts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartResponse>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(partService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PartResponse>> create(
            @Valid @RequestBody PartRequest request) {

        PartResponse created = partService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Parca olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PartResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PartUpdateRequest request) {

        PartResponse updated = partService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Parca guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        partService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Parca silindi", null));
    }
}