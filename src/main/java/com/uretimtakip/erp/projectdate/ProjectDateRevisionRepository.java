package com.uretimtakip.erp.projectdate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectDateRevisionRepository extends JpaRepository<ProjectDateRevision, UUID> {

    List<ProjectDateRevision> findByProjectDateIdOrderByCreatedAtDesc(UUID projectDateId);

    List<ProjectDateRevision> findByRevisedByOrderByCreatedAtDesc(UUID userId);
}