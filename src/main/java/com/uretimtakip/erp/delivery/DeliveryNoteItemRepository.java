package com.uretimtakip.erp.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryNoteItemRepository extends JpaRepository<DeliveryNoteItem, UUID> {

    List<DeliveryNoteItem> findByDeliveryNoteIdOrderByCreatedAtAsc(UUID deliveryNoteId);

    List<DeliveryNoteItem> findAllByOrderByCreatedAtAsc();

    long countByDeliveryNoteId(UUID deliveryNoteId);
}
