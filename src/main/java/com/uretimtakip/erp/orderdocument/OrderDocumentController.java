package com.uretimtakip.erp.orderdocument;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.orderdocument.dto.OrderDocumentMetaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * (12. tur m1) Siparis/teklif dosyalari REST API — BomDocumentController deseni.
 *
 *   GET    /api/order-documents?order_id=X   - siparisin dosya META'lari (data YOK)
 *   POST   /api/order-documents              - multipart (file, order_id, category, uploaded_by)
 *   GET    /api/order-documents/{id}/download- icerik (attachment)
 *   DELETE /api/order-documents/{id}         - sil
 *
 * Yetki (SecurityConfig): GET dahil TUM ucler orders_quotes/orders_edit/orders/
 * developer ister — teklif dosyalari fiyat icerir, salt-okuma da kisitli.
 */
@RestController
@RequestMapping("/api/order-documents")
@RequiredArgsConstructor
public class OrderDocumentController {

    private final OrderDocumentService orderDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDocumentMetaResponse>>> listByOrder(
            @RequestParam(name = "order_id") UUID orderId) {
        return ResponseEntity.ok(
                ApiResponse.success(orderDocumentService.listByOrder(orderId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OrderDocumentMetaResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "order_id") UUID orderId,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "uploaded_by", required = false) String uploadedBy) {
        OrderDocumentMetaResponse created =
                orderDocumentService.upload(file, orderId, category, uploadedBy);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dosya yuklendi", created));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        OrderDocument doc = orderDocumentService.getForDownload(id);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        orderDocumentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Dosya silindi", null));
    }
}
