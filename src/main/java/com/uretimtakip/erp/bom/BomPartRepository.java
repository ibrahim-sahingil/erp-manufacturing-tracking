package com.uretimtakip.erp.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * BomPart icin JPA repository.
 *
 * METODLAR:
 *   1. findByProductId / findByProductIdOrderBy... - urune ait parcalar
 *   2. findByParentId - bir parent'in alt parcalari
 *   3. existsByParentId - child var mi? (delete kontrolu)
 *   4. countByProductId - urune bagli parca sayisi (BomProduct.delete kontrolu)
 *   5. countByOperationCode (NATIVE) - jsonb icinde code arama (BomOperation.delete)
 *   6. countByOperationId (NATIVE) - jsonb icinde operationId arama (BomOperation.delete)
 *
 * JSONB NATIVE QUERY ACIKLAMASI:
 *   PostgreSQL'in @> ("contains") operatoru ile jsonb array'inde arama:
 *     operations @> '[{"code":"WLD"}]'::jsonb
 *   anlami: operations array'i icinde {"code":"WLD"} icereyen herhangi
 *   bir obje var mi? (case-sensitive, tam eslesme)
 *
 *   :param ile parametre gecerken jsonb cast'i icin
 *   CAST(:param AS jsonb) kullanmak gerekiyor.
 */
@Repository
public interface BomPartRepository extends JpaRepository<BomPart, UUID> {

    // ============ ORDERED LIST ============

    List<BomPart> findAllByOrderByLevelAscSortOrderAscNameAsc();

    List<BomPart> findByProductIdOrderByLevelAscSortOrderAscNameAsc(UUID productId);

    List<BomPart> findByParentIdOrderBySortOrderAscNameAsc(UUID parentId);

    // ============ EXISTS / COUNT ============

    boolean existsByParentId(UUID parentId);

    /** Ayni urun agacinda harf duyarsiz kod kontrolu. */
    boolean existsByProductIdAndCodeIgnoreCase(UUID productId, String code);

    boolean existsByProductIdAndCodeIgnoreCaseAndIdNot(UUID productId, String code, UUID id);

    long countByProductId(UUID productId);

    long countByParentId(UUID parentId);

    // ============ JSONB NATIVE QUERIES ============

    /**
     * operations jsonb array'i icinde "code" anahtari verilen deger
     * olan bir obje var mi? Kac parcada gecer?
     *
     * Ornek: countByOperationCode("WLD")
     *   -> operations icinde [..., {"code":"WLD", ...}, ...] gecen
     *      tum bom_parts kayitlarinin sayisi
     */
    @Query(
            value = "SELECT COUNT(*) FROM bom_parts " +
                    "WHERE operations @> CAST(:codeJson AS jsonb)",
            nativeQuery = true
    )
    long countByOperationCodeNative(@Param("codeJson") String codeJson);

    /**
     * operations jsonb array'i icinde "operationId" anahtari verilen
     * UUID olan bir obje var mi?
     */
    @Query(
            value = "SELECT COUNT(*) FROM bom_parts " +
                    "WHERE operations @> CAST(:idJson AS jsonb)",
            nativeQuery = true
    )
    long countByOperationIdNative(@Param("idJson") String idJson);

    /**
     * Helper method - JPA default method ile JSON string'ini kapsulle.
     * Service buradan cagirir, native query detayini bilmesine gerek yok.
     */
    default long countByOperationCode(String code) {
        // Escape: code icinde " gecerse JSON bozulur. Basit kontrol.
        if (code == null || code.contains("\"")) {
            return 0;
        }
        String json = "[{\"code\":\"" + code + "\"}]";
        return countByOperationCodeNative(json);
    }

    default long countByOperationId(UUID operationId) {
        if (operationId == null) {
            return 0;
        }
        String json = "[{\"operationId\":\"" + operationId + "\"}]";
        return countByOperationIdNative(json);
    }
}