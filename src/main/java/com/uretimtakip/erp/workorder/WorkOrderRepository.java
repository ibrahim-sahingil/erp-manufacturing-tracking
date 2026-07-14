package com.uretimtakip.erp.workorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    List<WorkOrder> findByOrderId(UUID orderId);

    /** Proje silme guard'i (K1): projeye bagli is emri sayisi. */
    long countByOrderId(UUID orderId);

    List<WorkOrder> findByWorkspaceId(UUID workspaceId);

    List<WorkOrder> findByDepartmentId(UUID departmentId);

    List<WorkOrder> findByAssignedUserId(UUID userId);

    List<WorkOrder> findByStatus(String status);

    List<WorkOrder> findAllByOrderByCreatedAtDesc();

    /** İE-YYYY-NNN üretimi: yılın en büyük kodu (sözlük sırası = sayısal sıra, NNN sıfır dolgulu). */
    java.util.Optional<WorkOrder> findTopByCodeStartingWithOrderByCodeDesc(String prefix);
}