package com.uretimtakip.erp.projectbom;

import com.uretimtakip.erp.bom.BomPart;
import com.uretimtakip.erp.bom.BomPartRepository;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.department.DepartmentRepository;
import com.uretimtakip.erp.projectbom.dto.ProjectBomPartRequest;
import com.uretimtakip.erp.projectbom.dto.ProjectBomPartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ProjectBomPart is mantigi.
 *
 * AKILLI OZELLIKLER:
 *
 * CREATE:
 *   1. projectBom var mi?
 *   2. bomPartId verilmisse:
 *      - BomPart var mi?
 *      - BomPart, projectBom'un bomProductId'sine bagli mi? (tutarlilik)
 *   3. bomPartId yoksa (custom-only mode):
 *      - customName ZORUNLU (coalesce edecek sablon yok)
 *      - customCode ZORUNLU
 *   4. parentCustomId verilmisse:
 *      - Parent var mi?
 *      - Parent ayni projectBom'a mi bagli? (kritik tutarlilik)
 *      - level = parent.level + 1 (auto)
 *   5. deptId verilmisse Department var mi?
 *
 * UPDATE:
 *   IMMUTABLE alanlar: projectBomId, bomPartId, parentCustomId, level.
 *   Sadece custom_X, deptId, isExcluded, operations, sortOrder degisir.
 *
 * DELETE:
 *   Defensive - child varsa BusinessException (Soru 4'te se\u00e7tigimiz B).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectBomPartService {

    private final ProjectBomPartRepository projectBomPartRepository;
    private final ProjectBomRepository projectBomRepository;
    private final BomPartRepository bomPartRepository;
    private final DepartmentRepository departmentRepository;

    // ============ LIST / GET ============

    @Transactional(readOnly = true)
    public List<ProjectBomPartResponse> listByProjectBom(UUID projectBomId) {
        if (!projectBomRepository.existsById(projectBomId)) {
            throw new ResourceNotFoundException("ProjectBom", "id", projectBomId);
        }
        List<ProjectBomPart> parts = projectBomPartRepository
                .findByProjectBomIdOrderByLevelAscSortOrderAscIdAsc(projectBomId);
        return resolveAll(parts);
    }

    @Transactional(readOnly = true)
    public List<ProjectBomPartResponse> listByParent(UUID parentCustomId) {
        List<ProjectBomPart> parts = projectBomPartRepository
                .findByParentCustomIdOrderBySortOrderAscIdAsc(parentCustomId);
        return resolveAll(parts);
    }

    @Transactional(readOnly = true)
    public ProjectBomPartResponse getById(UUID id) {
        ProjectBomPart part = findEntityById(id);
        BomPart bomPart = part.getBomPartId() != null
                ? bomPartRepository.findById(part.getBomPartId()).orElse(null)
                : null;
        return ProjectBomPartResponse.fromEntity(part, bomPart);
    }

    // ============ CREATE ============

    @Transactional
    public ProjectBomPartResponse create(ProjectBomPartRequest request) {
        // 1. ProjectBom var mi?
        ProjectBom projectBom = projectBomRepository.findById(request.getProjectBomId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProjectBom", "id", request.getProjectBomId()));

        // 2. bomPartId kontrolu (verilmisse)
        BomPart bomPart = null;
        if (request.getBomPartId() != null) {
            bomPart = bomPartRepository.findById(request.getBomPartId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "BomPart", "id", request.getBomPartId()));

            // BomPart, ProjectBom'un bagli oldugu BomProduct'a ait olmali
            if (!bomPart.getProductId().equals(projectBom.getBomProductId())) {
                throw new BusinessException(
                        "BomPart farkli bir BomProduct'a ait. "
                                + "BomPart.productId=" + bomPart.getProductId()
                                + ", ProjectBom.bomProductId="
                                + projectBom.getBomProductId(),
                        "BOMPART_PRODUCT_MISMATCH"
                );
            }
        } else {
            // 3. Custom-only mode: customName ve customCode zorunlu
            if (request.getCustomName() == null || request.getCustomName().isBlank()) {
                throw new BusinessException(
                        "bomPartId verilmediginde customName zorunlu",
                        "CUSTOM_NAME_REQUIRED"
                );
            }
            if (request.getCustomCode() == null || request.getCustomCode().isBlank()) {
                throw new BusinessException(
                        "bomPartId verilmediginde customCode zorunlu",
                        "CUSTOM_CODE_REQUIRED"
                );
            }
        }

        // 4. Parent kontrolu + level hesabi
        int level = 0;
        if (request.getParentCustomId() != null) {
            ProjectBomPart parent = projectBomPartRepository
                    .findById(request.getParentCustomId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ProjectBomPart (parent)", "id",
                            request.getParentCustomId()));

            if (!parent.getProjectBomId().equals(request.getProjectBomId())) {
                throw new BusinessException(
                        "Parent farkli bir ProjectBom'a ait. "
                                + "Parent.projectBomId=" + parent.getProjectBomId()
                                + ", verilen projectBomId=" + request.getProjectBomId(),
                        "PARENT_PROJECT_MISMATCH"
                );
            }
            level = (parent.getLevel() != null ? parent.getLevel() : 0) + 1;
        }

        // 5. Department kontrolu
        if (request.getDeptId() != null
                && !departmentRepository.existsById(request.getDeptId())) {
            throw new ResourceNotFoundException(
                    "Department", "id", request.getDeptId());
        }

        ProjectBomPart pbp = ProjectBomPart.builder()
                .projectBomId(request.getProjectBomId())
                .bomPartId(request.getBomPartId())
                .isExcluded(request.getIsExcluded() != null
                        ? request.getIsExcluded() : false)
                .customName(request.getCustomName())
                .customCode(request.getCustomCode())
                .customQty(request.getCustomQty())
                .customUnit(request.getCustomUnit())
                .customWeight(request.getCustomWeight())
                .customMaterial(request.getCustomMaterial())
                .deptId(request.getDeptId())
                .parentCustomId(request.getParentCustomId())
                .operations(request.getOperations() != null
                        ? request.getOperations() : new ArrayList<>())
                .level(level)
                .sortOrder(request.getSortOrder() != null
                        ? request.getSortOrder() : 0)
                .build();

        ProjectBomPart saved = projectBomPartRepository.save(pbp);
        log.info("ProjectBomPart created: id={}, projectBomId={}, bomPartId={}, "
                        + "parentCustomId={}, level={}",
                saved.getId(), saved.getProjectBomId(), saved.getBomPartId(),
                saved.getParentCustomId(), saved.getLevel());

        return ProjectBomPartResponse.fromEntity(saved, bomPart);
    }

    // ============ UPDATE ============

    /**
     * Update - sadece degisebilir alanlar.
     * IMMUTABLE: projectBomId, bomPartId, parentCustomId, level.
     */
    @Transactional
    public ProjectBomPartResponse update(UUID id, ProjectBomPartRequest request) {
        ProjectBomPart pbp = findEntityById(id);

        // Department degisiyorsa varlik kontrolu
        if (request.getDeptId() != null
                && !request.getDeptId().equals(pbp.getDeptId())
                && !departmentRepository.existsById(request.getDeptId())) {
            throw new ResourceNotFoundException(
                    "Department", "id", request.getDeptId());
        }

        // IMMUTABLE: projectBomId, bomPartId, parentCustomId, level DEGISMIYOR.
        if (request.getIsExcluded() != null) {
            pbp.setIsExcluded(request.getIsExcluded());
        }
        pbp.setCustomName(request.getCustomName());
        pbp.setCustomCode(request.getCustomCode());
        pbp.setCustomQty(request.getCustomQty());
        pbp.setCustomUnit(request.getCustomUnit());
        pbp.setCustomWeight(request.getCustomWeight());
        pbp.setCustomMaterial(request.getCustomMaterial());
        pbp.setDeptId(request.getDeptId());
        if (request.getOperations() != null) {
            pbp.setOperations(request.getOperations());
        }
        if (request.getSortOrder() != null) {
            pbp.setSortOrder(request.getSortOrder());
        }

        ProjectBomPart saved = projectBomPartRepository.save(pbp);
        log.info("ProjectBomPart updated: id={}", saved.getId());

        BomPart bomPart = saved.getBomPartId() != null
                ? bomPartRepository.findById(saved.getBomPartId()).orElse(null)
                : null;
        return ProjectBomPartResponse.fromEntity(saved, bomPart);
    }

    // ============ DELETE (defensive - Soru 4 secimi B) ============

    @Transactional
    public void delete(UUID id) {
        ProjectBomPart pbp = findEntityById(id);

        long childCount = projectBomPartRepository.countByParentCustomId(id);
        if (childCount > 0) {
            throw new BusinessException(
                    "Bu parcanin " + childCount + " alt parcasi var, silinemez. "
                            + "Once alt parcalari silmelisin.",
                    "PROJECT_BOM_PART_HAS_CHILDREN"
            );
        }

        projectBomPartRepository.delete(pbp);
        log.info("ProjectBomPart deleted: id={}", id);
    }

    // ============ HELPERS ============

    private ProjectBomPart findEntityById(UUID id) {
        return projectBomPartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProjectBomPart", "id", id));
    }

    /**
     * Bir liste icin resolve - BomPart referanslari toplu cekilir (N+1 onleme).
     */
    private List<ProjectBomPartResponse> resolveAll(List<ProjectBomPart> parts) {
        if (parts.isEmpty()) {
            return List.of();
        }

        // Tum bomPartId'leri tek seferde cek
        List<UUID> bomPartIds = parts.stream()
                .map(ProjectBomPart::getBomPartId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, BomPart> bomPartMap = new HashMap<>();
        if (!bomPartIds.isEmpty()) {
            for (UUID bpId : bomPartIds) {
                Optional<BomPart> bp = bomPartRepository.findById(bpId);
                bp.ifPresent(b -> bomPartMap.put(bpId, b));
            }
        }

        return parts.stream()
                .map(p -> ProjectBomPartResponse.fromEntity(
                        p, p.getBomPartId() != null
                                ? bomPartMap.get(p.getBomPartId()) : null))
                .collect(Collectors.toList());
    }
}