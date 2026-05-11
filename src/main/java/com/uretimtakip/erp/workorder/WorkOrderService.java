package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.workorder.dto.WorkOrderRequest;
import com.uretimtakip.erp.workorder.dto.WorkOrderResponse;
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
                .build();

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("WorkOrder created: id={}", saved.getId());

        return WorkOrderResponse.fromEntity(saved);
    }

    @Transactional
    public WorkOrderResponse update(UUID id, WorkOrderRequest request) {
        WorkOrder workOrder = findEntityById(id);

        workOrder.setOrderId(request.getOrderId());
        workOrder.setDepartmentId(request.getDepartmentId());
        workOrder.setWorkspaceId(request.getWorkspaceId());
        workOrder.setAssignedUserId(request.getAssignedUserId());
        workOrder.setStartDatetime(request.getStartDatetime());
        workOrder.setEndDatetime(request.getEndDatetime());
        if (request.getStatus() != null) workOrder.setStatus(request.getStatus());
        workOrder.setNotes(request.getNotes());

        WorkOrder updated = workOrderRepository.save(workOrder);
        log.info("WorkOrder updated: id={}", updated.getId());

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