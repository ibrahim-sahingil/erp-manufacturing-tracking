package com.uretimtakip.erp.purchasing;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderRequest;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PurchaseOrder (toplu siparis grubu) is mantigi.
 *
 * Durum gecisleri:
 *   DRAFT -> APPROVED : selected_quote_id (bu gruba ait) + approval_note ZORUNLU
 *   APPROVED -> ORDERED: ordered_at damgalanir; uye PLANNED kalemler topluca
 *                        ORDERED + supplier=kazanan firma + ordered_at yapilir
 *   ORDERED -> APPROVED: uye ORDERED kalemler PLANNED'a doner (damgalar korunur)
 *   APPROVED -> DRAFT  : onay alanlari temizlenir
 *   DRAFT/APPROVED -> CANCELLED: uyelik serbest birakilir; ORDERED once geri alinmali
 *
 * Grup olusturma TRANSAKSIYONEL: kalemler dogrulanip uyelik ayni islemde atanir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    /** DB'deki purchase_orders_status_check ile ayni liste. */
    private static final Set<String> VALID_STATUSES = Set.of(
            "DRAFT", "APPROVED", "ORDERED", "CANCELLED");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderQuoteRepository quoteRepository;
    private final PurchaseItemRepository purchaseItemRepository;

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> listAll() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PurchaseOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(UUID id) {
        return PurchaseOrderResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        // Kalemleri dogrula: mevcut + PLANNED + baska gruba bagli degil
        List<PurchaseItem> items = purchaseItemRepository.findAllById(request.getItemIds());
        if (items.size() != request.getItemIds().size()) {
            throw new BusinessException(
                    "Secilen kalemlerden bazilari bulunamadi.",
                    "PURCHASE_ORDER_ITEM_INVALID");
        }
        for (PurchaseItem item : items) {
            if (!"PLANNED".equals(item.getStatus())) {
                throw new BusinessException(
                        "\"" + item.getName() + "\" kalemi PLANNED durumunda degil, gruba eklenemez.",
                        "PURCHASE_ORDER_ITEM_INVALID");
            }
            if (item.getPurchaseOrderId() != null) {
                throw new BusinessException(
                        "\"" + item.getName() + "\" kalemi zaten baska bir toplu siparis grubunda.",
                        "PURCHASE_ORDER_ITEM_INVALID");
            }
        }

        PurchaseOrder order = PurchaseOrder.builder()
                .name(request.getName().trim())
                .createdBy(request.getCreatedBy())
                .build();
        PurchaseOrder saved = purchaseOrderRepository.save(order);

        for (PurchaseItem item : items) {
            item.setPurchaseOrderId(saved.getId());
        }
        purchaseItemRepository.saveAll(items);

        log.info("PurchaseOrder created: id={}, name={}, items={}",
                saved.getId(), saved.getName(), items.size());
        return PurchaseOrderResponse.fromEntity(saved);
    }

    @Transactional
    public PurchaseOrderResponse update(UUID id, PurchaseOrderUpdateRequest request) {
        PurchaseOrder order = findEntityById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            order.setName(request.getName().trim());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()
                && !request.getStatus().equals(order.getStatus())) {
            applyStatusTransition(order, request);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("PurchaseOrder updated: id={}, status={}", saved.getId(), saved.getStatus());
        return PurchaseOrderResponse.fromEntity(saved);
    }

    private void applyStatusTransition(PurchaseOrder order, PurchaseOrderUpdateRequest request) {
        String target = request.getStatus();
        if (!VALID_STATUSES.contains(target)) {
            throw new BusinessException(
                    "Gecersiz durum: " + target + ". Gecerli degerler: " + VALID_STATUSES,
                    "PURCHASE_ORDER_INVALID_STATUS");
        }
        String current = order.getStatus();

        switch (target) {
            case "APPROVED" -> {
                if ("ORDERED".equals(current)) {
                    // Siparisi geri al: uye ORDERED kalemler PLANNED'a doner
                    revertOrderedItems(order.getId());
                    order.setStatus("APPROVED");
                } else {
                    // Onay: secili teklif + aciklama zorunlu
                    UUID quoteId = request.getSelectedQuoteId() != null
                            ? request.getSelectedQuoteId() : order.getSelectedQuoteId();
                    String note = request.getApprovalNote() != null
                            ? request.getApprovalNote() : order.getApprovalNote();
                    if (quoteId == null || note == null || note.isBlank()) {
                        throw new BusinessException(
                                "Onay icin kazanan teklif ve onay aciklamasi zorunludur.",
                                "PURCHASE_ORDER_APPROVAL_INCOMPLETE");
                    }
                    PurchaseOrderQuote quote = quoteRepository.findById(quoteId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "PurchaseOrderQuote", "id", quoteId));
                    if (!order.getId().equals(quote.getPurchaseOrderId())) {
                        throw new BusinessException(
                                "Secilen teklif bu siparis grubuna ait degil.",
                                "PURCHASE_ORDER_APPROVAL_INCOMPLETE");
                    }
                    order.setSelectedQuoteId(quoteId);
                    order.setApprovalNote(note);
                    order.setApprovedBy(request.getApprovedBy());
                    order.setApprovedAt(LocalDateTime.now());
                    order.setStatus("APPROVED");
                }
            }
            case "ORDERED" -> {
                if (!"APPROVED".equals(current)) {
                    throw new BusinessException(
                            "Siparis verilebilmesi icin grubun once onaylanmasi gerekir.",
                            "PURCHASE_ORDER_NOT_APPROVED");
                }
                PurchaseOrderQuote winner = quoteRepository.findById(order.getSelectedQuoteId())
                        .orElseThrow(() -> new BusinessException(
                                "Kazanan teklif bulunamadi.", "PURCHASE_ORDER_NOT_APPROVED"));
                order.setOrderedAt(LocalDateTime.now());
                order.setStatus("ORDERED");
                // Uye PLANNED kalemler topluca siparis verilmis olur
                List<PurchaseItem> items = purchaseItemRepository.findByPurchaseOrderId(order.getId());
                LocalDateTime now = LocalDateTime.now();
                for (PurchaseItem item : items) {
                    if ("PLANNED".equals(item.getStatus())) {
                        item.setStatus("ORDERED");
                        item.setSupplier(winner.getSupplierName());
                        if (item.getOrderedAt() == null) item.setOrderedAt(now);
                    }
                }
                purchaseItemRepository.saveAll(items);
            }
            case "DRAFT" -> {
                if ("ORDERED".equals(current)) {
                    throw new BusinessException(
                            "Siparis verilmis grup once APPROVED'a geri alinmali.",
                            "PURCHASE_ORDER_LOCKED");
                }
                // Onayi geri al: onay alanlari temizlenir, teklifler kalir
                order.setSelectedQuoteId(null);
                order.setApprovalNote(null);
                order.setApprovedBy(null);
                order.setApprovedAt(null);
                order.setStatus("DRAFT");
            }
            case "CANCELLED" -> {
                if ("ORDERED".equals(current)) {
                    throw new BusinessException(
                            "Siparis verilmis grup iptal edilemez; once siparisi geri alin.",
                            "PURCHASE_ORDER_LOCKED");
                }
                releaseItems(order.getId());
                order.setSelectedQuoteId(null);
                order.setStatus("CANCELLED");
            }
            default -> throw new BusinessException(
                    "Gecersiz durum: " + target, "PURCHASE_ORDER_INVALID_STATUS");
        }
    }

    @Transactional
    public void delete(UUID id) {
        PurchaseOrder order = findEntityById(id);
        if ("APPROVED".equals(order.getStatus()) || "ORDERED".equals(order.getStatus())) {
            throw new BusinessException(
                    "Onaylanmis/siparis verilmis grup silinemez; once geri alin veya iptal edin.",
                    "PURCHASE_ORDER_LOCKED");
        }
        releaseItems(id);
        purchaseOrderRepository.delete(order);
        log.info("PurchaseOrder deleted: id={}, name={}", id, order.getName());
    }

    /** Uye kalemlerin grup bagini kopar (kalemler PLANNED olarak kalir). */
    private void releaseItems(UUID orderId) {
        List<PurchaseItem> items = purchaseItemRepository.findByPurchaseOrderId(orderId);
        for (PurchaseItem item : items) {
            item.setPurchaseOrderId(null);
        }
        purchaseItemRepository.saveAll(items);
    }

    /** ORDERED uye kalemleri PLANNED'a dondur (damgalar korunur). */
    private void revertOrderedItems(UUID orderId) {
        List<PurchaseItem> items = purchaseItemRepository.findByPurchaseOrderId(orderId);
        for (PurchaseItem item : items) {
            if ("ORDERED".equals(item.getStatus())) {
                item.setStatus("PLANNED");
            }
        }
        purchaseItemRepository.saveAll(items);
    }

    private PurchaseOrder findEntityById(UUID id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
    }
}
