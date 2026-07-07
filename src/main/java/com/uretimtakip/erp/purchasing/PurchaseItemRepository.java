package com.uretimtakip.erp.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    List<PurchaseItem> findAllByOrderByCreatedAtAsc();

    List<PurchaseItem> findByProjectNameOrderByCreatedAtAsc(String projectName);

    boolean existsByProjectBomPartId(UUID projectBomPartId);

    /** Proje silme guard'i (K1): projeye bagli satin alma kalemi sayisi. */
    long countByProjectName(String projectName);

    /**
     * Proje yeniden adlandirma senkronu (K2): satin alma kalemleri projeye
     * STRING ile bagli oldugundan orders.project_name degisince burasi da
     * ayni transaction'da guncellenir (OrderService.update cagirir).
     */
    @Modifying
    @Query("UPDATE PurchaseItem p SET p.projectName = :newName WHERE p.projectName = :oldName")
    int renameProjectName(@Param("oldName") String oldName, @Param("newName") String newName);

    boolean existsByWarehouseId(UUID warehouseId);

    List<PurchaseItem> findByPurchaseOrderId(UUID purchaseOrderId);
}
