package com.uretimtakip.erp.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * orders tablosunun Java karsiligi.
 *
 * DB Sema:
 *   id              uuid           (BaseEntity'den)
 *   project_name    varchar(100)   NOT NULL UNIQUE
 *   customer_name   varchar(150)
 *   customer_email  varchar(100)
 *   customer_phone  varchar(50)
 *   location        varchar(150)
 *   delivery_days   int4
 *   total_price     numeric(15,2)
 *   currency        varchar(10)    DEFAULT 'TRY'
 *   status          varchar(20)    DEFAULT 'ACTIVE'
 *   approved_by     uuid           (users.id'ye FK)
 *   notes           text
 *   created_at      timestamp      (BaseEntity'den)
 *
 * Iliskiler:
 *   OrderItem (1:N) - bir siparisin birden fazla kalemi olabilir
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "project_name", nullable = false, length = 100, unique = true)
    private String projectName;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "TRY";

    /**
     * (12. tur m1) Durum kanonu LOWERCASE + DB CHECK (orders_status_chk):
     * quote (teklif) / quote_lost (kaybedildi) / active / pending /
     * completed / cancelled. Teklif onaylaninca AYNI kayit active olur —
     * project_name baglari kopmaz.
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "approved_by")
    private UUID approvedBy;

    /** (12. tur m1) Teklif -> siparis onayinin zamani (quote->active gecisinde damgalanir). */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** (12. tur m1) Onay/teklif sureci notu. */
    @Column(name = "approval_note", columnDefinition = "TEXT")
    private String approvalNote;

    /**
     * (14. tur S4 — arkadas karari) KALICI sevkiyat zinciri SIPARIS uzerinde:
     * hazirlaniyor -> yuklendi -> sevk_edildi -> teslim_edildi (DB CHECK
     * orders_shipping_status_chk; null = sevkiyat sureci baslamadi).
     * Otomatik gecisler frontend'de (paket olustu/yuklendi/irsaliye sevk);
     * teslim_edildi ELLE isaretlenir (siparis kartindaki buton).
     */
    @Column(name = "shipping_status", length = 20)
    private String shippingStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Bir siparisin kalemleri.
     * @JsonIgnore: API cevabinda manuel donulecek (DTO uzerinden).
     * CascadeType.ALL: Order silinince OrderItem'lar da silinir.
     * orphanRemoval: Listeden cikarilan item'lar silinir.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}