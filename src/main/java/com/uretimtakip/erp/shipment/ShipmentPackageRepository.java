package com.uretimtakip.erp.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentPackageRepository extends JpaRepository<ShipmentPackage, UUID> {

    List<ShipmentPackage> findAllByOrderByCreatedAtDesc();

    List<ShipmentPackage> findByProjectNameOrderByCreatedAtDesc(String projectName);

    List<ShipmentPackage> findByDeliveryNoteId(UUID deliveryNoteId);

    long countByPackageNoStartingWith(String prefix);

    boolean existsByPackageNo(String packageNo);
}
