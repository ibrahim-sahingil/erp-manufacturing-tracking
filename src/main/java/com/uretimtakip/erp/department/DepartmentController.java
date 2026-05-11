package com.uretimtakip.erp.department;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.department.dto.DepartmentRequest;
import com.uretimtakip.erp.department.dto.DepartmentResponse;
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
 * Department REST endpoint'leri.
 *
 * Endpoints:
 *   GET    /api/departments                 -> Tum departmanlar
 *   GET    /api/departments/{id}            -> Tek departman
 *   GET    /api/departments?orderId=xxx     -> Belirli siparis departmanlari
 *   POST   /api/departments                 -> Yeni departman olustur
 *   PUT    /api/departments/{id}            -> Guncelle
 *   DELETE /api/departments/{id}            -> Sil
 *
 * Tum endpoint'ler JWT korumali (SecurityConfig'te /api/auth/** disindaki
 * her sey authentication ister).
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> list(
            @RequestParam(required = false) UUID orderId) {

        List<DepartmentResponse> departments = (orderId != null)
                ? departmentService.listByOrder(orderId)
                : departmentService.listAll();

        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getOne(@PathVariable UUID id) {
        DepartmentResponse department = departmentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(department));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(
            @Valid @RequestBody DepartmentRequest request) {

        DepartmentResponse created = departmentService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Departman olusturuldu", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest request) {

        DepartmentResponse updated = departmentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Departman guncellendi", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Departman silindi", null));
    }
}