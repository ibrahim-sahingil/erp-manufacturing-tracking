package com.uretimtakip.erp.workspace;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * workspaces tablosunun Java karsiligi.
 *
 * Workspace = Calisma alani (kaynak hucresi, montaj alani, vs.)
 *
 * DB Sema:
 *   id          uuid           (BaseEntity'den)
 *   name        varchar(150)   NOT NULL
 *   type        varchar(50)    DEFAULT 'area'
 *   description text
 *   sort_order  int4           DEFAULT 1
 *   created_at  timestamp      (BaseEntity'den)
 */
@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "type", length = 50)
    @Builder.Default
    private String type = "area";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 1;
}