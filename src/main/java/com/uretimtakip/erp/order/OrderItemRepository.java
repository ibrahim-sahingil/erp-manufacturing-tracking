package com.uretimtakip.erp.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * OrderItem icin Spring Data JPA repository.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Belirli siparise ait kalemleri getir.
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Belirli siparise ait kalemleri sil.
     * (Genelde Order silindiginde cascade ile gider, ama manuel silmek icin.)
     */
    void deleteByOrderId(UUID orderId);
}