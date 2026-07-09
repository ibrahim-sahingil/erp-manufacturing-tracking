package com.uretimtakip.erp.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseReservationRepository extends JpaRepository<WarehouseReservation, UUID> {

    List<WarehouseReservation> findAllByOrderByCreatedAtDesc();

    List<WarehouseReservation> findByStatusOrderByCreatedAtDesc(String status);

    /** Proje silme guard'i (K1): projeye bagli rezervasyon sayisi. */
    long countByProjectName(String projectName);

    /**
     * (K2) Proje adi degisince STRING ile bagli rezervasyonlar da
     * ayni transaction'da guncellenir (OrderService.update cagirir).
     */
    @Modifying
    @Query("UPDATE WarehouseReservation r SET r.projectName = :newName WHERE r.projectName = :oldName")
    int renameProjectName(@Param("oldName") String oldName, @Param("newName") String newName);
}
