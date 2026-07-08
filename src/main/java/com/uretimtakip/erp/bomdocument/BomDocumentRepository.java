package com.uretimtakip.erp.bomdocument;

import com.uretimtakip.erp.bomdocument.dto.BomDocumentMetaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * BomDocument icin JPA repository.
 *
 * KRITIK: listeleme sorgulari data (bytea) kolonunu ASLA cekmez —
 * constructor-projection ile yalniz meta alanlar secilir (50MB'lik
 * dosyalar liste ekraninda tasinmasin). data yalniz findById ile
 * (indirme akisinda) yuklenir.
 */
@Repository
public interface BomDocumentRepository extends JpaRepository<BomDocument, UUID> {

    @Query("select new com.uretimtakip.erp.bomdocument.dto.BomDocumentMetaResponse("
            + "d.id, d.productId, d.category, d.filename, d.contentType, "
            + "d.sizeBytes, d.uploadedBy, d.createdAt) "
            + "from BomDocument d where d.productId = :productId "
            + "order by d.createdAt desc")
    List<BomDocumentMetaResponse> findMetaByProduct(@Param("productId") UUID productId);

    /** Bir urunun dokumanlarinin parca baglari: [document_id, bom_part_id]. */
    @Query(value = "SELECT dp.document_id, dp.bom_part_id FROM bom_document_parts dp "
            + "WHERE dp.document_id IN (SELECT d.id FROM bom_documents d WHERE d.product_id = :productId)",
            nativeQuery = true)
    List<Object[]> findLinksByProduct(@Param("productId") UUID productId);
}
