package com.uretimtakip.erp.bomdocument;

import com.uretimtakip.erp.bom.BomPart;
import com.uretimtakip.erp.bom.BomPartRepository;
import com.uretimtakip.erp.bom.BomProductRepository;
import com.uretimtakip.erp.bomdocument.dto.BomDocumentMetaResponse;
import com.uretimtakip.erp.bomdocument.dto.BomDocumentUpdateRequest;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * BomDocument is mantigi (teknik resimler — 5. tur #7).
 *
 * Kurallar:
 * - category URETIM/ARGE olmali (DB CHECK + burada dogrulama).
 * - Baglanan parcalar (partIds) AYNI urune ait olmali — degilse
 *   DOC_PART_PRODUCT_MISMATCH.
 * - Dosya icerigi update'te degistirilemez; yeni surum = yeni yukleme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomDocumentService {

    private final BomDocumentRepository bomDocumentRepository;
    private final BomProductRepository bomProductRepository;
    private final BomPartRepository bomPartRepository;

    @Transactional(readOnly = true)
    public List<BomDocumentMetaResponse> listByProduct(UUID productId) {
        List<BomDocumentMetaResponse> metas =
                bomDocumentRepository.findMetaByProduct(productId);
        if (metas.isEmpty()) {
            return metas;
        }
        // Parca baglari tek sorguda cekilip meta'lara dagitilir
        Map<UUID, BomDocumentMetaResponse> byId = new HashMap<>();
        metas.forEach(m -> byId.put(m.getId(), m));
        for (Object[] link : bomDocumentRepository.findLinksByProduct(productId)) {
            BomDocumentMetaResponse m = byId.get((UUID) link[0]);
            if (m != null) {
                m.getPartIds().add((UUID) link[1]);
            }
        }
        return metas;
    }

    @Transactional
    public BomDocumentMetaResponse upload(MultipartFile file, UUID productId,
                                          String category, List<UUID> partIds,
                                          String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Dosya bos veya gonderilmedi", "DOC_FILE_EMPTY");
        }
        if (!bomProductRepository.existsById(productId)) {
            throw new ResourceNotFoundException("BomProduct", "id", productId);
        }
        validateCategory(category);
        Set<UUID> links = validatePartIds(productId, partIds);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Dosya okunamadi: " + e.getMessage(), "DOC_READ_ERROR");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "dosya";
        }
        // Yol bilesenleri temizlenir (tarayici tam yol gonderebilir)
        filename = filename.substring(Math.max(
                filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        if (filename.length() > 255) {
            filename = filename.substring(filename.length() - 255);
        }

        BomDocument doc = BomDocument.builder()
                .productId(productId)
                .category(category)
                .filename(filename)
                .contentType(file.getContentType())
                .sizeBytes((long) bytes.length)
                .data(bytes)
                .uploadedBy(uploadedBy != null && !uploadedBy.isBlank() ? uploadedBy : null)
                .partIds(links)
                .build();

        BomDocument saved = bomDocumentRepository.save(doc);
        log.info("BomDocument uploaded: id={}, product={}, category={}, file={}, size={}",
                saved.getId(), productId, category, filename, bytes.length);
        return toMeta(saved);
    }

    @Transactional
    public BomDocumentMetaResponse update(UUID id, BomDocumentUpdateRequest request) {
        BomDocument doc = findEntityById(id);
        if (request.getFilename() != null && !request.getFilename().isBlank()) {
            doc.setFilename(request.getFilename().trim());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            validateCategory(request.getCategory());
            doc.setCategory(request.getCategory());
        }
        if (request.getPartIds() != null) {
            doc.setPartIds(validatePartIds(doc.getProductId(), request.getPartIds()));
        }
        BomDocument saved = bomDocumentRepository.save(doc);
        log.info("BomDocument updated: id={}, category={}, links={}",
                saved.getId(), saved.getCategory(), saved.getPartIds().size());
        return toMeta(saved);
    }

    @Transactional
    public void delete(UUID id) {
        BomDocument doc = findEntityById(id);
        bomDocumentRepository.delete(doc);
        log.info("BomDocument deleted: id={}, file={}", id, doc.getFilename());
    }

    /** Indirme — data BURADA yuklenir (tek kayit). */
    @Transactional(readOnly = true)
    public BomDocument getForDownload(UUID id) {
        return findEntityById(id);
    }

    private void validateCategory(String category) {
        if (!"URETIM".equals(category) && !"ARGE".equals(category)) {
            throw new BusinessException(
                    "Kategori URETIM veya ARGE olmali: " + category, "DOC_BAD_CATEGORY");
        }
    }

    /** Baglanan parcalarin ayni urune ait oldugunu dogrular. */
    private Set<UUID> validatePartIds(UUID productId, List<UUID> partIds) {
        Set<UUID> links = new HashSet<>();
        if (partIds == null) {
            return links;
        }
        for (UUID pid : partIds) {
            BomPart part = bomPartRepository.findById(pid).orElseThrow(
                    () -> new ResourceNotFoundException("BomPart", "id", pid));
            if (!part.getProductId().equals(productId)) {
                throw new BusinessException(
                        "Parca baska bir urune ait: " + part.getCode(),
                        "DOC_PART_PRODUCT_MISMATCH");
            }
            links.add(pid);
        }
        return links;
    }

    private BomDocumentMetaResponse toMeta(BomDocument d) {
        BomDocumentMetaResponse m = new BomDocumentMetaResponse(
                d.getId(), d.getProductId(), d.getCategory(), d.getFilename(),
                d.getContentType(), d.getSizeBytes(), d.getUploadedBy(), d.getCreatedAt());
        m.getPartIds().addAll(d.getPartIds());
        return m;
    }

    private BomDocument findEntityById(UUID id) {
        return bomDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomDocument", "id", id));
    }
}
