package com.uretimtakip.erp.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    List<Warehouse> findAllByOrderByNameAsc();

    List<Warehouse> findByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
