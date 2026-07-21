package com.uretimtakip.erp.projectdocument.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * (16. tur M3.2) Meta guncelleme: kategori ve/veya parca baglari.
 * PARTIAL: null alan DOKUNMAZ; partIds=[] TUM baglari kaldirir.
 * Icerik guncellenemez — yeni surum = yeni yukleme (bom_documents kurali).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDocumentUpdateRequest {
    private String category;
    private List<UUID> partIds;
}
