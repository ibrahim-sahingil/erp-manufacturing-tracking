package com.uretimtakip.erp.bomdocument;

import com.uretimtakip.erp.bomdocument.dto.BomDocumentMetaResponse;
import com.uretimtakip.erp.bomdocument.dto.BomDocumentUpdateRequest;
import com.uretimtakip.erp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BomDocument REST API (teknik resimler — 5. tur #7).
 *
 * Endpoint'ler:
 *   GET    /api/bom-documents?product_id=X   - urunun dokuman META'lari (part_ids dahil, data YOK)
 *   POST   /api/bom-documents                - multipart yukleme (file, product_id, category, part_ids, uploaded_by)
 *   GET    /api/bom-documents/{id}/download  - dosya icerigi (attachment)
 *   PUT    /api/bom-documents/{id}           - META guncelle (kategori / ad / parca baglari)
 *   DELETE /api/bom-documents/{id}           - sil
 */
@RestController
@RequestMapping("/api/bom-documents")
@RequiredArgsConstructor
public class BomDocumentController {

    private final BomDocumentService bomDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BomDocumentMetaResponse>>> listByProduct(
            @RequestParam(name = "product_id") UUID productId) {
        return ResponseEntity.ok(
                ApiResponse.success(bomDocumentService.listByProduct(productId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BomDocumentMetaResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "product_id") UUID productId,
            @RequestParam(name = "category") String category,
            @RequestParam(name = "part_ids", required = false) String partIdsCsv,
            @RequestParam(name = "uploaded_by", required = false) String uploadedBy) {
        List<UUID> partIds = parseIds(partIdsCsv);
        BomDocumentMetaResponse created =
                bomDocumentService.upload(file, productId, category, partIds, uploadedBy);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dosya yuklendi", created));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        BomDocument doc = bomDocumentService.getForDownload(id);
        // Turkce/ozel karakterli dosya adlari icin RFC 5987 (filename*)
        String encoded = URLEncoder.encode(doc.getFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String ascii = doc.getFilename().replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
        String ct = doc.getContentType() != null
                ? doc.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(ct))
                .body(doc.getData());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BomDocumentMetaResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BomDocumentUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Dosya guncellendi", bomDocumentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        bomDocumentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Dosya silindi", null));
    }

    private List<UUID> parseIds(String csv) {
        List<UUID> ids = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return ids;
        }
        for (String s : csv.split(",")) {
            if (!s.isBlank()) {
                ids.add(UUID.fromString(s.trim()));
            }
        }
        return ids;
    }
}
