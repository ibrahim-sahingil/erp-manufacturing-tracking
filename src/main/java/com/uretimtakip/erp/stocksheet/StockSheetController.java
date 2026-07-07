package com.uretimtakip.erp.stocksheet;

import com.uretimtakip.erp.common.ApiResponse;
import com.uretimtakip.erp.stocksheet.dto.StockSheetRequest;
import com.uretimtakip.erp.stocksheet.dto.StockSheetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.UUID;

/**
 * StockSheet REST API (#10 plaka MRP olcu katalogu).
 *
 * Endpoint'ler:
 *   GET    /api/stock-sheets          - tum kayitlar (?kind=SAC|PROFIL filtresi)
 *   POST   /api/stock-sheets          - yeni olcu
 *   PUT    /api/stock-sheets/{id}     - guncelle (tam govde)
 *   DELETE /api/stock-sheets/{id}     - sil
 */
@RestController
@RequestMapping("/api/stock-sheets")
@RequiredArgsConstructor
public class StockSheetController {

    private final StockSheetService stockSheetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockSheetResponse>>> list(
            @RequestParam(name = "kind", required = false) String kind) {
        return ResponseEntity.ok(ApiResponse.success(stockSheetService.listAll(kind)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockSheetResponse>> create(
            @Valid @RequestBody StockSheetRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Olcu kaydi olusturuldu", stockSheetService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockSheetResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody StockSheetRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Olcu kaydi guncellendi", stockSheetService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        stockSheetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Olcu kaydi silindi", null));
    }
}
