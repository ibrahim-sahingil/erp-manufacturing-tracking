package com.uretimtakip.erp.part;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartRepository extends JpaRepository<Part, UUID> {

    /**
     * Kod ile bul (UNIQUE).
     */
    Optional<Part> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Belirli siparise ait parcalar.
     */
    List<Part> findByOrderId(UUID orderId);

    /**
     * Belirli departmana atanmis parcalar.
     */
    List<Part> findByDepartmentId(UUID departmentId);

    /**
     * Belirli durumdaki parcalar.
     */
    List<Part> findByStatus(String status);

    /**
     * Tum parcalari yeniden eskiye sirali.
     */
    List<Part> findAllByOrderByCreatedAtDesc();
}