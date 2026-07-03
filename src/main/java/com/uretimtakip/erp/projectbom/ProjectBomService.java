package com.uretimtakip.erp.projectbom;

import com.uretimtakip.erp.bom.BomPart;
import com.uretimtakip.erp.bom.BomPartRepository;
import com.uretimtakip.erp.bom.BomProductRepository;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.projectbom.dto.ProjectBomRequest;
import com.uretimtakip.erp.projectbom.dto.ProjectBomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ProjectBom is mantigi.
 *
 * AKILLI OZELLIKLER:
 *
 * CREATE:
 *   1. BomProduct var mi? (FK kontrolu)
 *   2. UNIQUE (projectName, bomProductId) kontrolu - ayni cifte iki kez izin yok
 *   3. ProjectBom kaydi olusur
 *   4. AUTO-POPULATE: BomProduct'un tum BomPart'lari ProjectBomPart olarak
 *      kopyalanir:
 *      - bomPartId referansi tutulur
 *      - Tum custom_X alanlari null kalir (kullanici sonra ozellestirir)
 *      - level/parent_custom_id hierarchy'si korunur (eski parent_id -> yeni
 *        parent_custom_id mapping yapilir)
 *      - operations jsonb kopyalanir
 *
 * UPDATE:
 *   bomProductId IMMUTABLE (request'te gelirse IGNORE). Sadece projectName,
 *   status, createdBy degisir.
 *
 * DELETE:
 *   ProjectBom silinince DB CASCADE ile ProjectBomPart'lar da silinir.
 *   Defensive yapmaya gerek yok - kullanici "bu proje BOM atamasini iptal
 *   ediyorum" diyorsa zaten alttaki tum custom calismayi da iptal etmis olur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectBomService {

    private final ProjectBomRepository projectBomRepository;
    private final ProjectBomPartRepository projectBomPartRepository;
    private final BomProductRepository bomProductRepository;
    private final BomPartRepository bomPartRepository;

    @Transactional(readOnly = true)
    public List<ProjectBomResponse> listAll() {
        return projectBomRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ProjectBomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectBomResponse> listByProject(String projectName) {
        return projectBomRepository.findByProjectNameOrderByCreatedAtDesc(projectName)
                .stream()
                .map(ProjectBomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectBomResponse> listByBomProduct(UUID bomProductId) {
        if (!bomProductRepository.existsById(bomProductId)) {
            throw new ResourceNotFoundException("BomProduct", "id", bomProductId);
        }
        return projectBomRepository.findByBomProductId(bomProductId)
                .stream()
                .map(ProjectBomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectBomResponse getById(UUID id) {
        ProjectBom pb = projectBomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProjectBom", "id", id));
        return ProjectBomResponse.fromEntity(pb);
    }

    /**
     * Create + AUTO-POPULATE
     *
     * Atomik islem (@Transactional):
     *   1. ProjectBom save
     *   2. BomProduct'un tum BomPart'larini cek
     *   3. Her biri icin ProjectBomPart olustur (bomPartId referansli)
     *   4. parent_id -> parent_custom_id mapping yap
     *
     * Hata olursa hepsi geri alinir.
     */
    @Transactional
    public ProjectBomResponse create(ProjectBomRequest request) {
        // 1. BomProduct var mi?
        if (!bomProductRepository.existsById(request.getBomProductId())) {
            throw new ResourceNotFoundException(
                    "BomProduct", "id", request.getBomProductId());
        }

        // 2. UNIQUE kontrol
        if (projectBomRepository.existsByProjectNameAndBomProductId(
                request.getProjectName(), request.getBomProductId())) {
            throw new BusinessException(
                    "Bu urun zaten bu projeye atanmis: project='"
                            + request.getProjectName() + "', bomProductId="
                            + request.getBomProductId(),
                    "DUPLICATE_PROJECT_BOM"
            );
        }

        // 3. ProjectBom olustur
        ProjectBom projectBom = ProjectBom.builder()
                .projectName(request.getProjectName())
                .bomProductId(request.getBomProductId())
                .status(request.getStatus() != null && !request.getStatus().isBlank()
                        ? request.getStatus() : "draft")
                .createdBy(request.getCreatedBy())
                .build();
        ProjectBom savedProjectBom = projectBomRepository.save(projectBom);

        // 4. AUTO-POPULATE: BomPart'lari kopyala
        int copiedCount = autoPopulateBomParts(
                savedProjectBom.getId(), request.getBomProductId());

        log.info("ProjectBom created: id={}, project='{}', bomProductId={}, "
                        + "auto-populated {} parts",
                savedProjectBom.getId(), savedProjectBom.getProjectName(),
                savedProjectBom.getBomProductId(), copiedCount);

        return ProjectBomResponse.fromEntityWithPartCount(savedProjectBom, copiedCount);
    }

    /**
     * UPDATE - bomProductId IMMUTABLE.
     * Sadece projectName, status, createdBy degisir.
     */
    @Transactional
    public ProjectBomResponse update(UUID id, ProjectBomRequest request) {
        ProjectBom pb = projectBomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProjectBom", "id", id));

        // Eger projectName degisiyorsa UNIQUE kontrolu lazim
        if (!pb.getProjectName().equals(request.getProjectName())) {
            if (projectBomRepository.existsByProjectNameAndBomProductId(
                    request.getProjectName(), pb.getBomProductId())) {
                throw new BusinessException(
                        "Bu urun zaten bu proje adina atanmis: '"
                                + request.getProjectName() + "'",
                        "DUPLICATE_PROJECT_BOM"
                );
            }
            pb.setProjectName(request.getProjectName());
        }

        // bomProductId IMMUTABLE - request'te gelse de IGNORE
        // status, createdBy degisebilir
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            pb.setStatus(request.getStatus());
        }
        pb.setCreatedBy(request.getCreatedBy());

        ProjectBom saved = projectBomRepository.save(pb);
        log.info("ProjectBom updated: id={}, project='{}'",
                saved.getId(), saved.getProjectName());
        return ProjectBomResponse.fromEntity(saved);
    }

    /**
     * DELETE - DB CASCADE ile ProjectBomPart'lar da silinir.
     */
    @Transactional
    public void delete(UUID id) {
        ProjectBom pb = projectBomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProjectBom", "id", id));
        projectBomRepository.delete(pb);
        log.info("ProjectBom deleted: id={}, project='{}'",
                id, pb.getProjectName());
    }

    // ============ AUTO-POPULATE HELPER ============

    /**
     * BomProduct'un tum BomPart'larini ProjectBomPart olarak kopyalar.
     *
     * Hierarchy korunmasi icin 2 gecisli (two-pass) algoritma:
     *   1. Pass: Tum BomPart'lar icin ProjectBomPart olustur,
     *      bomPartId -> yeni projectBomPart.id mapping kaydet
     *   2. Pass: parent_id'si olan kayitlar icin parent_custom_id'yi
     *      mapping'ten bul ve update et
     *
     * Return: olusturulan ProjectBomPart sayisi
     */
    private int autoPopulateBomParts(UUID projectBomId, UUID bomProductId) {
        List<BomPart> bomParts = bomPartRepository
                .findByProductIdOrderByLevelAscSortOrderAscNameAsc(bomProductId);

        if (bomParts.isEmpty()) {
            return 0;
        }

        // 1. Pass: ProjectBomPart'lar olustur (parent_custom_id null)
        Map<UUID, UUID> bomPartIdToProjectBomPartId = new HashMap<>();
        List<ProjectBomPart> created = new ArrayList<>();

        for (BomPart bp : bomParts) {
            ProjectBomPart pbp = ProjectBomPart.builder()
                    .projectBomId(projectBomId)
                    .bomPartId(bp.getId())
                    .isExcluded(false)
                    .customQty(null)        // baslangic null - sablona uy
                    .customName(null)
                    .customCode(null)
                    .customUnit(null)
                    .customWeight(null)
                    .customMaterial(null)
                    .deptId(null)
                    .parentCustomId(null)   // 2. pass'te doldurulacak
                    .operations(bp.getOperations() != null
                            ? new ArrayList<>(bp.getOperations())
                            : new ArrayList<>())
                    .level(bp.getLevel())
                    .sortOrder(bp.getSortOrder())
                    .build();
            ProjectBomPart saved = projectBomPartRepository.save(pbp);
            bomPartIdToProjectBomPartId.put(bp.getId(), saved.getId());
            created.add(saved);
        }

        // 2. Pass: parent_custom_id mapping yap (parent_id'si olanlar icin)
        for (int i = 0; i < bomParts.size(); i++) {
            BomPart bp = bomParts.get(i);
            if (bp.getParentId() != null) {
                UUID newParentCustomId = bomPartIdToProjectBomPartId.get(
                        bp.getParentId());
                if (newParentCustomId != null) {
                    ProjectBomPart pbp = created.get(i);
                    pbp.setParentCustomId(newParentCustomId);
                    projectBomPartRepository.save(pbp);
                }
            }
        }

        return created.size();
    }
}