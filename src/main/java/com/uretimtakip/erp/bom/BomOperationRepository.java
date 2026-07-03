package com.uretimtakip.erp.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * BomOperation icin JPA repository.
 *
 * - findAllByOrderBySortOrderAscNameAsc(): sort_order'a gore sirali liste
 * - existsByCode: UNIQUE kontrolu icin (yeni kayit oncesinde)
 * - findByCode: code ile arama
 */
@Repository
public interface BomOperationRepository extends JpaRepository<BomOperation, UUID> {

    List<BomOperation> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByCode(String code);

    Optional<BomOperation> findByCode(String code);
}