package com.uretimtakip.erp.delivery;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteItemRequest;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteItemResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * DeliveryNoteItem REST API.
 *
 * Endpoint'ler:
 *   GET    /api/delivery-note-items                 - tum kalemler
 *   GET    /api/delivery-note-items/by-note/{noteId} - bir irsaliyenin kalemleri
 *   POST   /api/delivery-note-items                 - kalem ekle (yalniz DRAFT)
 *   DELETE /api/delivery-note-items/{id}            - kalem sil (yalniz DRAFT)
 * (PUT yok - kalem duzeltilmez, silinip yeniden girilir.)
 */
@RestController
@RequestMapping("/api/delivery-note-items")
@RequiredArgsConstructor
public class DeliveryNoteItemController {

    private final DeliveryNoteItemService itemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryNoteItemResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(itemService.listAll()));
    }

    @GetMapping("/by-note/{noteId}")
    public ResponseEntity<ApiResponse<List<DeliveryNoteItemResponse>>> listByNote(
            @PathVariable UUID noteId) {
        return ResponseEntity.ok(ApiResponse.success(itemService.listByNote(noteId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryNoteItemResponse>> create(
            @Valid @RequestBody DeliveryNoteItemRequest request) {
        DeliveryNoteItemResponse created = itemService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Kalem eklendi", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        itemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Kalem silindi", null));
    }
}
