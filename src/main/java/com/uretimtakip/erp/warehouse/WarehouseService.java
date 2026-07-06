package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.purchasing.PurchaseItemRepository;
import com.uretimtakip.erp.user.UserRepository;
import com.uretimtakip.erp.warehouse.dto.WarehouseRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseResponse;
import com.uretimtakip.erp.warehouse.dto.WarehouseUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Warehouse is mantigi.
 *
 * Kurallar:
 * - Depo adi benzersiz (harf duyarsiz) -> WAREHOUSE_NAME_DUPLICATE
 * - Sorumlu kullanici mevcut olmali -> WAREHOUSE_RESPONSIBLE_NOT_FOUND
 * - Uzerinde hareket veya bagli satin alma kalemi olan depo silinemez
 *   -> WAREHOUSE_IN_USE ("once pasife alin")
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMovementRepository warehouseMovementRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> listAll(boolean onlyActive) {
        List<Warehouse> list = onlyActive
                ? warehouseRepository.findByIsActiveTrueOrderByNameAsc()
                : warehouseRepository.findAllByOrderByNameAsc();
        return list.stream()
                .map(WarehouseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(UUID id) {
        return WarehouseResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        if (warehouseRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException(
                    "Bu adla bir depo zaten var: " + request.getName(),
                    "WAREHOUSE_NAME_DUPLICATE");
        }
        validateResponsibleUser(request.getResponsibleUserId());

        Warehouse warehouse = Warehouse.builder()
                .name(request.getName().trim())
                .location(request.getLocation())
                .responsibleUserId(request.getResponsibleUserId())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created: id={}, name={}", saved.getId(), saved.getName());
        return WarehouseResponse.fromEntity(saved);
    }

    @Transactional
    public WarehouseResponse update(UUID id, WarehouseUpdateRequest request) {
        Warehouse warehouse = findEntityById(id);

        // PARTIAL update: sadece gonderilen alanlar islenir.
        if (request.getName() != null && !request.getName().isBlank()
                && !request.getName().trim().equalsIgnoreCase(warehouse.getName())) {
            if (warehouseRepository.existsByNameIgnoreCase(request.getName().trim())) {
                throw new BusinessException(
                        "Bu adla bir depo zaten var: " + request.getName(),
                        "WAREHOUSE_NAME_DUPLICATE");
            }
            warehouse.setName(request.getName().trim());
        }
        // Presence takipli alanlar: explicit null = temizle
        if (request.isLocationPresent()) {
            warehouse.setLocation(request.getLocation());
        }
        if (request.isResponsibleUserIdPresent()) {
            validateResponsibleUser(request.getResponsibleUserId());
            warehouse.setResponsibleUserId(request.getResponsibleUserId());
        }
        if (request.getIsActive() != null) {
            warehouse.setIsActive(request.getIsActive());
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse updated: id={}, name={}, active={}",
                saved.getId(), saved.getName(), saved.getIsActive());
        return WarehouseResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Warehouse warehouse = findEntityById(id);
        if (warehouseMovementRepository.existsByWarehouseId(id)
                || purchaseItemRepository.existsByWarehouseId(id)) {
            throw new BusinessException(
                    "Bu deponun hareket gecmisi veya bagli satin alma kalemi var, "
                            + "silinemez. Bunun yerine depoyu pasife alin.",
                    "WAREHOUSE_IN_USE");
        }
        warehouseRepository.delete(warehouse);
        log.info("Warehouse deleted: id={}, name={}", id, warehouse.getName());
    }

    private void validateResponsibleUser(UUID userId) {
        if (userId != null && !userRepository.existsById(userId)) {
            throw new BusinessException(
                    "Sorumlu olarak secilen kullanici bulunamadi.",
                    "WAREHOUSE_RESPONSIBLE_NOT_FOUND");
        }
    }

    private Warehouse findEntityById(UUID id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
    }
}
