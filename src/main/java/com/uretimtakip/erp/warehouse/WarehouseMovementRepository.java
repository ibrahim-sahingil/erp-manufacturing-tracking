package com.uretimtakip.erp.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WarehouseMovementRepository extends JpaRepository<WarehouseMovement, UUID> {

    List<WarehouseMovement> findAllByOrderByCreatedAtDesc();

    List<WarehouseMovement> findByWarehouseIdOrderByCreatedAtDesc(UUID warehouseId);

    boolean existsByWarehouseId(UUID warehouseId);
}
