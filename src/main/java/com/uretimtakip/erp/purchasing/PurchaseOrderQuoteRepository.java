package com.uretimtakip.erp.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderQuoteRepository extends JpaRepository<PurchaseOrderQuote, UUID> {

    List<PurchaseOrderQuote> findAllByOrderByCreatedAtAsc();

    List<PurchaseOrderQuote> findByPurchaseOrderIdOrderByCreatedAtAsc(UUID purchaseOrderId);
}
