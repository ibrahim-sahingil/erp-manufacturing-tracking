package com.uretimtakip.erp.projectdocument;

import com.uretimtakip.erp.projectdocument.dto.ProjectDocumentMetaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {

    /** META listesi — data KOLONU CEKILMEZ (50MB'lik icerik listede tasinmasin). */
    @Query("""
            select new com.uretimtakip.erp.projectdocument.dto.ProjectDocumentMetaResponse(
                d.id, d.projectName, d.category, d.filename, d.contentType,
                d.sizeBytes, d.uploadedBy, d.createdAt)
            from ProjectDocument d
            where d.projectName = :projectName
            order by d.createdAt desc
            """)
    List<ProjectDocumentMetaResponse> findMetaByProject(@Param("projectName") String projectName);

    /** Bir projenin dokumanlarinin parca baglari: [document_id, project_bom_part_id]. */
    @Query(value = "SELECT dp.document_id, dp.project_bom_part_id FROM project_document_parts dp "
            + "WHERE dp.document_id IN (SELECT d.id FROM project_documents d WHERE d.project_name = :projectName)",
            nativeQuery = true)
    List<Object[]> findLinksByProject(@Param("projectName") String projectName);

    /**
     * Verilen pbp id'lerinden KACI bu projenin agaclarina ait? (DOC_PART_PROJECT_MISMATCH
     * dogrulamasi — sayi != istenen sayi ise baska projenin parcasi var demektir.)
     */
    @Query(value = "SELECT count(*) FROM project_bom_parts pbp "
            + "JOIN project_bom pb ON pbp.project_bom_id = pb.id "
            + "WHERE pb.project_name = :projectName AND pbp.id IN (:ids)",
            nativeQuery = true)
    long countPartsInProject(@Param("projectName") String projectName,
                             @Param("ids") List<UUID> ids);
}
