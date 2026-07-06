package com.uretimtakip.erp.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, UUID> {

    List<DeliveryNote> findAllByOrderByCreatedAtDesc();

    /** Yil bazli numara uretimi icin: "IRS-2026-" ile baslayanlarin sayisi. */
    long countByNoteNoStartingWith(String prefix);

    boolean existsByNoteNo(String noteNo);
}
