package com.uretimtakip.erp.company;

import com.uretimtakip.erp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * company_settings tablosunun Java karsiligi (15. tur Y2a — arkadas karari:
 * "bir kere girilen sabit firma ayarlari: ad/adres/logo").
 *
 * TEK SATIRLIK tablo: Gonderen (Consignor) firma bilgileri — ceki listesi /
 * packing list / irsaliye PDF'lerinin ust blogunda basilir (dnWeighList'teki
 * BOS Gonderen hucresinin kaynagi). Service getOrCreate ile satiri garanti
 * eder; ikinci satir hicbir akista olusturulmaz.
 *
 * Logo BYTEA olarak saklanir (bom_documents deseni — dosya sistemi yok);
 * API'de base64 tasinir. PDF'ler data-URL olarak gomer.
 *
 * DB Sema:
 *   id                uuid         (BaseEntity'den)
 *   name              varchar(200) NOT NULL DEFAULT '' (firma adi; bos = henuz girilmedi)
 *   address           text         NULL
 *   phone             varchar(50)  NULL
 *   email             varchar(150) NULL
 *   tax_office        varchar(100) NULL
 *   tax_number        varchar(50)  NULL
 *   logo              bytea        NULL (kucuk antet gorseli, <=500KB service guard)
 *   logo_content_type varchar(100) NULL (image/png vb.)
 *   updated_at        timestamp    NULL (service her PUT'ta yazar)
 *   created_at        timestamp    (BaseEntity'den)
 */
@Entity
@Table(name = "company_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettings extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    @Builder.Default
    private String name = "";

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "logo")
    private byte[] logo;

    @Column(name = "logo_content_type", length = 100)
    private String logoContentType;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
