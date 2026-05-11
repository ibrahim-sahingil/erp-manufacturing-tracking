package com.uretimtakip.erp.projectdate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Yeni revize kaydi olusturma istegi.
 *
 * Ornek:
 *   {
 *     "projectDateId": "uuid-...",
 *     "oldStart": "2026-05-10",
 *     "oldEnd": "2026-06-10",
 *     "newStart": "2026-05-15",
 *     "newEnd": "2026-06-15",
 *     "reason": "Musteri talebi ile 5 gun uzatildi",
 *     "revisedBy": "uuid-..."
 *   }
 *
 * NOT: oldStart/oldEnd opsiyonel cunku ilk olusturmada eski deger olmayabilir.
 *      Uygulama mantigi: ProjectDate uzerinde update yapilirken bu eski degerler doldurulabilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDateRevisionRequest {

    @NotNull(message = "Proje tarih ID bos olamaz")
    private UUID projectDateId;

    private LocalDate oldStart;

    private LocalDate oldEnd;

    @NotNull(message = "Yeni baslangic tarihi bos olamaz")
    private LocalDate newStart;

    @NotNull(message = "Yeni bitis tarihi bos olamaz")
    private LocalDate newEnd;

    @NotBlank(message = "Revize sebebi bos olamaz")
    private String reason;

    private UUID revisedBy;
}