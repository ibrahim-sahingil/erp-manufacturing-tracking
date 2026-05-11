package com.uretimtakip.erp.department;

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
 * departments tablosunun Java karsiligi.
 *
 * DB Sema:
 *   id          uuid           (BaseEntity'den)
 *   order_id    uuid           NULL (siparise bagli olabilir veya genel)
 *   name        varchar(100)   NOT NULL
 *   sort_order  int4           DEFAULT 1
 *   created_at  timestamp      (BaseEntity'den)
 *
 * Not: order_id su an basit tutuyoruz. Order entity'si yazilinca
 *      buradan iliski (relationship) ekleyecegiz.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 1;
}