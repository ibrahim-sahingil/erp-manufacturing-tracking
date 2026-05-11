package com.uretimtakip.erp.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * order_items tablosunun Java karsiligi.
 *
 * DB Sema:
 *   id           uuid          (BaseEntity'den)
 *   order_id     uuid          NOT NULL (orders.id'ye FK)
 *   item_name    varchar(150)  NOT NULL
 *   description  text
 *   quantity     int4          DEFAULT 1
 *   created_at   timestamp     (BaseEntity'den)
 *
 * Bir Order'in N adet OrderItem'i olabilir.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    /**
     * Hangi siparise ait? Lazy loading ile gerektiginde yuklenir.
     * @JsonIgnore: Sonsuz dongu olmasin diye API'da yok sayilir.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 1;
}