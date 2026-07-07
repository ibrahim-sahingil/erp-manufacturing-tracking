package com.uretimtakip.erp.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order icin Spring Data JPA repository.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Proje adi ile bul (UNIQUE).
     */
    Optional<Order> findByProjectName(String projectName);

    /**
     * Proje adi var mi?
     */
    boolean existsByProjectName(String projectName);

    /**
     * Belirli durumdaki siparisleri getir.
     * Ornek: findByStatus("ACTIVE")
     */
    List<Order> findByStatus(String status);

    /**
     * Tum siparisleri yeniden eskiye sirali getir.
     */
    List<Order> findAllByOrderByCreatedAtDesc();

    /**
     * Belirli musteriye ait siparisler.
     */
    List<Order> findByCustomerName(String customerName);

    /** Kullanici silme guard'i (U1): bu kullaniciyi onaylayan olarak tutan siparis sayisi. */
    long countByApprovedBy(UUID approvedBy);
}