package com.uretimtakip.erp.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * BomProduct icin JPA repository.
 *
 * - findAllByOrderByNameAsc(): alfabetik sirali liste
 *
 * NOT: code UNIQUE degil, o yuzden existsByCode gibi bir kontrol
 * koymadik (cakisma sorun degil).
 */
@Repository
public interface BomProductRepository extends JpaRepository<BomProduct, UUID> {

    List<BomProduct> findAllByOrderByNameAsc();
}