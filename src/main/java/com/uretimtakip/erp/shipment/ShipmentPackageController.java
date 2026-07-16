package com.uretimtakip.erp.shipment;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageRequest;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageResponse;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageUpdateRequest;
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
 * ShipmentPackage REST API (13. tur madde 4 — Sevkiyat paketleme).
 *
 * Endpoint'ler:
 *   GET    /api/shipment-packages      - tum paketler (yeniden eskiye)
 *   GET    /api/shipment-packages/{id} - tek paket
 *   POST   /api/shipment-packages      - yeni paket (package_no backend uretir, OPEN baslar)
 *   PUT    /api/shipment-packages/{id} - PARTIAL guncelle + durum gecisleri
 *   DELETE /api/shipment-packages/{id} - sil (LOADED/SHIPPED silinemez)
 */
@RestController
@RequestMapping("/api/shipment-packages")
@RequiredArgsConstructor
public class ShipmentPackageController {

    private final ShipmentPackageService shipmentPackageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentPackageResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(shipmentPackageService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentPackageResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(shipmentPackageService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentPackageResponse>> create(
            @Valid @RequestBody ShipmentPackageRequest request) {
        ShipmentPackageResponse created = shipmentPackageService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Paket olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentPackageResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ShipmentPackageUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Paket guncellendi",
                        shipmentPackageService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        shipmentPackageService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Paket silindi", null));
    }
}
