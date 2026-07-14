package com.uretimtakip.erp.workorder;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * work_orders tablosunun Java karsiligi.
 *
 * GERCEK DB Sema:
 *   id               uuid           NOT NULL  (BaseEntity'den)
 *   order_id         uuid           NOT NULL
 *   department_id    uuid           YES
 *   workspace_id     uuid           YES
 *   assigned_user_id uuid           YES
 *   start_datetime   timestamp      NOT NULL  ← DIKKAT
 *   end_datetime     timestamp      YES
 *   status           varchar        DEFAULT 'planned'  ← lowercase!
 *   notes            text           YES
 *   code             varchar(20)    YES, UNIQUE  ← İE-YYYY-NNN, servis üretir (2026-07-14)
 *   created_at       timestamp      (BaseEntity'den)
 */
@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "planned";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** İnsan-okur iş emri numarası (İE-2026-011). İstemciden alınmaz, create'te servis üretir. */
    @Column(name = "code", length = 20, unique = true)
    private String code;
}