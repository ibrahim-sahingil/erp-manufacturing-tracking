package com.uretimtakip.erp.stocksheet;

import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.stocksheet.dto.StockSheetRequest;
import com.uretimtakip.erp.stocksheet.dto.StockSheetResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * StockSheet is mantigi (#10 plaka MRP olcu katalogu).
 * Silme serbest — plana donusen kalemler kendi metin snapshot'ini tasir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSheetService {

    private final StockSheetRepository stockSheetRepository;

    @Transactional(readOnly = true)
    public List<StockSheetResponse> listAll(String kind) {
        List<StockSheet> list = (kind != null && !kind.isBlank())
                ? stockSheetRepository.findByKindOrderByNameAsc(kind)
                : stockSheetRepository.findAllByOrderByKindAscNameAsc();
        return list.stream()
                .map(StockSheetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public StockSheetResponse create(StockSheetRequest request) {
        StockSheet sheet = StockSheet.builder()
                .kind(request.getKind())
                .name(request.getName().trim())
                .material(request.getMaterial())
                .widthMm(request.getWidthMm())
                .heightMm(request.getHeightMm())
                .thicknessMm(request.getThicknessMm())
                .lengthMm(request.getLengthMm())
                .notes(request.getNotes())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        StockSheet saved = stockSheetRepository.save(sheet);
        log.info("StockSheet created: id={}, kind={}, name={}", saved.getId(), saved.getKind(), saved.getName());
        return StockSheetResponse.fromEntity(saved);
    }

    @Transactional
    public StockSheetResponse update(UUID id, StockSheetRequest request) {
        StockSheet sheet = stockSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockSheet", "id", id));
        sheet.setKind(request.getKind());
        sheet.setName(request.getName().trim());
        sheet.setMaterial(request.getMaterial());
        sheet.setWidthMm(request.getWidthMm());
        sheet.setHeightMm(request.getHeightMm());
        sheet.setThicknessMm(request.getThicknessMm());
        sheet.setLengthMm(request.getLengthMm());
        sheet.setNotes(request.getNotes());
        if (request.getIsActive() != null) sheet.setIsActive(request.getIsActive());
        StockSheet saved = stockSheetRepository.save(sheet);
        log.info("StockSheet updated: id={}, name={}", saved.getId(), saved.getName());
        return StockSheetResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        StockSheet sheet = stockSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockSheet", "id", id));
        stockSheetRepository.delete(sheet);
        log.info("StockSheet deleted: id={}, name={}", id, sheet.getName());
    }
}
