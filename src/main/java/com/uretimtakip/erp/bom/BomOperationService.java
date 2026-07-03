package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomOperationRequest;
import com.uretimtakip.erp.bom.dto.BomOperationResponse;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BomOperation is mantigi.
 *
 * - code UNIQUE: create/update'te kontrol edilir, cakisma varsa BusinessException
 * - delete: bom_parts.operations jsonb icinde bu code geciyor mu kontrol edilir,
 *           geciyorsa silinemez (BusinessException).
 *
 * NOT: bom_parts.operations jsonb List<Map<String,Object>> formatinda. Map
 * icinde "code" veya "operationCode" anahtarinin degerine bakariz. Frontend
 * tarafinda hangi anahtar kullanildigi netlestiginde burayi guncelleyebilirsin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomOperationService {

    private final BomOperationRepository bomOperationRepository;
    private final BomPartRepository bomPartRepository;

    @Transactional(readOnly = true)
    public List<BomOperationResponse> listAll() {
        return bomOperationRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(BomOperationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BomOperationResponse getById(UUID id) {
        BomOperation op = bomOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomOperation", "id", id));
        return BomOperationResponse.fromEntity(op);
    }

    @Transactional
    public BomOperationResponse create(BomOperationRequest request) {
        // code UNIQUE kontrolu
        if (bomOperationRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                    "Bu kod zaten kullaniliyor: " + request.getCode(),
                    "DUPLICATE_CODE"
            );
        }

        BomOperation op = BomOperation.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        BomOperation saved = bomOperationRepository.save(op);
        log.info("BomOperation created: id={}, code={}", saved.getId(), saved.getCode());
        return BomOperationResponse.fromEntity(saved);
    }

    @Transactional
    public BomOperationResponse update(UUID id, BomOperationRequest request) {
        BomOperation op = bomOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomOperation", "id", id));

        // Code degisiyor ve yeni code baska bir kayitta varsa hata
        if (!op.getCode().equals(request.getCode())
                && bomOperationRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                    "Bu kod zaten kullaniliyor: " + request.getCode(),
                    "DUPLICATE_CODE"
            );
        }

        op.setName(request.getName());
        op.setCode(request.getCode());
        op.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            op.setSortOrder(request.getSortOrder());
        }

        BomOperation saved = bomOperationRepository.save(op);
        log.info("BomOperation updated: id={}", saved.getId());
        return BomOperationResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        BomOperation op = bomOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomOperation", "id", id));

        // Kullanimda mi? bom_parts.operations jsonb icinde bu code geciyor mu?
        long usageCount = bomPartRepository.countByOperationCode(op.getCode());
        if (usageCount > 0) {
            throw new BusinessException(
                    "Bu operasyon " + usageCount + " parcada kullaniliyor, silinemez",
                    "OPERATION_IN_USE"
            );
        }

        bomOperationRepository.delete(op);
        log.info("BomOperation deleted: id={}, code={}", id, op.getCode());
    }
}