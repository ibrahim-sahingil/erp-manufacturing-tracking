package com.uretimtakip.erp.projectdate;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * project_dates tablosunun Java karsiligi.
 *
 * ProjectDate = Bir siparise atanan baslangic-bitis tarihi.
 *
 * GERCEK DB Sema:
 *   id          uuid       NOT NULL
 *   order_id    uuid       NOT NULL
 *   start_date  date       NOT NULL
 *   end_date    date       NOT NULL
 *   created_at  timestamp
 */
@Entity
@Table(name = "project_dates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDate extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
}