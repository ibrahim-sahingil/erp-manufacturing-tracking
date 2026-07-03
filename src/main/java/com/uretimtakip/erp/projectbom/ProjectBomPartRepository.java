package com.uretimtakip.erp.projectbom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ProjectBomPart icin JPA repository.
 *
 * METODLAR:
 *   - findByProjectBomIdOrderBy...: bir ProjectBom'a ait parcalar (sirali)
 *   - findByParentCustomId: hierarchy alt parcalar
 *   - existsByParentCustomId: child var mi? (delete kontrolu)
 *   - countByParentCustomId: child sayisi (defensive delete)
 *   - countByBomPartId: BomPart.delete kontrolu icin (Faz 1 TODO aktif!)
 *   - countByDeptId: Department silinmeden once kullanim kontrolu (ileride)
 */
@Repository
public interface ProjectBomPartRepository extends JpaRepository<ProjectBomPart, UUID> {

    List<ProjectBomPart> findByProjectBomIdOrderByLevelAscSortOrderAscIdAsc(
            UUID projectBomId);

    List<ProjectBomPart> findByParentCustomIdOrderBySortOrderAscIdAsc(
            UUID parentCustomId);

    boolean existsByParentCustomId(UUID parentCustomId);

    long countByParentCustomId(UUID parentCustomId);

    long countByBomPartId(UUID bomPartId);

    long countByDeptId(UUID deptId);

    long countByProjectBomId(UUID projectBomId);
}