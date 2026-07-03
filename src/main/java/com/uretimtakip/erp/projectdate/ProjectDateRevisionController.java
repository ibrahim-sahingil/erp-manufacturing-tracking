package com.uretimtakip.erp.projectdate;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.projectdate.dto.ProjectDateRevisionRequest;
import com.uretimtakip.erp.projectdate.dto.ProjectDateRevisionResponse;
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

/**
 * ProjectDateRevision REST endpoints.
 *
 *   GET    /api/project-date-revisions?projectDateId=xxx -> Bir tarihin revizyonlari
 *   GET    /api/project-date-revisions?userId=xxx        -> Bir kullanicinin revizyonlari
 *   POST   /api/project-date-revisions                   -> Yeni revize (ProjectDate'i de gunceller)
 *   DELETE /api/project-date-revisions/{id}              -> Sil
 */
@RestController
@RequestMapping("/api/project-date-revisions")
@RequiredArgsConstructor
public class ProjectDateRevisionController {

    private final ProjectDateRevisionService revisionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDateRevisionResponse>>> list(
            @RequestParam(required = false) UUID projectDateId,
            @RequestParam(required = false) UUID userId) {

        List<ProjectDateRevisionResponse> result;
        if (projectDateId != null) {
            result = revisionService.listByProjectDate(projectDateId);
        } else if (userId != null) {
            result = revisionService.listByUser(userId);
        } else {
            // Filtre verilmezse tum liste (frontend filtreyi client-side uyguluyor)
            result = revisionService.listAll();
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDateRevisionResponse>> create(
            @Valid @RequestBody ProjectDateRevisionRequest request) {

        ProjectDateRevisionResponse created = revisionService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tarih revize edildi", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        revisionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Revize silindi", null));
    }
}