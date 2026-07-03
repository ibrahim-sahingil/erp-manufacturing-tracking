package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.part.PartRepository;
import com.uretimtakip.erp.workorder.dto.WorkOrderPartRequest;
import com.uretimtakip.erp.workorder.dto.WorkOrderPartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderPartService {

    private final WorkOrderPartRepository workOrderPartRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PartRepository partRepository;

    @Transactional(readOnly = true)
    public List<WorkOrderPartResponse> listAll() {
        return workOrderPartRepository.findAll()
                .stream()
                .map(WorkOrderPartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderPartResponse> listByWorkOrder(UUID workOrderId) {
        return workOrderPartRepository.findByWorkOrderId(workOrderId)
                .stream()
                .map(WorkOrderPartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderPartResponse> listByPart(UUID partId) {
        return workOrderPartRepository.findByPartId(partId)
                .stream()
                .map(WorkOrderPartResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkOrderPartResponse create(WorkOrderPartRequest request) {
        // WorkOrder var mi?
        if (!workOrderRepository.existsById(request.getWorkOrderId())) {
            throw new ResourceNotFoundException("WorkOrder", "id", request.getWorkOrderId());
        }

        // Part var mi?
        if (!partRepository.existsById(request.getPartId())) {
            throw new ResourceNotFoundException("Part", "id", request.getPartId());
        }

        // Bu kombinasyon zaten var mi?
        if (workOrderPartRepository.existsByWorkOrderIdAndPartId(
                request.getWorkOrderId(), request.getPartId())) {
            throw new BusinessException(
                    "Bu parca zaten bu is emrinde",
                    "WORK_ORDER_PART_ALREADY_EXISTS"
            );
        }

        WorkOrderPart wp = WorkOrderPart.builder()
                .workOrderId(request.getWorkOrderId())
                .partId(request.getPartId())
                .qty(request.getQty() != null ? request.getQty() : 1)
                .build();

        WorkOrderPart saved = workOrderPartRepository.save(wp);
        log.info("WorkOrderPart created: workOrderId={}, partId={}, qty={}",
                request.getWorkOrderId(), request.getPartId(), request.getQty());

        return WorkOrderPartResponse.fromEntity(saved);
    }

    @Transactional
    public WorkOrderPartResponse update(UUID id, WorkOrderPartRequest request) {
        WorkOrderPart wp = workOrderPartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrderPart", "id", id));

        if (request.getQty() != null) wp.setQty(request.getQty());

        WorkOrderPart updated = workOrderPartRepository.save(wp);
        log.info("WorkOrderPart updated: id={}", id);

        return WorkOrderPartResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        WorkOrderPart wp = workOrderPartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrderPart", "id", id));

        workOrderPartRepository.delete(wp);
        log.info("WorkOrderPart deleted: id={}", id);
    }
}