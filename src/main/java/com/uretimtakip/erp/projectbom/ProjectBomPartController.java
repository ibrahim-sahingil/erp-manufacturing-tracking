package com.uretimtakip.erp.projectbom;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.projectbom.dto.ProjectBomPartRequest;
import com.uretimtakip.erp.projectbom.dto.ProjectBomPartResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * ProjectBomPart REST API.
 *
 * Endpoint'ler:
 *   GET    /api/project-bom-parts/by-project-bom/{projectBomId} - tum parcalar
 *   GET    /api/project-bom-parts/by-parent/{parentCustomId}    - alt parcalar
 *   GET    /api/project-bom-parts/{id}                          - tek parca
 *   POST   /api/project-bom-parts                               - yeni parca
 *   PUT    /api/project-bom-parts/{id}                          - guncelle
 *   DELETE /api/project-bom-parts/{id}                          - sil (defensive)
 *
 * NOT: List all endpoint'i koymadik - cunku tum tablonun fiziksel dumpini
 * gostermek anlamsiz, frontend hep projectBomId bazinda calisir.
 */
@RestController
@RequestMapping("/api/project-bom-parts")
@RequiredArgsConstructor
public class ProjectBomPartController {

    private final ProjectBomPartService projectBomPartService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectBomPartResponse>>> listAll() {
        // Frontend planlama ekrani tum listeyi ceker, filtreyi client-side uygular
        return ResponseEntity.ok(ApiResponse.success(projectBomPartService.listAll()));
    }

    @GetMapping("/by-project-bom/{projectBomId}")
    public ResponseEntity<ApiResponse<List<ProjectBomPartResponse>>> listByProjectBom(
            @PathVariable UUID projectBomId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectBomPartService.listByProjectBom(projectBomId)));
    }

    @GetMapping("/by-parent/{parentCustomId}")
    public ResponseEntity<ApiResponse<List<ProjectBomPartResponse>>> listByParent(
            @PathVariable UUID parentCustomId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectBomPartService.listByParent(parentCustomId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectBomPartResponse>> getById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectBomPartService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectBomPartResponse>> create(
            @Valid @RequestBody ProjectBomPartRequest request) {
        ProjectBomPartResponse created = projectBomPartService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Proje parcasi olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectBomPartResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectBomPartRequest request) {
        ProjectBomPartResponse updated = projectBomPartService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Proje parcasi guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        projectBomPartService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Proje parcasi silindi", null));
    }
}