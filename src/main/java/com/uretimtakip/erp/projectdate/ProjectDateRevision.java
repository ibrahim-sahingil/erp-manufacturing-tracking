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
 * project_date_revisions tablosunun Java karsiligi.
 *
 * ProjectDateRevision = Proje tarihindeki revizyonlarin tarihce kaydi.
 * "Bu projenin tarihi 1 hafta uzatildi" gibi.
 *
 * GERCEK DB Sema:
 *   id               uuid       NOT NULL
 *   project_date_id  uuid       NOT NULL
 *   old_start        date       YES
 *   old_end          date       YES
 *   new_start        date       NOT NULL
 *   new_end          date       NOT NULL
 *   reason           text       NOT NULL  ← sebep zorunlu
 *   revised_by       uuid       YES        ← revize eden user
 *   created_at       timestamp
 */
@Entity
@Table(name = "project_date_revisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDateRevision extends BaseEntity {

    @Column(name = "project_date_id", nullable = false)
    private UUID projectDateId;

    @Column(name = "old_start")
    private LocalDate oldStart;

    @Column(name = "old_end")
    private LocalDate oldEnd;

    @Column(name = "new_start", nullable = false)
    private LocalDate newStart;

    @Column(name = "new_end", nullable = false)
    private LocalDate newEnd;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "revised_by")
    private UUID revisedBy;
}