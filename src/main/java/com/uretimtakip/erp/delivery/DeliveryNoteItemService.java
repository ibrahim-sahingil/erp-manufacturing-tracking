package com.uretimtakip.erp.delivery;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteItemRequest;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DeliveryNoteItem is mantigi.
 * Kalem ekleme/silme YALNIZ DRAFT irsaliyede yapilabilir - sevk edilmis
 * belge sabit kalir (once sevki geri al). Kalem PUT'u yok: yanlis kalem
 * silinip yeniden girilir (warehouse_movements ile ayni desen).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryNoteItemService {

    private final DeliveryNoteItemRepository itemRepository;
    private final DeliveryNoteRepository noteRepository;

    @Transactional(readOnly = true)
    public List<DeliveryNoteItemResponse> listAll() {
        return itemRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(DeliveryNoteItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryNoteItemResponse> listByNote(UUID noteId) {
        return itemRepository.findByDeliveryNoteIdOrderByCreatedAtAsc(noteId)
                .stream()
                .map(DeliveryNoteItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryNoteItemResponse create(DeliveryNoteItemRequest request) {
        DeliveryNote note = noteRepository.findById(request.getDeliveryNoteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DeliveryNote", "id", request.getDeliveryNoteId()));
        requireDraft(note);

        DeliveryNoteItem item = DeliveryNoteItem.builder()
                .deliveryNoteId(note.getId())
                .warehouseId(request.getWarehouseId())
                .itemName(request.getItemName())
                .itemCode(request.getItemCode() != null && !request.getItemCode().isBlank()
                        ? request.getItemCode() : null)
                .quantity(request.getQuantity() != null
                        ? request.getQuantity() : BigDecimal.ONE)
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .notes(request.getNotes() != null && !request.getNotes().isBlank()
                        ? request.getNotes() : null)
                .build();

        DeliveryNoteItem saved = itemRepository.save(item);
        log.info("DeliveryNoteItem created: id={}, note={}, item={}, qty={}",
                saved.getId(), note.getNoteNo(), saved.getItemName(), saved.getQuantity());
        return DeliveryNoteItemResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        DeliveryNoteItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryNoteItem", "id", id));
        DeliveryNote note = noteRepository.findById(item.getDeliveryNoteId()).orElse(null);
        if (note != null) requireDraft(note);
        itemRepository.delete(item);
        log.info("DeliveryNoteItem deleted: id={}, item={}", id, item.getItemName());
    }

    private void requireDraft(DeliveryNote note) {
        if (!"DRAFT".equals(note.getStatus())) {
            throw new BusinessException(
                    "Kalemler yalniz taslak (DRAFT) irsaliyede degistirilebilir. "
                            + "Once sevki geri alin.",
                    "DELIVERY_NOTE_NOT_DRAFT");
        }
    }
}
