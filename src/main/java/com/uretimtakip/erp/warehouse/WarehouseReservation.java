package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * warehouse_reservations tablosunun Java karsiligi (MIP Asama 2, 7. tur #4).
 *
 * MIP ekranindan depoya dusen REZERVASYON TALEBI: proje icin depodaki
 * malzemenin ayrilmasi istenir. Depocu fiziksel sayima gore TAM veya KISMI
 * onaylar; kismi onayda aciklama (kayip/envanter yanlis) zorunludur.
 *
 * DURUM AKISI:
 *   REQUESTED -> APPROVED  (approved_qty == requested_qty)
 *             -> PARTIAL   (0 < approved_qty < requested_qty; shortage_reason zorunlu)
 *             -> REJECTED  (approved_qty == 0; shortage_reason zorunlu)
 *             -> CANCELLED (talep sahibi onaydan once iptal etti)
 *
 * Onayda yazilanlar (tek transaction, WarehouseReservationService.approve):
 *   - approved_qty > 0        -> OUT hareketi (source_type=RESERVATION): stok
 *                                projeye mal edilir, serbest havuzdan duser.
 *   - eksik = requested-approved -> PLANNED purchase_items kaydi (satin almaya).
 *   - write_adjustment isaretli  -> kayip icin ikinci OUT (RESERVATION_ADJUST):
 *                                hayalet stok kaydi gercege cekilir.
 *
 * SILME MODELI: DELETE serbesttir (e2e temizligi + yanlis talep gideri).
 * Bagli hareketlerin reservation_id'si SET NULL olur; hareket notes'u proje +
 * malzeme snapshot'i tasidigindan defter izi kaybolmaz. Frontend yalnizca
 * REQUESTED durumunda sil/iptal butonu gosterir; onaylanmis kaydin silinmesi
 * bilincli bir gelistirici islemidir.
 *
 * project_name STRING'dir (purchase_items deseniyle tutarli; FK yok) —
 * MIP/satin alma tarafi projeye adiyla baglanir, parts'in order_id UUID
 * deseni burada KULLANILMAZ.
 *
 * DB Sema:
 *   id              uuid          (BaseEntity'den)
 *   project_name    varchar(100)  NOT NULL
 *   warehouse_id    uuid          NOT NULL (FK -> warehouses, ON DELETE RESTRICT)
 *   item_name       varchar(200)  NOT NULL (snapshot)
 *   item_code       varchar(100)  NULL (snapshot)
 *   requested_qty   numeric(15,4) NOT NULL CHECK (> 0)
 *   approved_qty    numeric(15,4) NULL (onayda dolar)
 *   unit            varchar(20)   DEFAULT 'adet'
 *   status          varchar(20)   DEFAULT 'REQUESTED' NOT NULL (CHECK)
 *   shortage_reason text          NULL (kismi/red onayda zorunlu)
 *   requested_by    varchar(150)  NULL (kullanici adi, join'siz)
 *   approved_by     varchar(150)  NULL
 *   approved_at     timestamp     NULL
 *   notes           text          NULL
 *   created_at      timestamp     (BaseEntity'den)
 * Index: status, project_name.
 */
@Entity
@Table(name = "warehouse_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseReservation extends BaseEntity {

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "requested_qty", nullable = false, precision = 15, scale = 4)
    private BigDecimal requestedQty;

    @Column(name = "approved_qty", precision = 15, scale = 4)
    private BigDecimal approvedQty;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "adet";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "REQUESTED";

    @Column(name = "shortage_reason", columnDefinition = "TEXT")
    private String shortageReason;

    @Column(name = "requested_by", length = 150)
    private String requestedBy;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
