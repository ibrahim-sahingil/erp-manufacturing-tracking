package com.uretimtakip.erp.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DeliveryNote CREATE icin request DTO.
 * note_no GONDERILMEZ - backend uretir (IRS-<yil>-<sira>).
 * status GONDERILMEZ - DRAFT baslar; gecisler update ile yapilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNoteRequest {

    /** Opsiyonel proje baglantisi (orders.id). */
    private UUID orderId;

    @NotBlank(message = "Alici adi bos olamaz")
    @Size(max = 200, message = "Alici adi en fazla 200 karakter olabilir")
    private String recipientName;

    @Size(max = 20, message = "TCKN/VKN en fazla 20 karakter olabilir")
    private String taxNumber;

    @Size(max = 100, message = "Vergi dairesi en fazla 100 karakter olabilir")
    private String taxOffice;

    private String address;

    @Size(max = 50, message = "Sehir en fazla 50 karakter olabilir")
    private String city;

    @Size(max = 50, message = "Ilce en fazla 50 karakter olabilir")
    private String district;

    @Size(max = 30, message = "Senaryo en fazla 30 karakter olabilir")
    private String scenario;

    @Size(max = 30, message = "Tur en fazla 30 karakter olabilir")
    private String noteType;

    @Size(max = 150, message = "Tasiyici en fazla 150 karakter olabilir")
    private String carrier;

    // (13. tur m4/F) Arac/yukleme bilgileri — CREATE ve UPDATE ikisinde de
    @Size(max = 20, message = "Plaka en fazla 20 karakter olabilir")
    private String vehiclePlate;

    @Size(max = 150, message = "Sofor en fazla 150 karakter olabilir")
    private String driverName;

    @Size(max = 50, message = "Konteyner no en fazla 50 karakter olabilir")
    private String containerNo;

    @Size(max = 50, message = "TIR no en fazla 50 karakter olabilir")
    private String tirNo;

    @Size(max = 100, message = "Kargo takip no en fazla 100 karakter olabilir")
    private String cargoTrackingNo;

    private LocalDate etaDate;

    // (15. tur Y2a) ceki listesi ust blogu
    @Size(max = 100, message = "Teslim kosulu en fazla 100 karakter olabilir")
    private String deliveryTerms;

    @Size(max = 100, message = "Mensei en fazla 100 karakter olabilir")
    private String originCountry;

    private LocalDate shipDate;

    private String notes;

    @Size(max = 100, message = "Olusturan en fazla 100 karakter olabilir")
    private String createdBy;
}
