package com.uretimtakip.erp.projectdocument;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.projectdocument.dto.ProjectDocumentMetaResponse;
import com.uretimtakip.erp.projectdocument.dto.ProjectDocumentUpdateRequest;
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
 * (16. tur M3.2) Proje teknik resimleri REST API — OrderDocumentController deseni.
 *
 *   GET    /api/project-documents?project=X    - projenin dosya META'lari (data YOK, part_ids VAR)
 *   POST   /api/project-documents              - multipart (file, project_name, category, part_ids CSV, uploaded_by)
 *   PUT    /api/project-documents/{id}         - meta guncelle (kategori / parca baglari)
 *   GET    /api/project-documents/{id}/download- icerik (attachment)
 *   DELETE /api/project-documents/{id}         - sil (bagli parca varsa PDOC_LINKED)
 *
 * Yetki (SecurityConfig writeRule): yazma bom/docs; okuma tum girisli
 * kullanicilara acik (satin almaci/imalatci parca ustunden indirir — M3.2c).
 */
@RestController
@RequestMapping("/api/project-documents")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService projectDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDocumentMetaResponse>>> listByProject(
            @RequestParam(name = "project") String project) {
        return ResponseEntity.ok(
                ApiResponse.success(projectDocumentService.listByProject(project)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectDocumentMetaResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "project_name") String projectName,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "part_ids", required = false) String partIdsCsv,
            @RequestParam(name = "uploaded_by", required = false) String uploadedBy) {
        List<UUID> partIds = new ArrayList<>();
        if (partIdsCsv != null && !partIdsCsv.isBlank()) {
            for (String s : partIdsCsv.split(",")) {
                if (!s.isBlank()) partIds.add(UUID.fromString(s.trim()));
            }
        }
        ProjectDocumentMetaResponse created =
                projectDocumentService.upload(file, projectName, category, partIds, uploadedBy);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dosya yuklendi", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDocumentMetaResponse>> update(
            @PathVariable UUID id,
            @RequestBody ProjectDocumentUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Dosya guncellendi",
                        projectDocumentService.update(id, request)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        ProjectDocument doc = projectDocumentService.getForDownload(id);
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
        projectDocumentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Dosya silindi", null));
    }
}
