package com.uretimtakip.erp.projectbom;

import org.springframework.data.jpa.repository.JpaRepository;
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
}