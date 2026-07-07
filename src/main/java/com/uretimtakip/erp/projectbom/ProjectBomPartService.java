package com.uretimtakip.erp.projectbom;

import com.uretimtakip.erp.bom.BomPart;
import com.uretimtakip.erp.bom.BomPartRepository;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.department.DepartmentRepository;
import com.uretimtakip.erp.projectbom.dto.ProjectBomPartRequest;
import com.uretimtakip.erp.projectbom.dto.ProjectBomPartResponse;
import com.uretimtakip.erp.projectbom.dto.ProjectBomPartUpdateRequest;
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
 *   PARTIAL calisir: sadece gonderilen alanlar degisir.
 *   parentCustomId/level degistirilebilir (hiyerarsi tasima),
 *   deptId atanabilir/kaldirilabilir (explicit null = kaldir).
 *   IMMUTABLE alanlar: projectBomId, bomPartId.
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
    public List<ProjectBomPartResponse> listAll() {
        // Frontend planlama ekrani tum listeyi ceker, filtreyi client-side uygular
        return resolveAll(projectBomPartRepository.findAll());
    }

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

        // Ayni parent altinda ayni kod (harf duyarsiz) iki kez olamaz.
        // Farkli dallarda ayni kod SERBEST (4. tur #1: aynalama parcalar).
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()
                && projectBomPartRepository.existsSiblingCode(
                        request.getProjectBomId(), request.getParentCustomId(),
                        request.getCustomCode())) {
            throw new BusinessException(
                    "Ayni ust parca altinda bu kodda bir parca zaten var: "
                            + request.getCustomCode(),
                    "PBOM_PART_CODE_EXISTS"
            );
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
                .customWidthMm(request.getCustomWidthMm())
                .customHeightMm(request.getCustomHeightMm())
                .customThicknessMm(request.getCustomThicknessMm())
                .materialKind(request.getMaterialKind() != null && !request.getMaterialKind().isBlank()
                        ? request.getMaterialKind() : null)
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
     * PARTIAL update - sadece gonderilen alanlar degisir.
     * parentCustomId/level/deptId dahil. IMMUTABLE: projectBomId, bomPartId.
     */
    @Transactional
    public ProjectBomPartResponse update(UUID id, ProjectBomPartUpdateRequest request) {
        ProjectBomPart pbp = findEntityById(id);

        // PARTIAL update: sadece gonderilen (non-null) alanlar islenir.
        // IMMUTABLE: projectBomId, bomPartId.
        if (request.getIsExcluded() != null) {
            pbp.setIsExcluded(request.getIsExcluded());
        }
        if (request.getCustomName() != null && !request.getCustomName().isBlank()) {
            pbp.setCustomName(request.getCustomName());
        }
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            // Kontrol, tasima da varsa HEDEF parent'a gore yapilir (4. tur #1)
            UUID targetParent = request.isParentCustomIdPresent()
                    ? request.getParentCustomId() : pbp.getParentCustomId();
            if (!request.getCustomCode().equalsIgnoreCase(pbp.getCustomCode())
                    && projectBomPartRepository.existsSiblingCodeExcept(
                            pbp.getProjectBomId(), targetParent,
                            request.getCustomCode(), pbp.getId())) {
                throw new BusinessException(
                        "Ayni ust parca altinda bu kodda bir parca zaten var: "
                                + request.getCustomCode(),
                        "PBOM_PART_CODE_EXISTS"
                );
            }
            pbp.setCustomCode(request.getCustomCode());
        }
        if (request.getCustomQty() != null) {
            pbp.setCustomQty(request.getCustomQty());
        }
        if (request.getCustomUnit() != null && !request.getCustomUnit().isBlank()) {
            pbp.setCustomUnit(request.getCustomUnit());
        }
        if (request.getCustomWeight() != null) {
            pbp.setCustomWeight(request.getCustomWeight());
        }
        if (request.getCustomMaterial() != null) {
            pbp.setCustomMaterial(request.getCustomMaterial());
        }
        if (request.getCustomWidthMm() != null) {
            pbp.setCustomWidthMm(request.getCustomWidthMm());
        }
        if (request.getCustomHeightMm() != null) {
            pbp.setCustomHeightMm(request.getCustomHeightMm());
        }
        if (request.getCustomThicknessMm() != null) {
            pbp.setCustomThicknessMm(request.getCustomThicknessMm());
        }
        if (request.getMaterialKind() != null) {
            // Bos string = override'i temizle (sablon turu gecerli olur)
            pbp.setMaterialKind(request.getMaterialKind().isBlank() ? null : request.getMaterialKind());
        }
        if (request.getOperations() != null) {
            pbp.setOperations(request.getOperations());
        }
        if (request.getSortOrder() != null) {
            pbp.setSortOrder(request.getSortOrder());
        }

        // Departman atama/kaldirma: dept_id JSON'da acikca geldiyse islenir
        // (null = departmani kaldir). Gelmediyse dokunulmaz.
        if (request.isDeptIdPresent()) {
            if (request.getDeptId() != null
                    && !request.getDeptId().equals(pbp.getDeptId())
                    && !departmentRepository.existsById(request.getDeptId())) {
                throw new ResourceNotFoundException(
                        "Department", "id", request.getDeptId());
            }
            pbp.setDeptId(request.getDeptId());
        }

        // Hiyerarsi tasima: parent_custom_id acikca geldiyse islenir
        // (null = kok seviyeye cikar). Gelmediyse dokunulmaz.
        if (request.isParentCustomIdPresent()) {
            applyParentChange(pbp, request.getParentCustomId(), request.getLevel());
        } else if (request.getLevel() != null) {
            pbp.setLevel(request.getLevel());
        }

        ProjectBomPart saved = projectBomPartRepository.save(pbp);
        log.info("ProjectBomPart updated: id={}, parentCustomId={}, level={}",
                saved.getId(), saved.getParentCustomId(), saved.getLevel());

        BomPart bomPart = saved.getBomPartId() != null
                ? bomPartRepository.findById(saved.getBomPartId()).orElse(null)
                : null;
        return ProjectBomPartResponse.fromEntity(saved, bomPart);
    }

    /**
     * Parcayi yeni parent'in altina tasir (null -> kok seviye).
     * Ayni projectBom kontrolu + dongu (kendi alt agacina tasima) kontrolu yapar.
     */
    private void applyParentChange(ProjectBomPart pbp, UUID newParentId, Integer requestedLevel) {
        if (newParentId == null) {
            assertNoSiblingCodeConflict(pbp, null);
            pbp.setParentCustomId(null);
            pbp.setLevel(requestedLevel != null ? requestedLevel : 0);
            return;
        }
        if (newParentId.equals(pbp.getId())) {
            throw new BusinessException(
                    "Bir parca kendi kendisinin ustune tasinamaz.",
                    "PROJECT_BOM_PART_SELF_PARENT");
        }
        ProjectBomPart parent = projectBomPartRepository.findById(newParentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProjectBomPart (parent)", "id", newParentId));
        if (!parent.getProjectBomId().equals(pbp.getProjectBomId())) {
            throw new BusinessException(
                    "Parent parca farkli bir proje baglantisina ait.",
                    "PARENT_PROJECT_BOM_MISMATCH");
        }
        // Dongu kontrolu: yeni parent'in atalari arasinda bu parca varsa
        // tasima kendi alt agacinin icine yapiliyor demektir.
        UUID cursor = parent.getParentCustomId();
        int guard = 0;
        while (cursor != null && guard++ < 500) {
            if (cursor.equals(pbp.getId())) {
                throw new BusinessException(
                        "Bir parca kendi alt parcasinin altina tasinamaz.",
                        "PROJECT_BOM_PART_CYCLE");
            }
            cursor = projectBomPartRepository.findById(cursor)
                    .map(ProjectBomPart::getParentCustomId)
                    .orElse(null);
        }
        assertNoSiblingCodeConflict(pbp, newParentId);
        pbp.setParentCustomId(newParentId);
        pbp.setLevel(requestedLevel != null
                ? requestedLevel
                : (parent.getLevel() != null ? parent.getLevel() : 0) + 1);
    }

    /**
     * Hedef parent altinda ayni (ETKIN) kodda kardes varsa tasima reddedilir
     * (4. tur B3). Etkin kod = custom_code, bossa sablondaki bom_parts.code;
     * otomatik kopyalanan satirlarda custom_code bos oldugundan repo'daki
     * custom_code sorgusu yetmez, kardesler bellekte cozumlenerek bakilir.
     * (pbome suruklemesi de update -> applyParentChange yolundan gecer.)
     */
    private void assertNoSiblingCodeConflict(ProjectBomPart pbp, UUID newParentId) {
        String movingCode = effectiveCode(pbp);
        if (movingCode == null) {
            return;
        }
        List<ProjectBomPart> siblings = newParentId == null
                ? projectBomPartRepository
                        .findByProjectBomIdOrderByLevelAscSortOrderAscIdAsc(pbp.getProjectBomId())
                        .stream().filter(s -> s.getParentCustomId() == null).toList()
                : projectBomPartRepository.findByParentCustomIdOrderBySortOrderAscIdAsc(newParentId);
        for (ProjectBomPart s : siblings) {
            if (!s.getId().equals(pbp.getId())
                    && movingCode.equalsIgnoreCase(effectiveCode(s))) {
                throw new BusinessException(
                        "Hedef ust parca altinda bu kodda bir parca zaten var: " + movingCode,
                        "PBOM_PART_CODE_EXISTS");
            }
        }
    }

    /** custom_code override'i, bossa bagli sablon parcasinin kodu. */
    private String effectiveCode(ProjectBomPart p) {
        if (p.getCustomCode() != null && !p.getCustomCode().isBlank()) {
            return p.getCustomCode();
        }
        if (p.getBomPartId() != null) {
            return bomPartRepository.findById(p.getBomPartId())
                    .map(BomPart::getCode)
                    .orElse(null);
        }
        return null;
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