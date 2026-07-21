package com.uretimtakip.erp.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    // (16. tur M1b) SIP-<yil>-<sira> ureticisi (nextNoteNo deseni)
    long countByCodeStartingWith(String prefix);

    boolean existsByCode(String code);
}
