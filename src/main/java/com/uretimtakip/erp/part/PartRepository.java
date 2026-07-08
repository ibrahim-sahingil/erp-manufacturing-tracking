package com.uretimtakip.erp.part;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartRepository extends JpaRepository<Part, UUID> {

    /**
     * (U4) QR uretim girisi sayaclarini ATOMIK artir — yaris (lost update)
     * onlemek icin. Java oku-degistir-yaz yerine tek SQL UPDATE: sag taraf
     * eski satir degerlerini kullandigindan es zamanli iki giris de dogru
     * toplanir. qty_pending, yeni done/reject'e gore turetilir (increment
     * degil). total_qty NULL ise 0 sayilir.
     */
    @Modifying
    @Query(value =
            "UPDATE parts SET " +
            "qty_done = qty_done + :d, " +
            "qty_reject = qty_reject + :r, " +
            "qty_pending = GREATEST(0, COALESCE(total_qty,0) - (qty_done + :d) - (qty_reject + :r)) " +
            "WHERE id = :id",
            nativeQuery = true)
    int incrementQty(@Param("id") UUID id, @Param("d") int d, @Param("r") int r);

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

    /** Parca silme guard'i (O1): alt parca sayisi. */
    long countByParentPartId(UUID parentPartId);

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