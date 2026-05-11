package com.uretimtakip.erp.projectdate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectDateRepository extends JpaRepository<ProjectDate, UUID> {

    List<ProjectDate> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<ProjectDate> findAllByOrderByCreatedAtDesc();
}