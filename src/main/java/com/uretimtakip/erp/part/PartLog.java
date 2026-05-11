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
 * part_logs tablosunun Java karsiligi.
 *
 * Her uretim hareketinin tarihcesi.
 * QR kod tarandiginda buraya kayit eklenir + parts tablosundaki sayilar guncellenir.
 *
 * DB Sema:
 *   id          uuid          (BaseEntity'den)
 *   part_id     uuid          NOT NULL (parts.id'ye FK)
 *   user_id     uuid          NOT NULL (users.id'ye FK)
 *   qty_done    int4          DEFAULT 0
 *   qty_pending int4          DEFAULT 0
 *   qty_reject  int4          DEFAULT 0
 *   note        text
 *   created_at  timestamp     (BaseEntity'den)
 */
@Entity
@Table(name = "part_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartLog extends BaseEntity {

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "qty_done")
    @Builder.Default
    private Integer qtyDone = 0;

    @Column(name = "qty_pending")
    @Builder.Default
    private Integer qtyPending = 0;

    @Column(name = "qty_reject")
    @Builder.Default
    private Integer qtyReject = 0;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}