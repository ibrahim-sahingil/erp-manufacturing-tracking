package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.workorder.dto.WorkOrderPartRequest;
import com.uretimtakip.erp.workorder.dto.WorkOrderPartResponse;
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
@RequestMapping("/api/work-order-parts")
@RequiredArgsConstructor
public class WorkOrderPartController {

    private final WorkOrderPartService workOrderPartService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkOrderPartResponse>>> list(
            @RequestParam(required = false) UUID workOrderId,
            @RequestParam(required = false) UUID partId) {

        List<WorkOrderPartResponse> result;
        if (workOrderId != null) {
            result = workOrderPartService.listByWorkOrder(workOrderId);
        } else if (partId != null) {
            result = workOrderPartService.listByPart(partId);
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("workOrderId veya partId parametresi zorunlu", "MISSING_PARAM")
            );
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderPartResponse>> create(
            @Valid @RequestBody WorkOrderPartRequest request) {

        WorkOrderPartResponse created = workOrderPartService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Parca is emrine eklendi", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrderPartResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkOrderPartRequest request) {

        WorkOrderPartResponse updated = workOrderPartService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        workOrderPartService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Silindi", null));
    }
}