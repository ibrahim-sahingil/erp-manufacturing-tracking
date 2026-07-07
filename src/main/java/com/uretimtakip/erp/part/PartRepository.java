package com.uretimtakip.erp.part;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartRepository extends JpaRepository<Part, UUID> {

    /**
     * Kod benzersizligi PROJE (order) kapsaminda ve buyuk/kucuk harf
     * duyarsizdir: ayni kod farkli projelerde serbest, ayni projede yasak.
     * DB tarafinda parts_order_code_ci_key unique index'i ile eslesir.
     */
    boolean existsByOrderIdAndCodeIgnoreCase(UUID orderId, String code);

    boolean existsByOrderIdAndCodeIgnoreCaseAndIdNot(UUID orderId, String code, UUID id);

    boolean existsByOrderIdIsNullAndCodeIgnoreCase(String code);

    boolean existsByOrderIdIsNullAndCodeIgnoreCaseAndIdNot(String code, UUID id);

    /**
     * Belirli siparise ait parcalar.
     */
    List<Part> findByOrderId(UUID orderId);

    /** Proje silme guard'i (K1): projeye bagli parca sayisi. */
    long countByOrderId(UUID orderId);

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