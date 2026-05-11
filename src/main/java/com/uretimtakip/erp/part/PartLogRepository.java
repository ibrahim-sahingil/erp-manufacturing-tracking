package com.uretimtakip.erp.part;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartLogRepository extends JpaRepository<PartLog, UUID> {

    /**
     * Tum loglari yeniden eskiye sirali getir.
     */
    List<PartLog> findAllByOrderByCreatedAtDesc();

    /**
     * Belirli parcaya ait tum loglari yeniden eskiye getir.
     */
    List<PartLog> findByPartIdOrderByCreatedAtDesc(UUID partId);

    /**
     * Belirli kullanicinin tum loglari.
     */
    List<PartLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Belirli parca + kullanici (analiz icin).
     */
    List<PartLog> findByPartIdAndUserIdOrderByCreatedAtDesc(UUID partId, UUID userId);
}