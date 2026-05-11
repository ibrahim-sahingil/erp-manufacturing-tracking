package com.uretimtakip.erp.workorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkOrderRevisionRepository extends JpaRepository<WorkOrderRevision, UUID> {

    List<WorkOrderRevision> findByWorkOrderIdOrderByCreatedAtDesc(UUID workOrderId);

    List<WorkOrderRevision> findByRevisedByOrderByCreatedAtDesc(UUID userId);

    List<WorkOrderRevision> findByWorkOrderIdAndFieldChangedOrderByCreatedAtDesc(
            UUID workOrderId, String fieldChanged);
}