package com.uretimtakip.erp.shipment;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageItemRequest;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageItemResponse;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageItemUpdateRequest;
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
 * ShipmentPackageItem REST API (13. tur madde 4 — paket icerigi).
 *
 * Endpoint'ler:
 *   GET    /api/shipment-package-items?packageId= - satirlar (opsiyonel paket filtresi)
 *   GET    /api/shipment-package-items/{id}       - tek satir
 *   POST   /api/shipment-package-items            - satir ekle (paket OPEN olmali)
 *   PUT    /api/shipment-package-items/{id}       - miktar duzelt (paket OPEN olmali)
 *   DELETE /api/shipment-package-items/{id}       - satir cikar (paket OPEN olmali)
 */
@RestController
@RequestMapping("/api/shipment-package-items")
@RequiredArgsConstructor
public class ShipmentPackageItemController {

    private final ShipmentPackageItemService itemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentPackageItemResponse>>> list(
            @RequestParam(required = false) UUID packageId) {
        return ResponseEntity.ok(ApiResponse.success(itemService.listAll(packageId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentPackageItemResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentPackageItemResponse>> create(
            @Valid @RequestBody ShipmentPackageItemRequest request) {
        ShipmentPackageItemResponse created = itemService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Paket satiri eklendi", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentPackageItemResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ShipmentPackageItemUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Paket satiri guncellendi",
                        itemService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        itemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Paket satiri silindi", null));
    }
}
