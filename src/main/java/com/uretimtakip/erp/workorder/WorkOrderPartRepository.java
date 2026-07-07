package com.uretimtakip.erp.workorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkOrderPartRepository extends JpaRepository<WorkOrderPart, UUID> {

    List<WorkOrderPart> findByWorkOrderId(UUID workOrderId);

    List<WorkOrderPart> findByPartId(UUID partId);

    Optional<WorkOrderPart> findByWorkOrderIdAndPartId(UUID workOrderId, UUID partId);

    boolean existsByWorkOrderIdAndPartId(UUID workOrderId, UUID partId);

    /** Parca silme guard'i (O1): parcanin bagli oldugu is emri sayisi. */
    long countByPartId(UUID partId);

    void deleteByWorkOrderId(UUID workOrderId);
}