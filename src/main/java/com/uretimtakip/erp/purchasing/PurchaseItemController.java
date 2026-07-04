package com.uretimtakip.erp.purchasing;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemRequest;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemUpdateRequest;
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
 * PurchaseItem REST API (satin alma planlamasi).
 *
 * Endpoint'ler:
 *   GET    /api/purchase-items                 - tum kalemler (?project= ile filtre)
 *   GET    /api/purchase-items/{id}            - tek kalem
 *   POST   /api/purchase-items                 - yeni kalem (elle veya BOM'dan aktarma)
 *   PUT    /api/purchase-items/{id}            - PARTIAL guncelle (durum gecisleri dahil)
 *   DELETE /api/purchase-items/{id}            - sil
 */
@RestController
@RequestMapping("/api/purchase-items")
@RequiredArgsConstructor
public class PurchaseItemController {

    private final PurchaseItemService purchaseItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseItemResponse>>> list(
            @RequestParam(name = "project", required = false) String project) {
        List<PurchaseItemResponse> items = (project != null && !project.isBlank())
                ? purchaseItemService.listByProject(project)
                : purchaseItemService.listAll();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseItemResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseItemService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseItemResponse>> create(
            @Valid @RequestBody PurchaseItemRequest request) {
        PurchaseItemResponse created = purchaseItemService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Satin alma kalemi olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseItemResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseItemUpdateRequest request) {
        PurchaseItemResponse updated = purchaseItemService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Satin alma kalemi guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        purchaseItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Satin alma kalemi silindi", null));
    }
}
