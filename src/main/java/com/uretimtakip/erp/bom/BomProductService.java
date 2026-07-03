package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomProductRequest;
import com.uretimtakip.erp.bom.dto.BomProductResponse;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.projectbom.ProjectBomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BomProduct is mantigi.
 *
 * - Code UNIQUE degil, o yuzden cakisma kontrolu yok
 * - Delete:
 *     1. bom_parts.product_id = id olan kayit var mi?
 *     2. project_bom.bom_product_id = id olan kayit var mi? (FAZ 2 AKTIF)
 *     Varsa BusinessException.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomProductService {

    private final BomProductRepository bomProductRepository;
    private final BomPartRepository bomPartRepository;
    private final ProjectBomRepository projectBomRepository; // FAZ 2 EKLENDI

    @Transactional(readOnly = true)
    public List<BomProductResponse> listAll() {
        return bomProductRepository.findAllByOrderByNameAsc()
                .stream()
                .map(BomProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BomProductResponse getById(UUID id) {
        BomProduct product = bomProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomProduct", "id", id));
        return BomProductResponse.fromEntity(product);
    }

    @Transactional
    public BomProductResponse create(BomProductRequest request) {
        BomProduct product = BomProduct.builder()
                .name(request.getName())
                .code(request.getCode())
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .description(request.getDescription())
                .build();

        BomProduct saved = bomProductRepository.save(product);
        log.info("BomProduct created: id={}, name={}", saved.getId(), saved.getName());
        return BomProductResponse.fromEntity(saved);
    }

    @Transactional
    public BomProductResponse update(UUID id, BomProductRequest request) {
        BomProduct product = bomProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomProduct", "id", id));

        product.setName(request.getName());
        product.setCode(request.getCode());
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            product.setUnit(request.getUnit());
        }
        product.setDescription(request.getDescription());

        BomProduct saved = bomProductRepository.save(product);
        log.info("BomProduct updated: id={}", saved.getId());
        return BomProductResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        BomProduct product = bomProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomProduct", "id", id));

        // 1. bom_parts'ta kullanim kontrolu
        long partsCount = bomPartRepository.countByProductId(id);
        if (partsCount > 0) {
            throw new BusinessException(
                    "Bu urune bagli " + partsCount + " parca var, silinemez. "
                            + "Once parcalari silmelisin.",
                    "PRODUCT_IN_USE"
            );
        }

        // 2. project_bom'da kullanim kontrolu (FAZ 2 AKTIF)
        long projectCount = projectBomRepository.countByBomProductId(id);
        if (projectCount > 0) {
            throw new BusinessException(
                    "Bu urun " + projectCount + " projede kullaniliyor, silinemez. "
                            + "Once proje BOM atamalarini silmelisin.",
                    "PRODUCT_IN_USE"
            );
        }

        bomProductRepository.delete(product);
        log.info("BomProduct deleted: id={}, name={}", id, product.getName());
    }
}