package com.uretimtakip.erp.bomdocument.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * BomDocument META guncelleme (PARTIAL — dosya icerigi degistirilemez,
 * yeni surum = yeni yukleme). part_ids gonderilirse liste KOMPLE degisir.
 */
@Getter
@Setter
@NoArgsConstructor
public class BomDocumentUpdateRequest {

    @Size(max = 255, message = "Dosya adi en fazla 255 karakter olabilir")
    private String filename;

    @jakarta.validation.constraints.Pattern(
            regexp = "^(URETIM|ARGE)?$",
            message = "Kategori URETIM veya ARGE olmali")
    private String category;

    private List<UUID> partIds;
}
