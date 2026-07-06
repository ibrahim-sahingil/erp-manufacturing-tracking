package com.uretimtakip.erp.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Excel parse sonucunun tamami.
 *
 * rows: dosyadaki her veri satiri (hatali olanlar dahil - error alani dolu).
 * fileErrors: satira baglanamayan genel hatalar (bos dosya, baslik yok vb.).
 *
 * Dosya SAKLANMAZ - sadece parse edilip JSON donulur; kullanici onizlemede
 * sectiklerini mevcut create endpoint'leriyle tek tek olusturur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomImportParseResponse {

    private List<BomImportRowResponse> rows;
    private List<String> fileErrors;
    private int okCount;
    private int errorCount;
}
