package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.workorder.dto.WorkOrderRequest;
import com.uretimtakip.erp.workorder.dto.WorkOrderResponse;
import com.uretimtakip.erp.workorder.dto.WorkOrderUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> list(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) UUID assignedUserId) {

        List<WorkOrderResponse> orders;
        if (orderId != null) {
            orders = workOrderService.listByOrder(orderId);
        } else if (status != null && !status.isBlank()) {
            orders = workOrderService.listByStatus(status);
        } else if (workspaceId != null) {
            orders = workOrderService.listByWorkspace(workspaceId);
        } else if (assignedUserId != null) {
            orders = workOrderService.listByAssignedUser(assignedUserId);
        } else {
            orders = workOrderService.listAll();
        }

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderResponse>> create(
            @Valid @RequestBody WorkOrderRequest request) {

        WorkOrderResponse created = workOrderService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Is emri olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkOrderUpdateRequest request) {

        WorkOrderResponse updated = workOrderService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Is emri guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        workOrderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Is emri silindi", null));
    }
}