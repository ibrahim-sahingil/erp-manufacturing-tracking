package com.uretimtakip.erp.purchasing;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemRequest;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PurchaseItem is mantigi.
 *
 * Update PARTIAL calisir: sadece gonderilen alanlar degisir.
 * Durum degisiminde damgalar otomatik atilir:
 *   -> ORDERED  : ordered_at = now (bossa)
 *   -> RECEIVED : received_at = now (bossa)
 * Kati durum makinesi YOK - kullanici yanlis tiklamayi geri alabilmeli.
 * Gecerli durum seti DB CHECK constraint'i ile ayni tutulur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseItemService {

    /** DB'deki purchase_items_status_check ile ayni liste. */
    private static final Set<String> VALID_STATUSES = Set.of(
            "PLANNED", "ORDERED", "RECEIVED", "IN_WAREHOUSE", "IN_STOCK", "CANCELLED");

    private final PurchaseItemRepository purchaseItemRepository;

    @Transactional(readOnly = true)
    public List<PurchaseItemResponse> listAll() {
        return purchaseItemRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(PurchaseItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseItemResponse> listByProject(String projectName) {
        return purchaseItemRepository.findByProjectNameOrderByCreatedAtAsc(projectName)
                .stream()
                .map(PurchaseItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseItemResponse getById(UUID id) {
        return PurchaseItemResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public PurchaseItemResponse create(PurchaseItemRequest request) {
        String status = request.getStatus() != null && !request.getStatus().isBlank()
                ? request.getStatus() : "PLANNED";
        validateStatus(status);

        // Ayni BOM parcasi ikinci kez ice aktarilmasin (elle kalemler etkilenmez)
        if (request.getProjectBomPartId() != null
                && purchaseItemRepository.existsByProjectBomPartId(request.getProjectBomPartId())) {
            throw new BusinessException(
                    "Bu urun agaci parcasi zaten satin alma listesinde.",
                    "PURCHASE_ITEM_DUPLICATE");
        }

        PurchaseItem item = PurchaseItem.builder()
                .projectName(request.getProjectName())
                .projectBomPartId(request.getProjectBomPartId())
                .name(request.getName())
                .code(request.getCode())
                .quantity(request.getQuantity() != null
                        ? request.getQuantity() : BigDecimal.ONE)
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .material(request.getMaterial())
                .supplier(request.getSupplier())
                .unitPrice(request.getUnitPrice())
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank()
                        ? request.getCurrency() : "TRY")
                .expectedDate(request.getExpectedDate())
                .status(status)
                .notes(request.getNotes())
                .createdBy(request.getCreatedBy())
                .build();

        PurchaseItem saved = purchaseItemRepository.save(item);
        log.info("PurchaseItem created: id={}, project={}, name={}, status={}",
                saved.getId(), saved.getProjectName(), saved.getName(), saved.getStatus());
        return PurchaseItemResponse.fromEntity(saved);
    }

    @Transactional
    public PurchaseItemResponse update(UUID id, PurchaseItemUpdateRequest request) {
        PurchaseItem item = findEntityById(id);

        // PARTIAL update: sadece gonderilen alanlar islenir.
        // IMMUTABLE: projectName, projectBomPartId.
        if (request.getName() != null && !request.getName().isBlank()) {
            item.setName(request.getName());
        }
        if (request.getCode() != null) {
            item.setCode(request.getCode());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            item.setUnit(request.getUnit());
        }
        if (request.getMaterial() != null) {
            item.setMaterial(request.getMaterial());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            item.setCurrency(request.getCurrency());
        }
        // Presence takipli alanlar: explicit null = temizle
        if (request.isSupplierPresent()) {
            item.setSupplier(request.getSupplier());
        }
        if (request.isUnitPricePresent()) {
            item.setUnitPrice(request.getUnitPrice());
        }
        if (request.isExpectedDatePresent()) {
            item.setExpectedDate(request.getExpectedDate());
        }
        if (request.isNotesPresent()) {
            item.setNotes(request.getNotes());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()
                && !request.getStatus().equals(item.getStatus())) {
            validateStatus(request.getStatus());
            item.setStatus(request.getStatus());
            // Damgalar: ilk gecise atilir, geri alinip tekrar gecilirse korunur
            if ("ORDERED".equals(request.getStatus()) && item.getOrderedAt() == null) {
                item.setOrderedAt(LocalDateTime.now());
            }
            if ("RECEIVED".equals(request.getStatus()) && item.getReceivedAt() == null) {
                item.setReceivedAt(LocalDateTime.now());
            }
        }

        PurchaseItem saved = purchaseItemRepository.save(item);
        log.info("PurchaseItem updated: id={}, status={}", saved.getId(), saved.getStatus());
        return PurchaseItemResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PurchaseItem item = findEntityById(id);
        purchaseItemRepository.delete(item);
        log.info("PurchaseItem deleted: id={}, name={}", id, item.getName());
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(
                    "Gecersiz durum: " + status + ". Gecerli degerler: " + VALID_STATUSES,
                    "PURCHASE_ITEM_INVALID_STATUS");
        }
    }

    private PurchaseItem findEntityById(UUID id) {
        return purchaseItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseItem", "id", id));
    }
}
