package com.uretimtakip.erp.part;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.part.dto.PartRequest;
import com.uretimtakip.erp.part.dto.PartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Part is mantigi. SADECE Part CRUD.
 *
 * NOT: PartLog yonetimi PartLogService'te.
 *      PartLog ekleme islemi (uretim kaydi) PartLogService.create() icinden
 *      Part'in qty_done/pending/reject alanlarini guncelleyecek.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;

    @Transactional(readOnly = true)
    public List<PartResponse> listAll() {
        return partRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PartResponse getById(UUID id) {
        return PartResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<PartResponse> listByOrder(UUID orderId) {
        return partRepository.findByOrderId(orderId)
                .stream()
                .map(PartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PartResponse> listByDepartment(UUID departmentId) {
        return partRepository.findByDepartmentId(departmentId)
                .stream()
                .map(PartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PartResponse create(PartRequest request) {
        if (partRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                    "Bu kodda bir parca zaten var: " + request.getCode(),
                    "PART_CODE_EXISTS"
            );
        }

        Part part = Part.builder()
                .name(request.getName())
                .code(request.getCode())
                .drawingNo(request.getDrawingNo())
                .material(request.getMaterial())
                .orderId(request.getOrderId())
                .departmentId(request.getDepartmentId())
                .totalQty(request.getTotalQty() != null ? request.getTotalQty() : 1)
                .status(request.getStatus() != null ? request.getStatus() : "PENDING")
                .description(request.getDescription())
                .qtyDone(request.getQtyDone() != null ? request.getQtyDone() : 0)
                .qtyPending(request.getQtyPending() != null ? request.getQtyPending() : 0)
                .qtyReject(request.getQtyReject() != null ? request.getQtyReject() : 0)
                .build();

        Part saved = partRepository.save(part);
        log.info("Part created: id={}, code={}, name={}", saved.getId(), saved.getCode(), saved.getName());

        return PartResponse.fromEntity(saved);
    }

    @Transactional
    public PartResponse update(UUID id, PartRequest request) {
        Part part = findEntityById(id);

        if (!part.getCode().equals(request.getCode())
                && partRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                    "Bu kodda bir parca zaten var: " + request.getCode(),
                    "PART_CODE_EXISTS"
            );
        }

        part.setName(request.getName());
        part.setCode(request.getCode());
        part.setDrawingNo(request.getDrawingNo());
        part.setMaterial(request.getMaterial());
        part.setOrderId(request.getOrderId());
        part.setDepartmentId(request.getDepartmentId());
        if (request.getTotalQty() != null) part.setTotalQty(request.getTotalQty());
        if (request.getStatus() != null) part.setStatus(request.getStatus());
        part.setDescription(request.getDescription());
        if (request.getQtyDone() != null) part.setQtyDone(request.getQtyDone());
        if (request.getQtyPending() != null) part.setQtyPending(request.getQtyPending());
        if (request.getQtyReject() != null) part.setQtyReject(request.getQtyReject());

        Part updated = partRepository.save(part);
        log.info("Part updated: id={}, code={}", updated.getId(), updated.getCode());

        return PartResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Part part = findEntityById(id);
        partRepository.delete(part);
        log.info("Part deleted: id={}, code={}", id, part.getCode());
    }

    /**
     * Internal helper - PartLogService bunu kullanir.
     */
    @Transactional(readOnly = true)
    public Part findEntityById(UUID id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part", "id", id));
    }
}