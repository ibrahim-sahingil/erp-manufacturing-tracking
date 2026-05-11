package com.uretimtakip.erp.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * users tablosunun Java karsiligi.
 *
 * DB Sema:
 *   id            uuid           (BaseEntity'den)
 *   full_name     varchar(100)   NOT NULL
 *   department    varchar(50)
 *   role          varchar(20)    NOT NULL
 *   is_active     boolean        DEFAULT true
 *   created_at    timestamp      (BaseEntity'den)
 *   username      varchar        UNIQUE
 *   password_hash varchar(255)
 *   pin_code      varchar
 *   permissions   jsonb          DEFAULT '[]'
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "username", unique = true)
    private String username;

    /**
     * BCrypt hash'lenmis sifre.
     * @JsonIgnore: API cevaplarinda ASLA donmemeli (guvenlik).
     */
    @JsonIgnore
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "pin_code")
    private String pinCode;

    /**
     * Yetki listesi (PostgreSQL jsonb -> Java List<String>).
     * Ornek: ["all"] veya ["orders.read", "parts.write"]
     *
     * @JdbcTypeCode(SqlTypes.JSON): Hibernate 6.4+ native jsonb destegi.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> permissions = new ArrayList<>();
}