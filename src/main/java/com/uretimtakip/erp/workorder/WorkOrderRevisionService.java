package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.workorder.dto.WorkOrderRevisionRequest;
import com.uretimtakip.erp.workorder.dto.WorkOrderRevisionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WorkOrderRevision is mantigi.
 *
 * Audit trail (denetim izi) tutuyor:
 * Bir is emrindeki her alan degisikligi icin bir kayit dusuyor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderRevisionService {

    private final WorkOrderRevisionRepository revisionRepository;
    private final WorkOrderRepository workOrderRepository;

    @Transactional(readOnly = true)
    public List<WorkOrderRevisionResponse> listAll() {
        return revisionRepository.findAll()
                .stream()
                .map(WorkOrderRevisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderRevisionResponse> listByWorkOrder(UUID workOrderId) {
        return revisionRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId)
                .stream()
                .map(WorkOrderRevisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderRevisionResponse> listByUser(UUID userId) {
        return revisionRepository.findByRevisedByOrderByCreatedAtDesc(userId)
                .stream()
                .map(WorkOrderRevisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkOrderRevisionResponse create(WorkOrderRevisionRequest request) {
        // WorkOrder var mi?
        if (!workOrderRepository.existsById(request.getWorkOrderId())) {
            throw new ResourceNotFoundException("WorkOrder", "id", request.getWorkOrderId());
        }

        WorkOrderRevision revision = WorkOrderRevision.builder()
                .workOrderId(request.getWorkOrderId())
                .fieldChanged(request.getFieldChanged())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .reason(request.getReason())
                .revisedBy(request.getRevisedBy())
                .build();

        WorkOrderRevision saved = revisionRepository.save(revision);
        log.info("WorkOrderRevision created: workOrderId={}, field={}, old={}, new={}",
                request.getWorkOrderId(), request.getFieldChanged(),
                request.getOldValue(), request.getNewValue());

        return WorkOrderRevisionResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        WorkOrderRevision revision = revisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrderRevision", "id", id));

        revisionRepository.delete(revision);
        log.info("WorkOrderRevision deleted: id={}", id);
    }
}