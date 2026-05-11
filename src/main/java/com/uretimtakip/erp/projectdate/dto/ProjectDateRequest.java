package com.uretimtakip.erp.projectdate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Yeni proje tarih kaydi olusturma/guncelleme istegi.
 *
 * Ornek JSON:
 *   {
 *     "orderId": "uuid-...",
 *     "startDate": "2026-05-10",
 *     "endDate": "2026-06-10"
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDateRequest {

    @NotNull(message = "Siparis ID bos olamaz")
    private UUID orderId;

    @NotNull(message = "Baslangic tarihi bos olamaz")
    private LocalDate startDate;

    @NotNull(message = "Bitis tarihi bos olamaz")
    private LocalDate endDate;
}