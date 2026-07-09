package com.uretimtakip.erp.projectbom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Kod benzersizligi AYNI PARENT kapsaminda kontrol edilir (4. tur #1):
    // ayni kod agacin farkli dallarinda serbestce tekrar kullanilabilir.
    boolean existsByProjectBomIdAndParentCustomIdAndCustomCodeIgnoreCase(
            UUID projectBomId, UUID parentCustomId, String customCode);

    boolean existsByProjectBomIdAndParentCustomIdIsNullAndCustomCodeIgnoreCase(
            UUID projectBomId, String customCode);

    boolean existsByProjectBomIdAndParentCustomIdAndCustomCodeIgnoreCaseAndIdNot(
            UUID projectBomId, UUID parentCustomId, String customCode, UUID id);

    boolean existsByProjectBomIdAndParentCustomIdIsNullAndCustomCodeIgnoreCaseAndIdNot(
            UUID projectBomId, String customCode, UUID id);

    /** Ayni parent altinda (kardesler arasinda) harf duyarsiz kod var mi? */
    default boolean existsSiblingCode(UUID projectBomId, UUID parentCustomId,
                                      String customCode) {
        return parentCustomId == null
                ? existsByProjectBomIdAndParentCustomIdIsNullAndCustomCodeIgnoreCase(
                        projectBomId, customCode)
                : existsByProjectBomIdAndParentCustomIdAndCustomCodeIgnoreCase(
                        projectBomId, parentCustomId, customCode);
    }

    default boolean existsSiblingCodeExcept(UUID projectBomId, UUID parentCustomId,
                                            String customCode, UUID exceptId) {
        return parentCustomId == null
                ? existsByProjectBomIdAndParentCustomIdIsNullAndCustomCodeIgnoreCaseAndIdNot(
                        projectBomId, customCode, exceptId)
                : existsByProjectBomIdAndParentCustomIdAndCustomCodeIgnoreCaseAndIdNot(
                        projectBomId, parentCustomId, customCode, exceptId);
    }

    long countByParentCustomId(UUID parentCustomId);

    long countByBomPartId(UUID bomPartId);

    long countByDeptId(UUID deptId);

    long countByProjectBomId(UUID projectBomId);

    /**
     * (7. tur #3) Verilen islem kodunu operations jsonb'sinde barindiran TUM
     * proje parcalari. Islem tanimi duzenlenince yayinlanmis proje agaclarindaki
     * kodlar da guncellenir (kullanicinin sectigi kapsam: tum agaclar).
     */
    @Query(
            value = "SELECT * FROM project_bom_parts " +
                    "WHERE operations @> CAST(:codeJson AS jsonb)",
            nativeQuery = true
    )
    List<ProjectBomPart> findByOperationCodeNative(@Param("codeJson") String codeJson);

    default List<ProjectBomPart> findByOperationCode(String code) {
        if (code == null || code.contains("\"")) {
            return List.of();
        }
        return findByOperationCodeNative("[{\"code\":\"" + code + "\"}]");
    }
}