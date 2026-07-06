package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.warehouse.dto.WarehouseMovementRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseMovementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WarehouseMovement is mantigi.
 *
 * UPDATE YOK: hareket defteri duzeltilmez; yanlis kayit silinip
 * yeniden girilir (DELETE + POST). Stok her zaman hareketlerden
 * turetildigi icin ayri bir stok senkronu gerekmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseMovementService {

    /** DB'deki warehouse_movements_type_check ile ayni liste. */
    private static final Set<String> VALID_TYPES = Set.of("IN", "OUT");

    /** DB'deki warehouse_movements_source_check ile ayni liste. */
    private static final Set<String> VALID_SOURCES = Set.of(
            "MANUAL", "PURCHASE_TRANSFER", "GOODS_RECEIPT");

    private final WarehouseMovementRepository warehouseMovementRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public List<WarehouseMovementResponse> listAll() {
        return warehouseMovementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(WarehouseMovementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WarehouseMovementResponse> listByWarehouse(UUID warehouseId) {
        return warehouseMovementRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .stream()
                .map(WarehouseMovementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseMovementResponse create(WarehouseMovementRequest request) {
        if (!warehouseRepository.existsById(request.getWarehouseId())) {
            throw new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId());
        }
        if (!VALID_TYPES.contains(request.getMovementType())) {
            throw new BusinessException(
                    "Gecersiz hareket tipi: " + request.getMovementType()
                            + ". Gecerli degerler: " + VALID_TYPES,
                    "WAREHOUSE_MOVEMENT_INVALID_TYPE");
        }
        String sourceType = request.getSourceType() != null && !request.getSourceType().isBlank()
                ? request.getSourceType() : "MANUAL";
        if (!VALID_SOURCES.contains(sourceType)) {
            throw new BusinessException(
                    "Gecersiz kaynak tipi: " + sourceType
                            + ". Gecerli degerler: " + VALID_SOURCES,
                    "WAREHOUSE_MOVEMENT_INVALID_SOURCE");
        }

        WarehouseMovement movement = WarehouseMovement.builder()
                .warehouseId(request.getWarehouseId())
                .purchaseItemId(request.getPurchaseItemId())
                .itemName(request.getItemName())
                .itemCode(request.getItemCode())
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .sourceType(sourceType)
                .performedBy(request.getPerformedBy())
                .notes(request.getNotes())
                .build();

        WarehouseMovement saved = warehouseMovementRepository.save(movement);
        log.info("WarehouseMovement created: id={}, warehouse={}, type={}, item={}, qty={}",
                saved.getId(), saved.getWarehouseId(), saved.getMovementType(),
                saved.getItemName(), saved.getQuantity());
        return WarehouseMovementResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        WarehouseMovement movement = warehouseMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseMovement", "id", id));
        warehouseMovementRepository.delete(movement);
        log.info("WarehouseMovement deleted: id={}, item={}", id, movement.getItemName());
    }
}
