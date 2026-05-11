package com.uretimtakip.erp.projectdate;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.projectdate.dto.ProjectDateRequest;
import com.uretimtakip.erp.projectdate.dto.ProjectDateResponse;
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
 * ProjectDate REST endpoints.
 *
 *   GET    /api/project-dates                  -> Tum proje tarihleri
 *   GET    /api/project-dates/{id}             -> Tek kayit
 *   GET    /api/project-dates?orderId=xxx      -> Belirli siparis tarihleri
 *   POST   /api/project-dates                  -> Yeni tarih
 *   PUT    /api/project-dates/{id}             -> Guncelle
 *   DELETE /api/project-dates/{id}             -> Sil
 */
@RestController
@RequestMapping("/api/project-dates")
@RequiredArgsConstructor
public class ProjectDateController {

    private final ProjectDateService projectDateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDateResponse>>> list(
            @RequestParam(required = false) UUID orderId) {

        List<ProjectDateResponse> dates = (orderId != null)
                ? projectDateService.listByOrder(orderId)
                : projectDateService.listAll();

        return ResponseEntity.ok(ApiResponse.success(dates));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDateResponse>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectDateService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDateResponse>> create(
            @Valid @RequestBody ProjectDateRequest request) {

        ProjectDateResponse created = projectDateService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Proje tarihi olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDateResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectDateRequest request) {

        ProjectDateResponse updated = projectDateService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Proje tarihi guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        projectDateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Proje tarihi silindi", null));
    }
}