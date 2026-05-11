package com.uretimtakip.erp.workspace;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.workspace.dto.WorkspaceRequest;
import com.uretimtakip.erp.workspace.dto.WorkspaceResponse;
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

/**
 * Workspace REST endpoint'leri.
 */
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> list(
            @RequestParam(required = false) String type) {

        List<WorkspaceResponse> workspaces = (type != null && !type.isBlank())
                ? workspaceService.listByType(type)
                : workspaceService.listAll();

        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @Valid @RequestBody WorkspaceRequest request) {

        WorkspaceResponse created = workspaceService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workspace olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkspaceRequest request) {

        WorkspaceResponse updated = workspaceService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Workspace guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        workspaceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Workspace silindi", null));
    }
}