package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.workorder.dto.WorkOrderRevisionRequest;
import com.uretimtakip.erp.workorder.dto.WorkOrderRevisionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-order-revisions")
@RequiredArgsConstructor
public class WorkOrderRevisionController {

    private final WorkOrderRevisionService revisionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkOrderRevisionResponse>>> list(
            @RequestParam(required = false) UUID workOrderId,
            @RequestParam(required = false) UUID userId) {

        List<WorkOrderRevisionResponse> result;
        if (workOrderId != null) {
            result = revisionService.listByWorkOrder(workOrderId);
        } else if (userId != null) {
            result = revisionService.listByUser(userId);
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("workOrderId veya userId parametresi zorunlu", "MISSING_PARAM")
            );
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderRevisionResponse>> create(
            @Valid @RequestBody WorkOrderRevisionRequest request) {

        WorkOrderRevisionResponse created = revisionService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Revize kaydedildi", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        revisionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Revize silindi", null));
    }
}