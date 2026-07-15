package com.uretimtakip.erp.orderdocument;

import com.uretimtakip.erp.orderdocument.dto.OrderDocumentMetaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderDocumentRepository extends JpaRepository<OrderDocument, UUID> {

    /** META listesi — data KOLONU CEKILMEZ (50MB'lik icerik listede tasinmasin). */
    @Query("""
            select new com.uretimtakip.erp.orderdocument.dto.OrderDocumentMetaResponse(
                d.id, d.orderId, d.category, d.filename, d.contentType,
                d.sizeBytes, d.uploadedBy, d.createdAt)
            from OrderDocument d
            where d.orderId = :orderId
            order by d.createdAt desc
            """)
    List<OrderDocumentMetaResponse> findMetaByOrder(@Param("orderId") UUID orderId);
}
