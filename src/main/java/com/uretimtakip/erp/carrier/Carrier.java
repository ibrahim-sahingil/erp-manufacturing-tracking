package com.uretimtakip.erp.carrier;

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
 * carriers tablosunun Java karsiligi (15. tur Y2 — arkadas istegi:
 * "hangi sirket uzerinden bu nakliyatlarin gerceklestigi bilgileri
 * alinsin. Sabit olarak sirket detaylari kayit edilebilir").
 *
 * Nakliye firmasi kartoteki (suppliers birebir klonu): irsaliyedeki
 * "Nakliye Firmasi" alani bu listeden onerilir ("listede yoksa ekle"
 * deseniyle, ensureCarrier). delivery_notes.carrier SERBEST METIN kalir
 * (snapshot) - buraya FK YOKTUR; kartotek yalnizca secim listesi +
 * iletisim/adres bilgisi kaynagi.
 *
 * Silme kurali yok (FK bagi olmadigi icin serbest); yanlislikla buyuyen
 * listeyi toparlamak icin is_active=false ile pasife alinabilir.
 *
 * DB Sema:
 *   id             uuid         (BaseEntity'den)
 *   name           varchar(150) NOT NULL (service'te harf duyarsiz benzersiz)
 *   contact_person varchar(150) NULL
 *   phone          varchar(50)  NULL
 *   email          varchar(150) NULL
 *   address        varchar(300) NULL
 *   tax_office     varchar(100) NULL (vergi dairesi)
 *   tax_number     varchar(50)  NULL (vergi no / TCKN)
 *   notes          text         NULL
 *   is_active      boolean      DEFAULT true NOT NULL
 *   created_at     timestamp    (BaseEntity'den)
 */
@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrier extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
