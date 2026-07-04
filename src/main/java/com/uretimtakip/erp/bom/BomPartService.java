package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomPartRequest;
import com.uretimtakip.erp.bom.dto.BomPartResponse;
import com.uretimtakip.erp.bom.dto.BomPartUpdateRequest;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.projectbom.ProjectBomPartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BomPart is mantigi.
 *
 * Update PARTIAL calisir: sadece gonderilen alanlar degisir.
 * parentId/level degistirilebilir (hiyerarsi tasima); productId IMMUTABLE.
 *
 * Delete:
 *   1. Child BomPart var mi? (existsByParentId)
 *   2. project_bom_parts.bom_part_id'de kullanim var mi? (FAZ 2 AKTIF)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomPartService {

    private final BomPartRepository bomPartRepository;
    private final BomProductRepository bomProductRepository;
    private final ProjectBomPartRepository projectBomPartRepository; // FAZ 2 EKLENDI

    @Transactional(readOnly = true)
    public List<BomPartResponse> listAll() {
        return bomPartRepository.findAllByOrderByLevelAscSortOrderAscNameAsc()
                .stream()
                .map(BomPartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BomPartResponse> listByProduct(UUID productId) {
        if (!bomProductRepository.existsById(productId)) {
            throw new ResourceNotFoundException("BomProduct", "id", productId);
        }
        return bomPartRepository.findByProductIdOrderByLevelAscSortOrderAscNameAsc(productId)
                .stream()
                .map(BomPartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BomPartResponse> listByParent(UUID parentId) {
        return bomPartRepository.findByParentIdOrderBySortOrderAscNameAsc(parentId)
                .stream()
                .map(BomPartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BomPartResponse getById(UUID id) {
        BomPart part = findEntityById(id);
        return BomPartResponse.fromEntity(part);
    }

    @Transactional
    public BomPartResponse create(BomPartRequest request) {
        if (!bomProductRepository.existsById(request.getProductId())) {
            throw new ResourceNotFoundException(
                    "BomProduct", "id", request.getProductId());
        }

        int level = 0;
        if (request.getParentId() != null) {
            BomPart parent = bomPartRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "BomPart (parent)", "id", request.getParentId()));

            if (!parent.getProductId().equals(request.getProductId())) {
                throw new BusinessException(
                        "Parent parca farkli bir urune ait. Parent productId="
                                + parent.getProductId() + ", verilen productId="
                                + request.getProductId(),
                        "PARENT_PRODUCT_MISMATCH"
                );
            }
            level = (parent.getLevel() != null ? parent.getLevel() : 0) + 1;
        }

        BomPart part = BomPart.builder()
                .productId(request.getProductId())
                .parentId(request.getParentId())
                .name(request.getName())
                .code(request.getCode())
                .quantity(request.getQuantity() != null
                        ? request.getQuantity() : BigDecimal.ONE)
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .weightKg(request.getWeightKg())
                .material(request.getMaterial())
                .operations(request.getOperations() != null
                        ? request.getOperations() : new ArrayList<>())
                .level(level)
                .sortOrder(request.getSortOrder() != null
                        ? request.getSortOrder() : 0)
                .build();

        BomPart saved = bomPartRepository.save(part);
        log.info("BomPart created: id={}, name={}, productId={}, parentId={}, level={}",
                saved.getId(), saved.getName(), saved.getProductId(),
                saved.getParentId(), saved.getLevel());
        return BomPartResponse.fromEntity(saved);
    }

    @Transactional
    public BomPartResponse update(UUID id, BomPartUpdateRequest request) {
        BomPart part = findEntityById(id);

        // PARTIAL update: sadece gonderilen (non-null) alanlar islenir.
        // Frontend hiyerarsi editoru {parent_id, level} gibi tekil alanlar yollar.
        if (request.getName() != null && !request.getName().isBlank()) {
            part.setName(request.getName());
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            part.setCode(request.getCode());
        }
        if (request.getQuantity() != null) {
            part.setQuantity(request.getQuantity());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            part.setUnit(request.getUnit());
        }
        if (request.getWeightKg() != null) {
            part.setWeightKg(request.getWeightKg());
        }
        if (request.getMaterial() != null) {
            part.setMaterial(request.getMaterial());
        }
        if (request.getOperations() != null) {
            part.setOperations(request.getOperations());
        }
        if (request.getSortOrder() != null) {
            part.setSortOrder(request.getSortOrder());
        }

        // Hiyerarsi tasima: parent_id JSON'da acikca geldiyse islenir
        // (null = kok seviyeye cikar). Gelmediyse dokunulmaz.
        if (request.isParentIdPresent()) {
            applyParentChange(part, request.getParentId(), request.getLevel());
        } else if (request.getLevel() != null) {
            // updateDescendantLevels sadece {level: N} yollar
            part.setLevel(request.getLevel());
        }

        BomPart saved = bomPartRepository.save(part);
        log.info("BomPart updated: id={}, name={}, parentId={}, level={}",
                saved.getId(), saved.getName(), saved.getParentId(), saved.getLevel());
        return BomPartResponse.fromEntity(saved);
    }

    /**
     * Parcayi yeni parent'in altina tasir (null -> kok seviye).
     * Ayni urun kontrolu + dongu (kendi alt agacina tasima) kontrolu yapar.
     */
    private void applyParentChange(BomPart part, UUID newParentId, Integer requestedLevel) {
        if (newParentId == null) {
            part.setParentId(null);
            part.setLevel(requestedLevel != null ? requestedLevel : 0);
            return;
        }
        if (newParentId.equals(part.getId())) {
            throw new BusinessException(
                    "Bir parca kendi kendisinin ustune tasinamaz.",
                    "BOM_PART_SELF_PARENT");
        }
        BomPart parent = bomPartRepository.findById(newParentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BomPart (parent)", "id", newParentId));
        if (!parent.getProductId().equals(part.getProductId())) {
            throw new BusinessException(
                    "Parent parca farkli bir urune ait.",
                    "PARENT_PRODUCT_MISMATCH");
        }
        // Dongu kontrolu: yeni parent'in atalari arasinda bu parca varsa
        // tasima kendi alt agacinin icine yapiliyor demektir.
        UUID cursor = parent.getParentId();
        int guard = 0;
        while (cursor != null && guard++ < 500) {
            if (cursor.equals(part.getId())) {
                throw new BusinessException(
                        "Bir parca kendi alt parcasinin altina tasinamaz.",
                        "BOM_PART_CYCLE");
            }
            cursor = bomPartRepository.findById(cursor)
                    .map(BomPart::getParentId)
                    .orElse(null);
        }
        part.setParentId(newParentId);
        part.setLevel(requestedLevel != null
                ? requestedLevel
                : (parent.getLevel() != null ? parent.getLevel() : 0) + 1);
    }

    @Transactional
    public void delete(UUID id) {
        BomPart part = findEntityById(id);

        long childCount = bomPartRepository.countByParentId(id);
        if (childCount > 0) {
            throw new BusinessException(
                    "Bu parcanin " + childCount + " alt parcasi var, silinemez. "
                            + "Once alt parcalari silmelisin.",
                    "BOM_PART_HAS_CHILDREN"
            );
        }

        // FAZ 2 AKTIF: project_bom_parts'ta kullanim kontrolu
        long projectUsage = projectBomPartRepository.countByBomPartId(id);
        if (projectUsage > 0) {
            throw new BusinessException(
                    "Bu parca " + projectUsage + " projede kullaniliyor, silinemez. "
                            + "Once proje atamalarini silmelisin.",
                    "BOM_PART_IN_USE"
            );
        }

        bomPartRepository.delete(part);
        log.info("BomPart deleted: id={}, name={}", id, part.getName());
    }

    private BomPart findEntityById(UUID id) {
        return bomPartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomPart", "id", id));
    }
}