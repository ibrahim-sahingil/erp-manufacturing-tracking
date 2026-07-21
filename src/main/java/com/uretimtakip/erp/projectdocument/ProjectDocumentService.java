package com.uretimtakip.erp.projectdocument;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.order.OrderRepository;
import com.uretimtakip.erp.projectdocument.dto.ProjectDocumentMetaResponse;
import com.uretimtakip.erp.projectdocument.dto.ProjectDocumentUpdateRequest;
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
 * (16. tur M3.2) Proje teknik resimleri is mantigi — OrderDocumentService +
 * BomDocumentService desenlerinin birlesimi:
 * - 50MB byte guard'i (OrderDocumentService)
 * - parca baglari + ayni-proje dogrulamasi + DOC_LINKED silme kurali
 *   (BomDocumentService; baglar cozulmeden dosya silinmez — yanlislikla
 *   parcali dosya kaybolmasin, frontend once baglari kaldirir)
 * - icerik update edilemez; yeni surum = yeni yukleme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDocumentService {

    /** UCLU KURAL: DB CHECK (project_documents.category) ile ayni liste. */
    private static final Set<String> CATEGORIES = Set.of("SIPARIS", "IMALAT", "DIGER");
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private final ProjectDocumentRepository projectDocumentRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<ProjectDocumentMetaResponse> listByProject(String projectName) {
        List<ProjectDocumentMetaResponse> metas =
                projectDocumentRepository.findMetaByProject(projectName);
        if (metas.isEmpty()) return metas;
        // Parca baglari tek sorguda cekilip meta'lara dagitilir (bom deseni)
        Map<UUID, ProjectDocumentMetaResponse> byId = new HashMap<>();
        metas.forEach(m -> byId.put(m.getId(), m));
        for (Object[] link : projectDocumentRepository.findLinksByProject(projectName)) {
            ProjectDocumentMetaResponse m = byId.get((UUID) link[0]);
            if (m != null) m.getPartIds().add((UUID) link[1]);
        }
        return metas;
    }

    @Transactional
    public ProjectDocumentMetaResponse upload(MultipartFile file, String projectName,
                                              String category, List<UUID> partIds,
                                              String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Dosya bos veya gonderilmedi", "PDOC_FILE_EMPTY");
        }
        if (projectName == null || projectName.isBlank()
                || orderRepository.findByProjectName(projectName.trim()).isEmpty()) {
            throw new BusinessException(
                    "Proje bulunamadi: " + projectName, "PDOC_PROJECT_NOT_FOUND");
        }
        String proj = projectName.trim();
        String cat = category == null || category.isBlank() ? "DIGER" : category.toUpperCase();
        if (!CATEGORIES.contains(cat)) {
            throw new BusinessException(
                    "Gecersiz kategori (SIPARIS/IMALAT/DIGER): " + category, "PDOC_BAD_CATEGORY");
        }
        validateParts(proj, partIds);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Dosya okunamadi: " + e.getMessage(), "PDOC_READ_ERROR");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BusinessException("Dosya 50MB siniri asiyor", "PDOC_TOO_LARGE");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) filename = "dosya";
        filename = filename.substring(Math.max(
                filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        if (filename.length() > 300) filename = filename.substring(filename.length() - 300);

        ProjectDocument saved = projectDocumentRepository.save(ProjectDocument.builder()
                .projectName(proj)
                .category(cat)
                .filename(filename)
                .contentType(file.getContentType())
                .sizeBytes((long) bytes.length)
                .data(bytes)
                .uploadedBy(uploadedBy != null && !uploadedBy.isBlank() ? uploadedBy : null)
                .partIds(partIds != null ? new HashSet<>(partIds) : new HashSet<>())
                .build());
        log.info("ProjectDocument uploaded: id={}, project={}, category={}, file={}, size={}, parts={}",
                saved.getId(), proj, cat, filename, bytes.length,
                partIds != null ? partIds.size() : 0);
        return toMeta(saved);
    }

    @Transactional
    public ProjectDocumentMetaResponse update(UUID id, ProjectDocumentUpdateRequest request) {
        ProjectDocument doc = projectDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDocument", "id", id));
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            String cat = request.getCategory().toUpperCase();
            if (!CATEGORIES.contains(cat)) {
                throw new BusinessException(
                        "Gecersiz kategori (SIPARIS/IMALAT/DIGER): " + request.getCategory(),
                        "PDOC_BAD_CATEGORY");
            }
            doc.setCategory(cat);
        }
        if (request.getPartIds() != null) {
            validateParts(doc.getProjectName(), request.getPartIds());
            doc.setPartIds(new HashSet<>(request.getPartIds()));
        }
        ProjectDocument saved = projectDocumentRepository.save(doc);
        log.info("ProjectDocument updated: id={}, category={}, parts={}",
                id, saved.getCategory(), saved.getPartIds().size());
        return toMeta(saved);
    }

    @Transactional(readOnly = true)
    public ProjectDocument getForDownload(UUID id) {
        return projectDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDocument", "id", id));
    }

    @Transactional
    public void delete(UUID id) {
        ProjectDocument doc = projectDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDocument", "id", id));
        if (!doc.getPartIds().isEmpty()) {
            throw new BusinessException(
                    "Dosya parcalara bagli, once baglari kaldirin (Parcalar > temizle).",
                    "PDOC_LINKED");
        }
        projectDocumentRepository.delete(doc);
        log.info("ProjectDocument deleted: id={}, project={}", id, doc.getProjectName());
    }

    /** Baglanan pbp'ler AYNI projenin agaclarina ait olmali. */
    private void validateParts(String projectName, List<UUID> partIds) {
        if (partIds == null || partIds.isEmpty()) return;
        long ok = projectDocumentRepository.countPartsInProject(projectName, partIds);
        if (ok != new HashSet<>(partIds).size()) {
            throw new BusinessException(
                    "Baglanan parcalardan bazilari bu projenin agacina ait degil.",
                    "PDOC_PART_PROJECT_MISMATCH");
        }
    }

    private ProjectDocumentMetaResponse toMeta(ProjectDocument d) {
        ProjectDocumentMetaResponse m = new ProjectDocumentMetaResponse(
                d.getId(), d.getProjectName(), d.getCategory(), d.getFilename(),
                d.getContentType(), d.getSizeBytes(), d.getUploadedBy(), d.getCreatedAt());
        m.getPartIds().addAll(d.getPartIds());
        return m;
    }
}
