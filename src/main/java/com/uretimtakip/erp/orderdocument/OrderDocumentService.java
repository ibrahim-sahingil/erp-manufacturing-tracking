package com.uretimtakip.erp.orderdocument;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.order.OrderRepository;
import com.uretimtakip.erp.orderdocument.dto.OrderDocumentMetaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * (12. tur m1) Teklif/siparis dosya ekleri — BomDocumentService deseni.
 * Icerik update edilemez; yeni surum = yeni yukleme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDocumentService {

    /** UCLU KURAL: DB CHECK (order_documents.category) ile ayni liste. */
    private static final Set<String> CATEGORIES = Set.of("QUOTE", "ORDER");
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private final OrderDocumentRepository orderDocumentRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<OrderDocumentMetaResponse> listByOrder(UUID orderId) {
        return orderDocumentRepository.findMetaByOrder(orderId);
    }

    @Transactional
    public OrderDocumentMetaResponse upload(MultipartFile file, UUID orderId,
                                            String category, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Dosya bos veya gonderilmedi", "ODOC_FILE_EMPTY");
        }
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }
        String cat = category == null || category.isBlank()
                ? "QUOTE" : category.toUpperCase();
        if (!CATEGORIES.contains(cat)) {
            throw new BusinessException("Gecersiz kategori (QUOTE/ORDER): " + category,
                    "ODOC_BAD_CATEGORY");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Dosya okunamadi: " + e.getMessage(), "ODOC_READ_ERROR");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BusinessException("Dosya 50MB siniri asiyor", "ODOC_TOO_LARGE");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) filename = "dosya";
        filename = filename.substring(Math.max(
                filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        if (filename.length() > 300) filename = filename.substring(filename.length() - 300);

        OrderDocument saved = orderDocumentRepository.save(OrderDocument.builder()
                .orderId(orderId)
                .category(cat)
                .filename(filename)
                .contentType(file.getContentType())
                .sizeBytes((long) bytes.length)
                .data(bytes)
                .uploadedBy(uploadedBy != null && !uploadedBy.isBlank() ? uploadedBy : null)
                .build());
        log.info("OrderDocument uploaded: id={}, order={}, category={}, file={}, size={}",
                saved.getId(), orderId, cat, filename, bytes.length);
        return new OrderDocumentMetaResponse(saved.getId(), saved.getOrderId(),
                saved.getCategory(), saved.getFilename(), saved.getContentType(),
                saved.getSizeBytes(), saved.getUploadedBy(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public OrderDocument getForDownload(UUID id) {
        return orderDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderDocument", "id", id));
    }

    @Transactional
    public void delete(UUID id) {
        OrderDocument doc = orderDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderDocument", "id", id));
        orderDocumentRepository.delete(doc);
        log.info("OrderDocument deleted: id={}, order={}", id, doc.getOrderId());
    }
}
