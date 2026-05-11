package com.uretimtakip.erp.department;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.department.dto.DepartmentRequest;
import com.uretimtakip.erp.department.dto.DepartmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Departman is mantigi.
 *
 * Saglanan operasyonlar:
 *   - listAll()     : Tum departmanlari listele
 *   - getById(id)   : Tek departman getir
 *   - create(req)   : Yeni departman olustur
 *   - update(id,req): Guncelle
 *   - delete(id)    : Sil
 *   - listByOrder(orderId) : Belirli siparise bagli departmanlari getir
 *
 * @Transactional: DB islemi sirasinda hata olursa otomatik rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(DepartmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(UUID id) {
        Department department = findEntityById(id);
        return DepartmentResponse.fromEntity(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listByOrder(UUID orderId) {
        return departmentRepository.findByOrderId(orderId)
                .stream()
                .map(DepartmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        // Ayni isimde departman var mi kontrol et
        if (departmentRepository.existsByName(request.getName())) {
            throw new BusinessException(
                    "Bu isimde bir departman zaten var: " + request.getName(),
                    "DEPARTMENT_ALREADY_EXISTS"
            );
        }

        Department department = Department.builder()
                .name(request.getName())
                .orderId(request.getOrderId())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 1)
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created: id={}, name={}", saved.getId(), saved.getName());

        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        Department department = findEntityById(id);

        // Eger isim degisti VE yeni isim baska bir departmanda kullaniliyorsa hata ver
        if (!department.getName().equals(request.getName())
                && departmentRepository.existsByName(request.getName())) {
            throw new BusinessException(
                    "Bu isimde bir departman zaten var: " + request.getName(),
                    "DEPARTMENT_ALREADY_EXISTS"
            );
        }

        department.setName(request.getName());
        department.setOrderId(request.getOrderId());
        if (request.getSortOrder() != null) {
            department.setSortOrder(request.getSortOrder());
        }

        Department updated = departmentRepository.save(department);
        log.info("Department updated: id={}, name={}", updated.getId(), updated.getName());

        return DepartmentResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Department department = findEntityById(id);
        departmentRepository.delete(department);
        log.info("Department deleted: id={}, name={}", id, department.getName());
    }

    /**
     * Internal helper - entity'yi bul, yoksa exception firlat.
     */
    private Department findEntityById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }
}