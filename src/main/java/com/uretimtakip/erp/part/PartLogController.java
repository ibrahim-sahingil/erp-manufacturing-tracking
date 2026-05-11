package com.uretimtakip.erp.part;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.part.dto.PartLogRequest;
import com.uretimtakip.erp.part.dto.PartLogResponse;
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
 * PartLog REST endpoints.
 *
 *   GET    /api/part-logs                  -> Tum loglar (yeni->eski)
 *   GET    /api/part-logs/{id}             -> Tek log
 *   GET    /api/part-logs?partId=xxx       -> Belirli parca loglari
 *   GET    /api/part-logs?userId=xxx       -> Belirli kullanici loglari
 *   POST   /api/part-logs                  -> Yeni log + Part qty guncelle
 *   DELETE /api/part-logs/{id}             -> Sil
 *
 * NOT: PUT yok - log kaydi degistirilmez (audit trail).
 */
@RestController
@RequestMapping("/api/part-logs")
@RequiredArgsConstructor
public class PartLogController {

    private final PartLogService partLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PartLogResponse>>> list(
            @RequestParam(required = false) UUID partId,
            @RequestParam(required = false) UUID userId) {

        List<PartLogResponse> logs;
        if (partId != null) {
            logs = partLogService.listByPart(partId);
        } else if (userId != null) {
            logs = partLogService.listByUser(userId);
        } else {
            logs = partLogService.listAll();
        }

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartLogResponse>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(partLogService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PartLogResponse>> create(
            @Valid @RequestBody PartLogRequest request) {

        PartLogResponse created = partLogService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Log kaydi olusturuldu", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        partLogService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Log silindi", null));
    }
}