package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.warehouse.dto.WarehouseRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseResponse;
import com.uretimtakip.erp.warehouse.dto.WarehouseUpdateRequest;
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
 * Warehouse REST API (depo tanimlari).
 *
 * Endpoint'ler:
 *   GET    /api/warehouses          - tum depolar (?active=true ile sadece aktifler)
 *   GET    /api/warehouses/{id}     - tek depo
 *   POST   /api/warehouses          - yeni depo
 *   PUT    /api/warehouses/{id}     - PARTIAL guncelle (pasife alma dahil)
 *   DELETE /api/warehouses/{id}     - sil (hareketi/bagli kalemi varsa 400)
 */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> list(
            @RequestParam(name = "active", required = false, defaultValue = "false") boolean active) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.listAll(active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(
            @Valid @RequestBody WarehouseRequest request) {
        WarehouseResponse created = warehouseService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Depo olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WarehouseUpdateRequest request) {
        WarehouseResponse updated = warehouseService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Depo guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        warehouseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Depo silindi", null));
    }
}
