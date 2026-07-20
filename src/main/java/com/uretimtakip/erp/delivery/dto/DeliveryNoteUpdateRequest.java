package com.uretimtakip.erp.delivery.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DeliveryNote PARTIAL update icin request DTO - hicbir alan zorunlu degil.
 *
 * STATUS GECISLERI (service dogrular):
 *   DRAFT   -> SHIPPED   (sevk; shipped_at damgalanir, ship_date bos ise bugun)
 *   SHIPPED -> DRAFT     (geri al; shipped_at silinir - depo hareketlerini
 *                         frontend delivery_note_id uzerinden siler)
 *   DRAFT   -> CANCELLED (iptal)
 *   CANCELLED -> DRAFT   (yeniden ac)
 *
 * Alan duzenlemeleri (alici, adres, kalemler...) yalniz DRAFT'ta yapilir;
 * SHIPPED irsaliyede sadece status gecisi kabul edilir (belge sabit kalmali).
 * note_no ve order_id IMMUTABLE.
 */
@Getter
@Setter
@NoArgsConstructor
public class DeliveryNoteUpdateRequest {

    @Size(max = 20, message = "Durum en fazla 20 karakter olabilir")
    private String status;

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

    // (13. tur m4/F) Arac/yukleme bilgileri
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

    /** Gelse de service ignore eder (IMMUTABLE) - yanlislikla degismesin. */
    private UUID orderId;
}
