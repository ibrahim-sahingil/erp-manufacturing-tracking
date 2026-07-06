package com.uretimtakip.erp.supplier.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Supplier PARTIAL update icin request DTO.
 *
 * warehouses'taki desenle ayni: sadece gonderilen alanlar degisir.
 *
 * PRESENCE TAKIPLI ALANLAR (explicit null = temizle):
 *   contact_person, phone, email, address, tax_office, tax_number, notes
 *
 * Tipik kullanim:
 *   {"is_active": false}   - tedarikciyi pasife al
 *   {"phone": null}        - telefonu sil
 */
@Getter
@Setter
@NoArgsConstructor
public class SupplierUpdateRequest {

    @Size(max = 150, message = "Tedarikci adi en fazla 150 karakter olabilir")
    private String name;

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 150, message = "Yetkili adi en fazla 150 karakter olabilir")
    private String contactPerson;
    @Setter(lombok.AccessLevel.NONE)
    private boolean contactPersonPresent;
    public void setContactPerson(String v) { this.contactPerson = v; this.contactPersonPresent = true; }

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 50, message = "Telefon en fazla 50 karakter olabilir")
    private String phone;
    @Setter(lombok.AccessLevel.NONE)
    private boolean phonePresent;
    public void setPhone(String v) { this.phone = v; this.phonePresent = true; }

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 150, message = "E-posta en fazla 150 karakter olabilir")
    private String email;
    @Setter(lombok.AccessLevel.NONE)
    private boolean emailPresent;
    public void setEmail(String v) { this.email = v; this.emailPresent = true; }

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 300, message = "Adres en fazla 300 karakter olabilir")
    private String address;
    @Setter(lombok.AccessLevel.NONE)
    private boolean addressPresent;
    public void setAddress(String v) { this.address = v; this.addressPresent = true; }

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 100, message = "Vergi dairesi en fazla 100 karakter olabilir")
    private String taxOffice;
    @Setter(lombok.AccessLevel.NONE)
    private boolean taxOfficePresent;
    public void setTaxOffice(String v) { this.taxOffice = v; this.taxOfficePresent = true; }

    @Setter(lombok.AccessLevel.NONE)
    @Size(max = 50, message = "Vergi numarasi en fazla 50 karakter olabilir")
    private String taxNumber;
    @Setter(lombok.AccessLevel.NONE)
    private boolean taxNumberPresent;
    public void setTaxNumber(String v) { this.taxNumber = v; this.taxNumberPresent = true; }

    @Setter(lombok.AccessLevel.NONE)
    private String notes;
    @Setter(lombok.AccessLevel.NONE)
    private boolean notesPresent;
    public void setNotes(String v) { this.notes = v; this.notesPresent = true; }

    private Boolean isActive;
}
