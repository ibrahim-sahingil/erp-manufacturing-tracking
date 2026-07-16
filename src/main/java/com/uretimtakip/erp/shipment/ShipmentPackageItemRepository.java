package com.uretimtakip.erp.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentPackageItemRepository extends JpaRepository<ShipmentPackageItem, UUID> {

    List<ShipmentPackageItem> findAllByOrderByCreatedAtAsc();

    List<ShipmentPackageItem> findByPackageIdOrderByCreatedAtAsc(UUID packageId);

    long countByPackageId(UUID packageId);
}
