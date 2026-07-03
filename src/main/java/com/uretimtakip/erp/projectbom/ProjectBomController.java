package com.uretimtakip.erp.projectbom;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.projectbom.dto.ProjectBomRequest;
import com.uretimtakip.erp.projectbom.dto.ProjectBomResponse;
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
 * ProjectBom REST API.
 *
 * Endpoint'ler:
 *   GET    /api/project-bom                         - tum ProjectBom kayitlari (yeni->eski)
 *   GET    /api/project-bom?projectName=X           - bir projedeki BOM atamalari
 *   GET    /api/project-bom/by-product/{productId}  - bir urune yapilan tum atamalar
 *   GET    /api/project-bom/{id}                    - tek kayit
 *   POST   /api/project-bom                         - yeni ProjectBom + AUTO-POPULATE
 *   PUT    /api/project-bom/{id}                    - guncelle (bomProductId IGNORE)
 *   DELETE /api/project-bom/{id}                    - sil (CASCADE)
 */
@RestController
@RequestMapping("/api/project-bom")
@RequiredArgsConstructor
public class ProjectBomController {

    private final ProjectBomService projectBomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectBomResponse>>> list(
            @RequestParam(required = false) String projectName) {
        List<ProjectBomResponse> data = (projectName != null && !projectName.isBlank())
                ? projectBomService.listByProject(projectName)
                : projectBomService.listAll();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/by-product/{bomProductId}")
    public ResponseEntity<ApiResponse<List<ProjectBomResponse>>> listByProduct(
            @PathVariable UUID bomProductId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectBomService.listByBomProduct(bomProductId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectBomResponse>> getById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectBomService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectBomResponse>> create(
            @Valid @RequestBody ProjectBomRequest request) {
        ProjectBomResponse created = projectBomService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Proje BOM olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectBomResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectBomRequest request) {
        ProjectBomResponse updated = projectBomService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Proje BOM guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        projectBomService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Proje BOM silindi", null));
    }
}