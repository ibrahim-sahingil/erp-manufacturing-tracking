package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomPartRequest;
import com.uretimtakip.erp.bom.dto.BomPartResponse;
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
 * Update'te: IMMUTABLE alanlar (productId, parentId, level).
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
    public BomPartResponse update(UUID id, BomPartRequest request) {
        BomPart part = findEntityById(id);

        part.setName(request.getName());
        part.setCode(request.getCode());
        if (request.getQuantity() != null) {
            part.setQuantity(request.getQuantity());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            part.setUnit(request.getUnit());
        }
        part.setWeightKg(request.getWeightKg());
        part.setMaterial(request.getMaterial());
        if (request.getOperations() != null) {
            part.setOperations(request.getOperations());
        }
        if (request.getSortOrder() != null) {
            part.setSortOrder(request.getSortOrder());
        }

        BomPart saved = bomPartRepository.save(part);
        log.info("BomPart updated: id={}, name={}", saved.getId(), saved.getName());
        return BomPartResponse.fromEntity(saved);
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