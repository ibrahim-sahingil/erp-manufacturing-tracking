package com.uretimtakip.erp.userpin;

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
 * user_pins tablosunun Java karsiligi.
 *
 * Kullanicinin dashboard'da sabitledigi (pin) ogeler:
 * proje veya siparis favorileri.
 *
 * DB Sema:
 *   id          uuid          (BaseEntity'den)
 *   user_id     uuid          NOT NULL (FK -> users, CASCADE)
 *   pin_type    varchar(50)   NOT NULL ('project' | 'order')
 *   pin_key     varchar(100)  NOT NULL (proje adi veya siparis id'si)
 *   created_at  timestamp     (BaseEntity'den)
 *   UNIQUE(user_id, pin_type, pin_key)
 */
@Entity
@Table(name = "user_pins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPin extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pin_type", nullable = false, length = 50)
    private String pinType;

    @Column(name = "pin_key", nullable = false, length = 100)
    private String pinKey;
}
