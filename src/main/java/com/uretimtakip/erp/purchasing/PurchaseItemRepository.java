package com.uretimtakip.erp.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    List<PurchaseItem> findAllByOrderByCreatedAtAsc();

    List<PurchaseItem> findByProjectNameOrderByCreatedAtAsc(String projectName);

    boolean existsByProjectBomPartId(UUID projectBomPartId);

    /** Proje silme guard'i (K1): projeye bagli satin alma kalemi sayisi. */
    long countByProjectName(String projectName);

    boolean existsByWarehouseId(UUID warehouseId);

    List<PurchaseItem> findByPurchaseOrderId(UUID purchaseOrderId);
}
