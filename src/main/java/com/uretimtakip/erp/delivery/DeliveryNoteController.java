package com.uretimtakip.erp.delivery;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteRequest;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteResponse;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteUpdateRequest;
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
 * DeliveryNote REST API (dahili sevk irsaliyesi).
 *
 * Endpoint'ler:
 *   GET    /api/delivery-notes      - tum irsaliyeler (yeniden eskiye)
 *   GET    /api/delivery-notes/{id} - tek irsaliye
 *   POST   /api/delivery-notes      - yeni irsaliye (note_no backend uretir)
 *   PUT    /api/delivery-notes/{id} - PARTIAL guncelle + durum gecisleri
 *   DELETE /api/delivery-notes/{id} - sil (SHIPPED silinemez)
 */
@RestController
@RequestMapping("/api/delivery-notes")
@RequiredArgsConstructor
public class DeliveryNoteController {

    private final DeliveryNoteService deliveryNoteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryNoteResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(deliveryNoteService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryNoteResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deliveryNoteService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryNoteResponse>> create(
            @Valid @RequestBody DeliveryNoteRequest request) {
        DeliveryNoteResponse created = deliveryNoteService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Irsaliye olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryNoteResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryNoteUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Irsaliye guncellendi",
                        deliveryNoteService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        deliveryNoteService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Irsaliye silindi", null));
    }
}
