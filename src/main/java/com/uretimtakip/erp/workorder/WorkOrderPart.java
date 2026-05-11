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

import java.util.UUID;

/**
 * work_order_parts tablosunun Java karsiligi.
 *
 * GERCEK DB Sema:
 *   id            uuid       NOT NULL
 *   work_order_id uuid       NOT NULL
 *   part_id       uuid       NOT NULL
 *   qty           integer    DEFAULT 1
 *   created_at    timestamp
 */
@Entity
@Table(name = "work_order_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderPart extends BaseEntity {

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "qty")
    @Builder.Default
    private Integer qty = 1;
}