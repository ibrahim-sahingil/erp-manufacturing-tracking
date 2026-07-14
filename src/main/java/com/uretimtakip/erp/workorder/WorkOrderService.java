package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.workorder.dto.WorkOrderRequest;
import com.uretimtakip.erp.workorder.dto.WorkOrderResponse;
import com.uretimtakip.erp.workorder.dto.WorkOrderUpdateRequest;
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
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> listAll() {
        return workOrderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(WorkOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse getById(UUID id) {
        return WorkOrderResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> listByOrder(UUID orderId) {
        return workOrderRepository.findByOrderId(orderId)
                .stream()
                .map(WorkOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> listByStatus(String status) {
        return workOrderRepository.findByStatus(status)
                .stream()
                .map(WorkOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> listByWorkspace(UUID workspaceId) {
        return workOrderRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(WorkOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> listByAssignedUser(UUID userId) {
        return workOrderRepository.findByAssignedUserId(userId)
                .stream()
                .map(WorkOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request) {
        WorkOrder workOrder = WorkOrder.builder()
                .orderId(request.getOrderId())
                .departmentId(request.getDepartmentId())
                .workspaceId(request.getWorkspaceId())
                .assignedUserId(request.getAssignedUserId())
                .startDatetime(request.getStartDatetime())
                .endDatetime(request.getEndDatetime())
                .status(request.getStatus() != null ? request.getStatus() : "planned")
                .notes(request.getNotes())
                .code(nextCode())
                .build();

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("WorkOrder created: id={}, code={}", saved.getId(), saved.getCode());

        return WorkOrderResponse.fromEntity(saved);
    }

    /**
     * İnsan-okur iş emri numarası: İE-YYYY-NNN, yıl bazında ardışık.
     * NNN sıfır dolgulu olduğundan sözlük sırası = sayısal sıra; yılın en
     * büyük kodu bulunur, +1 verilir. (Tek kullanıcılı atölye — yarış
     * durumu pratikte yok; olursa unique constraint yakalar.)
     */
    private String nextCode() {
        String prefix = "İE-" + java.time.Year.now().getValue() + "-";
        int next = workOrderRepository.findTopByCodeStartingWithOrderByCodeDesc(prefix)
                .map(w -> {
                    try {
                        return Integer.parseInt(w.getCode().substring(prefix.length())) + 1;
                    } catch (NumberFormatException e) {
                        return 1;
                    }
                })
                .orElse(1);
        return prefix + String.format("%03d", next);
    }

    @Transactional
    public WorkOrderResponse update(UUID id, WorkOrderUpdateRequest request) {
        WorkOrder workOrder = findEntityById(id);

        // PARTIAL update: sadece gonderilen (non-null) alanlar islenir.
        // Dashboard {status} tek basina yollar; revize modali da kismi yollar.
        if (request.getOrderId() != null) workOrder.setOrderId(request.getOrderId());
        if (request.getDepartmentId() != null) workOrder.setDepartmentId(request.getDepartmentId());
        if (request.getWorkspaceId() != null) workOrder.setWorkspaceId(request.getWorkspaceId());
        if (request.getAssignedUserId() != null) workOrder.setAssignedUserId(request.getAssignedUserId());
        if (request.getStartDatetime() != null) workOrder.setStartDatetime(request.getStartDatetime());
        if (request.getEndDatetime() != null) workOrder.setEndDatetime(request.getEndDatetime());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            workOrder.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) workOrder.setNotes(request.getNotes());

        WorkOrder updated = workOrderRepository.save(workOrder);
        log.info("WorkOrder updated: id={}, status={}", updated.getId(), updated.getStatus());

        return WorkOrderResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        WorkOrder workOrder = findEntityById(id);
        workOrderRepository.delete(workOrder);
        log.info("WorkOrder deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public WorkOrder findEntityById(UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", "id", id));
    }
}