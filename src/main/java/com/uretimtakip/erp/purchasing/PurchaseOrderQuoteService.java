package com.uretimtakip.erp.purchasing;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderQuoteRequest;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderQuoteResponse;
import com.uretimtakip.erp.purchasing.dto.PurchaseOrderQuoteUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PurchaseOrderQuote (firma teklifi) is mantigi.
 *
 * - Teklif sadece DRAFT gruba eklenir (onaylanmis gruba yeni teklif girilmez).
 * - Grubun SECILI teklifi silinemez (once onay geri alinmali).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderQuoteService {

    private final PurchaseOrderQuoteRepository quoteRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(readOnly = true)
    public List<PurchaseOrderQuoteResponse> listAll() {
        return quoteRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(PurchaseOrderQuoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderQuoteResponse> listByOrder(UUID orderId) {
        return quoteRepository.findByPurchaseOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(PurchaseOrderQuoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PurchaseOrderQuoteResponse create(PurchaseOrderQuoteRequest request) {
        PurchaseOrder order = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PurchaseOrder", "id", request.getPurchaseOrderId()));
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(
                    "Teklif sadece taslak (DRAFT) gruba eklenebilir.",
                    "QUOTE_ORDER_NOT_DRAFT");
        }

        PurchaseOrderQuote quote = PurchaseOrderQuote.builder()
                .purchaseOrderId(request.getPurchaseOrderId())
                .supplierName(request.getSupplierName().trim())
                .contactInfo(request.getContactInfo())
                .totalPrice(request.getTotalPrice())
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank()
                        ? request.getCurrency() : "TRY")
                .deliveryDate(request.getDeliveryDate())
                .notes(request.getNotes())
                .build();

        PurchaseOrderQuote saved = quoteRepository.save(quote);
        log.info("PurchaseOrderQuote created: id={}, order={}, supplier={}",
                saved.getId(), saved.getPurchaseOrderId(), saved.getSupplierName());
        return PurchaseOrderQuoteResponse.fromEntity(saved);
    }

    @Transactional
    public PurchaseOrderQuoteResponse update(UUID id, PurchaseOrderQuoteUpdateRequest request) {
        PurchaseOrderQuote quote = findEntityById(id);

        // PARTIAL update: sadece gonderilen alanlar islenir.
        if (request.getSupplierName() != null && !request.getSupplierName().isBlank()) {
            quote.setSupplierName(request.getSupplierName().trim());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            quote.setCurrency(request.getCurrency());
        }
        // Presence takipli alanlar: explicit null = temizle
        if (request.isContactInfoPresent()) {
            quote.setContactInfo(request.getContactInfo());
        }
        if (request.isTotalPricePresent()) {
            quote.setTotalPrice(request.getTotalPrice());
        }
        if (request.isDeliveryDatePresent()) {
            quote.setDeliveryDate(request.getDeliveryDate());
        }
        if (request.isNotesPresent()) {
            quote.setNotes(request.getNotes());
        }
        if (request.isRejectionReasonPresent()) {
            quote.setRejectionReason(request.getRejectionReason());
        }

        PurchaseOrderQuote saved = quoteRepository.save(quote);
        log.info("PurchaseOrderQuote updated: id={}, supplier={}", saved.getId(), saved.getSupplierName());
        return PurchaseOrderQuoteResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PurchaseOrderQuote quote = findEntityById(id);
        PurchaseOrder order = purchaseOrderRepository.findById(quote.getPurchaseOrderId()).orElse(null);
        if (order != null && id.equals(order.getSelectedQuoteId())) {
            throw new BusinessException(
                    "Kazanan olarak secilmis teklif silinemez; once onayi geri alin.",
                    "QUOTE_SELECTED_UNDELETABLE");
        }
        quoteRepository.delete(quote);
        log.info("PurchaseOrderQuote deleted: id={}, supplier={}", id, quote.getSupplierName());
    }

    private PurchaseOrderQuote findEntityById(UUID id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderQuote", "id", id));
    }
}
