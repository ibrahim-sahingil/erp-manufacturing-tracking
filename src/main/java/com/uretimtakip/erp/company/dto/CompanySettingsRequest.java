package com.uretimtakip.erp.company.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CompanySettings PUT DTO (15. tur Y2a). PARTIAL: null alan DOKUNMAZ,
 * bos string temizler (tek satirlik ayar — CREATE yok, GET satiri garanti eder).
 *
 * logoBase64: null = dokunma, "" = logoyu SIL, dolu = yeni logo
 * (data-URL onen KIRPILMIS saf base64; content type ayri alanda gelir).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettingsRequest {

    @Size(max = 200, message = "Firma adi en fazla 200 karakter olabilir")
    private String name;

    private String address;

    @Size(max = 50, message = "Telefon en fazla 50 karakter olabilir")
    private String phone;

    @Size(max = 150, message = "E-posta en fazla 150 karakter olabilir")
    private String email;

    @Size(max = 100, message = "Vergi dairesi en fazla 100 karakter olabilir")
    private String taxOffice;

    @Size(max = 50, message = "Vergi no en fazla 50 karakter olabilir")
    private String taxNumber;

    /** ~500KB ikili sinirin base64 karsiligi service'te denetlenir. */
    private String logoBase64;

    @Size(max = 100, message = "Logo tipi en fazla 100 karakter olabilir")
    private String logoContentType;
}
