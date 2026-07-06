package com.uretimtakip.erp.purchasing;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderQuoteRequest;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderQuoteResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderQuoteUpdateRequest;
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
 * PurchaseOrderQuote REST API (firma teklifleri).
 *
 * Endpoint'ler:
 *   GET    /api/purchase-order-quotes        - tum teklifler (?order={id} ile filtre)
 *   POST   /api/purchase-order-quotes        - yeni teklif (sadece DRAFT gruba)
 *   PUT    /api/purchase-order-quotes/{id}   - PARTIAL guncelle (rejection_reason dahil)
 *   DELETE /api/purchase-order-quotes/{id}   - sil (secili teklif silinemez)
 */
@RestController
@RequestMapping("/api/purchase-order-quotes")
@RequiredArgsConstructor
public class PurchaseOrderQuoteController {

    private final PurchaseOrderQuoteService quoteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderQuoteResponse>>> list(
            @RequestParam(name = "order", required = false) UUID order) {
        List<PurchaseOrderQuoteResponse> quotes = order != null
                ? quoteService.listByOrder(order)
                : quoteService.listAll();
        return ResponseEntity.ok(ApiResponse.success(quotes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderQuoteResponse>> create(
            @Valid @RequestBody PurchaseOrderQuoteRequest request) {
        PurchaseOrderQuoteResponse created = quoteService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Teklif kaydedildi", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderQuoteResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseOrderQuoteUpdateRequest request) {
        PurchaseOrderQuoteResponse updated = quoteService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Teklif guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        quoteService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Teklif silindi", null));
    }
}
