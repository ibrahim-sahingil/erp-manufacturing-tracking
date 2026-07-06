package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.warehouse.dto.WarehouseMovementRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseMovementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * WarehouseMovement REST API (depo hareket defteri).
 *
 * Endpoint'ler:
 *   GET    /api/warehouse-movements        - tum hareketler (?warehouse={id} ile filtre)
 *   POST   /api/warehouse-movements        - yeni hareket (IN/OUT)
 *   DELETE /api/warehouse-movements/{id}   - sil (yanlis giris duzeltme yolu)
 *
 * PUT YOK: hareket defteri duzeltilmez.
 */
@RestController
@RequestMapping("/api/warehouse-movements")
@RequiredArgsConstructor
public class WarehouseMovementController {

    private final WarehouseMovementService warehouseMovementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseMovementResponse>>> list(
            @RequestParam(name = "warehouse", required = false) UUID warehouse) {
        List<WarehouseMovementResponse> movements = warehouse != null
                ? warehouseMovementService.listByWarehouse(warehouse)
                : warehouseMovementService.listAll();
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseMovementResponse>> create(
            @Valid @RequestBody WarehouseMovementRequest request) {
        WarehouseMovementResponse created = warehouseMovementService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Depo hareketi kaydedildi", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        warehouseMovementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Depo hareketi silindi", null));
    }
}
