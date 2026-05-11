package com.uretimtakip.erp.order;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.order.dto.OrderRequest;
import com.uretimtakip.erp.order.dto.OrderResponse;
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
 * Order REST endpoint'leri.
 *
 * Endpoints:
 *   GET    /api/orders                  -> Tum siparisler (yeni->eski)
 *   GET    /api/orders/{id}             -> Tek siparis
 *   GET    /api/orders?status=ACTIVE    -> Belirli durumdakiler
 *   POST   /api/orders                  -> Yeni siparis
 *   PUT    /api/orders/{id}             -> Guncelle
 *   DELETE /api/orders/{id}             -> Sil
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> list(
            @RequestParam(required = false) String status) {

        List<OrderResponse> orders = (status != null && !status.isBlank())
                ? orderService.listByStatus(status)
                : orderService.listAll();

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody OrderRequest request) {

        OrderResponse created = orderService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Siparis olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody OrderRequest request) {

        OrderResponse updated = orderService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Siparis guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Siparis silindi", null));
    }
}