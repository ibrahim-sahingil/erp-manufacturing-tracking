package com.uretimtakip.erp.projectbom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ProjectBom icin JPA repository.
 *
 * METODLAR:
 *   - findAllByOrderByCreatedAtDesc(): yeni->eski sirali
 *   - findByProjectNameOrderByCreatedAtDesc(): bir projedeki tum BOM atamalari
 *   - findByBomProductId(): bir urune yapilan tum atamalar
 *   - existsByProjectNameAndBomProductId(): UNIQUE kontrolu (create oncesi)
 *   - countByBomProductId(): BomProduct.delete kontrolu icin (Faz 1 TODO aktif!)
 */
@Repository
public interface ProjectBomRepository extends JpaRepository<ProjectBom, UUID> {

    List<ProjectBom> findAllByOrderByCreatedAtDesc();

    List<ProjectBom> findByProjectNameOrderByCreatedAtDesc(String projectName);

    List<ProjectBom> findByBomProductId(UUID bomProductId);

    Optional<ProjectBom> findByProjectNameAndBomProductId(
            String projectName, UUID bomProductId);

    boolean existsByProjectNameAndBomProductId(String projectName, UUID bomProductId);

    long countByBomProductId(UUID bomProductId);

    /** Proje silme guard'i (K1): projeye bagli urun agaci baglantisi sayisi. */
    long countByProjectName(String projectName);

    /**
     * Proje yeniden adlandirma senkronu (K2): BOM baglantilari projeye
     * STRING ile bagli oldugundan orders.project_name degisince burasi da
     * ayni transaction'da guncellenir (OrderService.update cagirir).
     */
    @Modifying
    @Query("UPDATE ProjectBom b SET b.projectName = :newName WHERE b.projectName = :oldName")
    int renameProjectName(@Param("oldName") String oldName, @Param("newName") String newName);
}