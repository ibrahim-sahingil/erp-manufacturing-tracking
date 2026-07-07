package com.uretimtakip.erp.part;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.part.dto.PartRequest;
import com.uretimtakip.erp.part.dto.PartResponse;
import com.uretimtakip.erp.part.dto.PartUpdateRequest;
import com.uretimtakip.erp.workorder.WorkOrderPartRepository;
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
    private final WorkOrderPartRepository workOrderPartRepository;

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
        // Kod benzersizligi PROJE kapsaminda ve harf duyarsiz:
        // ayni urun agaci farkli projelere yayinlanabilmeli.
        if (codeExistsInProject(request.getOrderId(), request.getCode(), null)) {
            throw new BusinessException(
                    "Bu projede bu kodda bir parca zaten var: " + request.getCode(),
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
                .parentPartId(validateParent(request.getParentPartId(), null))
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
    public PartResponse update(UUID id, PartUpdateRequest request) {
        Part part = findEntityById(id);

        // PARTIAL update: sadece gonderilen (non-null) alanlar islenir.
        // QR/ilerleme kaydi yalnizca {status, qty_*} yollar.
        if (request.getCode() != null && !request.getCode().isBlank()
                && !part.getCode().equalsIgnoreCase(request.getCode())) {
            UUID targetOrderId = request.getOrderId() != null
                    ? request.getOrderId() : part.getOrderId();
            if (codeExistsInProject(targetOrderId, request.getCode(), part.getId())) {
                throw new BusinessException(
                        "Bu projede bu kodda bir parca zaten var: " + request.getCode(),
                        "PART_CODE_EXISTS"
                );
            }
            part.setCode(request.getCode());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            part.setName(request.getName());
        }
        if (request.getDrawingNo() != null) part.setDrawingNo(request.getDrawingNo());
        if (request.getMaterial() != null) part.setMaterial(request.getMaterial());
        if (request.getOrderId() != null) part.setOrderId(request.getOrderId());
        if (request.getDepartmentId() != null) part.setDepartmentId(request.getDepartmentId());
        // Ust parca bagi (#8): explicit null = kaldir
        if (request.isParentPartIdPresent()) {
            part.setParentPartId(validateParent(request.getParentPartId(), part.getId()));
        }
        if (request.getTotalQty() != null) part.setTotalQty(request.getTotalQty());
        if (request.getStatus() != null) part.setStatus(request.getStatus());
        if (request.getDescription() != null) part.setDescription(request.getDescription());
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

        // (O1) Silme korkuluklari — BOM taraflarindaki desenle ayni:
        // is emri bagi / uretim ilerlemesi / alt parca varken tek confirm
        // ile silinmesin (work_order_parts + part_logs CASCADE gider,
        // alt parcalarin ust bagi sessizce NULL olurdu).
        long workOrderCount = workOrderPartRepository.countByPartId(id);
        if (workOrderCount > 0) {
            throw new BusinessException(
                    "Bu parca " + workOrderCount + " is emrine bagli, silinemez. "
                            + "Once is emirlerinden cikarin.",
                    "PART_IN_WORK_ORDER");
        }
        int progress = (part.getQtyDone() != null ? part.getQtyDone() : 0)
                + (part.getQtyReject() != null ? part.getQtyReject() : 0);
        if (progress > 0) {
            throw new BusinessException(
                    "Bu parcanin uretim ilerlemesi var (" + progress + " adet islenmis), "
                            + "silinemez. Yanlis kayit ise once ilerlemeyi sifirlayin.",
                    "PART_HAS_PROGRESS");
        }
        long childCount = partRepository.countByParentPartId(id);
        if (childCount > 0) {
            throw new BusinessException(
                    "Bu parcanin " + childCount + " alt parcasi var, silinemez. "
                            + "Once alt parcalari silin ya da baska ust parcaya tasiyin.",
                    "PART_HAS_CHILDREN");
        }

        partRepository.delete(part);
        log.info("Part deleted: id={}, code={}", id, part.getCode());
    }

    /**
     * Ust parca dogrulamasi (#8): mevcut olmali ve parca kendisine
     * baglanamamali. Gecerliyse ayni degeri dondurur.
     */
    private UUID validateParent(UUID parentId, UUID selfId) {
        if (parentId == null) return null;
        if (parentId.equals(selfId)) {
            throw new BusinessException(
                    "Parca kendisine ust parca olarak baglanamaz.",
                    "PART_PARENT_SELF");
        }
        if (!partRepository.existsById(parentId)) {
            throw new BusinessException(
                    "Ust parca bulunamadi.",
                    "PART_PARENT_NOT_FOUND");
        }
        return parentId;
    }

    /**
     * Ayni proje (order) icinde harf duyarsiz kod var mi?
     * excludeId verilirse o kayit haric tutulur (update senaryosu).
     */
    private boolean codeExistsInProject(UUID orderId, String code, UUID excludeId) {
        if (orderId == null) {
            return excludeId == null
                    ? partRepository.existsByOrderIdIsNullAndCodeIgnoreCase(code)
                    : partRepository.existsByOrderIdIsNullAndCodeIgnoreCaseAndIdNot(code, excludeId);
        }
        return excludeId == null
                ? partRepository.existsByOrderIdAndCodeIgnoreCase(orderId, code)
                : partRepository.existsByOrderIdAndCodeIgnoreCaseAndIdNot(orderId, code, excludeId);
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