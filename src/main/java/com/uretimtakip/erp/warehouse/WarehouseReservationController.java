package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.warehouse.dto.WarehouseReservationApproveRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseReservationRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseReservationResponse;
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
 * WarehouseReservation REST API (MIP Asama 2 - depoya rezervasyon is emri).
 *
 * Endpoint'ler:
 *   GET    /api/warehouse-reservations              - talepler (?status= filtre)
 *   POST   /api/warehouse-reservations              - yeni talep (MIP ekrani; 'mip' yetkisi)
 *   POST   /api/warehouse-reservations/{id}/approve - tam/kismi onay ('warehouse' yetkisi)
 *   POST   /api/warehouse-reservations/{id}/cancel  - REQUESTED iptali
 *   DELETE /api/warehouse-reservations/{id}         - sil (guard'siz, bilincli - servise bak)
 *
 * Yetki kurallari SecurityConfig'te (approve yalniz warehouse; digerleri mip+warehouse).
 */
@RestController
@RequestMapping("/api/warehouse-reservations")
@RequiredArgsConstructor
public class WarehouseReservationController {

    private final WarehouseReservationService warehouseReservationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseReservationResponse>>> list(
            @RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(warehouseReservationService.list(status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseReservationResponse>> create(
            @Valid @RequestBody WarehouseReservationRequest request) {
        WarehouseReservationResponse created = warehouseReservationService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rezervasyon talebi depoya iletildi", created));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<WarehouseReservationResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody WarehouseReservationApproveRequest request) {
        WarehouseReservationResponse approved = warehouseReservationService.approve(id, request);
        return ResponseEntity.ok(ApiResponse.success("Rezervasyon sonuclandi", approved));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<WarehouseReservationResponse>> cancel(@PathVariable UUID id) {
        WarehouseReservationResponse cancelled = warehouseReservationService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success("Rezervasyon talebi iptal edildi", cancelled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        warehouseReservationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Rezervasyon kaydi silindi", null));
    }
}
