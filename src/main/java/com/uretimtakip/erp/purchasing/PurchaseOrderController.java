package com.uretimtakip.erp.purchasing;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderRequest;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderUpdateRequest;
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
 * PurchaseOrder REST API (toplu siparis gruplari).
 *
 * Endpoint'ler:
 *   GET    /api/purchase-orders        - tum gruplar (yeni -> eski)
 *   GET    /api/purchase-orders/{id}   - tek grup
 *   POST   /api/purchase-orders        - grup olustur (item_ids ile, transaksiyonel)
 *   PUT    /api/purchase-orders/{id}   - PARTIAL guncelle (onay/siparis/geri alma dahil)
 *   DELETE /api/purchase-orders/{id}   - sil (sadece DRAFT/CANCELLED; uyelik serbest kalir)
 */
@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> create(
            @Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse created = purchaseOrderService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Toplu siparis grubu olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseOrderUpdateRequest request) {
        PurchaseOrderResponse updated = purchaseOrderService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Toplu siparis grubu guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Toplu siparis grubu silindi", null));
    }
}
