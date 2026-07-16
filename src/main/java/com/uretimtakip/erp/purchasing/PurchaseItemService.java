package com.uretimtakip.erp.purchasing;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemRequest;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseItemUpdateRequest;
import com.uretimtakip.erp.warehouse.WarehouseRepository;
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
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

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
                // (10. tur M5) MIP havuz karari kalemi dogrudan havuzda olusturur
                .needsPlanning(Boolean.TRUE.equals(request.getNeedsPlanning()))
                // (13. tur madde 2) verilmezse TRUE — elle giris eski davranis;
                // MIP havuz/MRP plan akislari false gonderir (otomatik dusme yok)
                .sentToPurchasing(request.getSentToPurchasing() == null
                        || Boolean.TRUE.equals(request.getSentToPurchasing()))
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
        if (request.getNeedsPlanning() != null) {
            item.setNeedsPlanning(request.getNeedsPlanning());
        }
        // (13. tur madde 2) MIP'ten "Satin Almaya Gonder" true yollar
        if (request.getSentToPurchasing() != null) {
            item.setSentToPurchasing(request.getSentToPurchasing());
        }
        // Mal kabul bilgileri (4. tur #3)
        if (request.isReceivedByPresent()) {
            item.setReceivedBy(request.getReceivedBy() != null
                    && !request.getReceivedBy().isBlank()
                    ? request.getReceivedBy() : null);
        }
        if (request.getReceivedQty() != null) {
            item.setReceivedQty(request.getReceivedQty());
        }
        // Bolunen kalemlere mal kabul/siparis damgasi tasinir (4. tur B4/B7)
        if (request.getReceivedAt() != null) {
            item.setReceivedAt(request.getReceivedAt());
        }
        if (request.getOrderedAt() != null) {
            item.setOrderedAt(request.getOrderedAt());
        }
        if (request.getReturnedQty() != null) {
            item.setReturnedQty(request.getReturnedQty());
        }
        if (request.isStockPlanIdPresent()) {
            // Plaka/profil bagi (#10 MRP): hedef kalem mevcut olmali
            if (request.getStockPlanId() != null
                    && !purchaseItemRepository.existsById(request.getStockPlanId())) {
                throw new BusinessException(
                        "Baglanacak plaka/profil kalemi bulunamadi.",
                        "PURCHASE_STOCK_PLAN_NOT_FOUND");
            }
            item.setStockPlanId(request.getStockPlanId());
        }
        if (request.isWarehouseIdPresent()) {
            if (request.getWarehouseId() != null
                    && !warehouseRepository.existsById(request.getWarehouseId())) {
                throw new BusinessException(
                        "Secilen depo bulunamadi.",
                        "PURCHASE_ITEM_WAREHOUSE_NOT_FOUND");
            }
            item.setWarehouseId(request.getWarehouseId());
        }
        if (request.isPurchaseOrderIdPresent()
                && !java.util.Objects.equals(request.getPurchaseOrderId(), item.getPurchaseOrderId())) {
            // Gruptan cikarma: mevcut grup DRAFT olmali (siparis grup uzerinden yonetiliyor)
            if (item.getPurchaseOrderId() != null) {
                assertOrderDraft(item.getPurchaseOrderId(),
                        "Kalem sadece taslak (DRAFT) gruptan cikarilabilir.");
            }
            // Gruba ekleme: hedef grup DRAFT + kalem PLANNED olmali
            if (request.getPurchaseOrderId() != null) {
                assertOrderDraft(request.getPurchaseOrderId(),
                        "Kalem sadece taslak (DRAFT) gruba eklenebilir.");
                if (!"PLANNED".equals(item.getStatus())) {
                    throw new BusinessException(
                            "Sadece PLANNED durumundaki kalem gruba eklenebilir.",
                            "PURCHASE_ORDER_ITEM_INVALID");
                }
            }
            item.setPurchaseOrderId(request.getPurchaseOrderId());
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
            // Mal Kabul'de ORDERED'dan dogrudan depoya alinabiliyor (#4):
            // siparisi verilmis kalem depoya girdiyse teslim de alinmistir.
            // (IN_STOCK yolunda orderedAt null oldugundan damga atilmaz;
            // whUndo'nun IN_STOCK'a geri dusme mantigi bozulmaz.)
            if ("IN_WAREHOUSE".equals(request.getStatus())
                    && item.getReceivedAt() == null && item.getOrderedAt() != null) {
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

        // (O4) Silme korkuluklari: depodaki kalem silinirse IN hareketi
        // sahipsiz kalir ve stok ozeti "munferit stok" gibi sisip kalemle
        // ayrisir; gruptaki kalem silinirse grup toplami sessizce degisir.
        // NOT: warehouse_id kontrolu bilinclidir — bolme telafisi (B6) depoya
        // BAGLANAMAMIS (warehouse_id null) IN_WAREHOUSE kaydini siler.
        if ("IN_WAREHOUSE".equals(item.getStatus()) && item.getWarehouseId() != null) {
            throw new BusinessException(
                    "Depodaki kalem silinemez. Once 'Depodan Geri Al' ile cikarin.",
                    "PURCHASE_ITEM_IN_WAREHOUSE");
        }
        if (item.getPurchaseOrderId() != null) {
            throw new BusinessException(
                    "Siparis grubuna bagli kalem silinemez. Once gruptan cikarin.",
                    "PURCHASE_ITEM_IN_ORDER_GROUP");
        }

        purchaseItemRepository.delete(item);
        log.info("PurchaseItem deleted: id={}, name={}", id, item.getName());
    }

    private void assertOrderDraft(java.util.UUID orderId, String message) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", orderId));
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(message, "PURCHASE_ORDER_LOCKED");
        }
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
