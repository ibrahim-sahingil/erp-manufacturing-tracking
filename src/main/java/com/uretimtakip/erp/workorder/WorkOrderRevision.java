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
 * work_order_revisions tablosunun Java karsiligi.
 *
 * Bu tablo, is emrinde yapilan ALAN BAZLI degisiklikleri kaydediyor.
 * Audit trail (denetim izi) icin kullaniliyor.
 *
 * Ornek kayit:
 *   "Is emri X'in 'status' alani 'planned' yapildi 'in_progress'.
 *    Sebep: 'Uretime baslandi'. Revize eden: Ali Yilmaz."
 *
 * GERCEK DB Sema:
 *   id              uuid       NOT NULL
 *   work_order_id   uuid       NOT NULL
 *   field_changed   varchar    NOT NULL  ← hangi alan degisti
 *   old_value       text       YES        ← eski deger (string)
 *   new_value       text       YES        ← yeni deger (string)
 *   reason          text       NOT NULL   ← sebep zorunlu
 *   revised_by      uuid       YES        ← revize eden user
 *   created_at      timestamp
 */
@Entity
@Table(name = "work_order_revisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderRevision extends BaseEntity {

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "field_changed", nullable = false, length = 100)
    private String fieldChanged;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "revised_by")
    private UUID revisedBy;
}