package com.uretimtakip.erp.carrier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Carrier CREATE icin request DTO.
 *
 * Ornek (combobox "listede yoksa ekle" akisi cogunlukla sadece ad yollar):
 *   { "name": "Demir Celik A.S." }
 *   { "name": "...", "phone": "0212 ...", "tax_office": "...", "tax_number": "..." }
 *
 * is_active verilmezse true baslar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierRequest {

    @NotBlank(message = "Nakliye firmasi adi bos olamaz")
    @Size(max = 150, message = "Nakliye firmasi adi en fazla 150 karakter olabilir")
    private String name;

    @Size(max = 150, message = "Yetkili adi en fazla 150 karakter olabilir")
    private String contactPerson;

    @Size(max = 50, message = "Telefon en fazla 50 karakter olabilir")
    private String phone;

    @Size(max = 150, message = "E-posta en fazla 150 karakter olabilir")
    private String email;

    @Size(max = 300, message = "Adres en fazla 300 karakter olabilir")
    private String address;

    @Size(max = 100, message = "Vergi dairesi en fazla 100 karakter olabilir")
    private String taxOffice;

    @Size(max = 50, message = "Vergi numarasi en fazla 50 karakter olabilir")
    private String taxNumber;

    private String notes;

    private Boolean isActive;
}
