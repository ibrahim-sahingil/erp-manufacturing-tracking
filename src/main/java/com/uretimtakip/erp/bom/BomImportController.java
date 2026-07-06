package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomImportParseResponse;
import com.uretimtakip.erp.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel'den urun agaci iceri aktarma API'si.
 *
 * Endpoint'ler:
 *   POST /api/bom-import/parse    - multipart .xlsx -> parse sonucu JSON
 *                                   (dosya saklanmaz; olusturma frontend'te
 *                                   secim sonrasi mevcut endpoint'lerle yapilir)
 *   GET  /api/bom-import/template - doldurulabilir ornek sablon (.xlsx indirme)
 */
@RestController
@RequestMapping("/api/bom-import")
@RequiredArgsConstructor
public class BomImportController {

    private final BomImportService bomImportService;

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BomImportParseResponse>> parse(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                ApiResponse.success("Dosya cozumlendi", bomImportService.parse(file)));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        byte[] bytes = bomImportService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"urun-agaci-sablon.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
