package com.uretimtakip.erp.supplier;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.supplier.dto.SupplierRequest;
import com.uretimtakip.erp.supplier.dto.SupplierResponse;
import com.uretimtakip.erp.supplier.dto.SupplierUpdateRequest;
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
 * Supplier REST API (tedarikci kartoteki - arkadas istegi 3. tur #12).
 *
 * Endpoint'ler:
 *   GET    /api/suppliers        - tum tedarikciler (?active=true ile sadece aktifler)
 *   GET    /api/suppliers/{id}   - tek tedarikci
 *   POST   /api/suppliers        - yeni tedarikci
 *   PUT    /api/suppliers/{id}   - PARTIAL guncelle (pasife alma dahil)
 *   DELETE /api/suppliers/{id}   - sil (FK bagi yok, serbest)
 */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> list(
            @RequestParam(name = "active", required = false, defaultValue = "false") boolean active) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.listAll(active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponse>> create(
            @Valid @RequestBody SupplierRequest request) {
        SupplierResponse created = supplierService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tedarikci olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateRequest request) {
        SupplierResponse updated = supplierService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Tedarikci guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Tedarikci silindi", null));
    }
}
