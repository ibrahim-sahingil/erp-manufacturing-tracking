package com.uretimtakip.erp.part;

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
 * parts tablosunun Java karsiligi.
 *
 * Uretilecek/uretilmis parcalar.
 *
 * DB Sema:
 *   id            uuid          (BaseEntity'den)
 *   order_id      uuid          (siparise bagli)
 *   department_id uuid          (departmana bagli)
 *   name          varchar(150)  NOT NULL
 *   code          varchar(100)  NOT NULL UNIQUE
 *   drawing_no    varchar(100)
 *   material      varchar(100)
 *   total_qty     int4          DEFAULT 1
 *   status        varchar(20)   DEFAULT 'PENDING'
 *   description   text
 *   qty_done      int4          DEFAULT 0  (uretildi)
 *   qty_pending   int4          DEFAULT 0  (bekliyor)
 *   qty_reject    int4          DEFAULT 0  (red)
 *   created_at    timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Part extends BaseEntity {

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;

    @Column(name = "drawing_no", length = 100)
    private String drawingNo;

    @Column(name = "material", length = 100)
    private String material;

    @Column(name = "total_qty")
    @Builder.Default
    private Integer totalQty = 1;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "qty_done", nullable = false)
    @Builder.Default
    private Integer qtyDone = 0;

    @Column(name = "qty_pending", nullable = false)
    @Builder.Default
    private Integer qtyPending = 0;

    @Column(name = "qty_reject", nullable = false)
    @Builder.Default
    private Integer qtyReject = 0;
}