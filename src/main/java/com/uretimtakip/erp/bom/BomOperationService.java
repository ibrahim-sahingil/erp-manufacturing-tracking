package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomOperationRequest;
import com.uretimtakip.erp.bom.dto.BomOperationResponse;
import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uretimtakip.erp.projectbom.ProjectBomPart;
import com.uretimtakip.erp.projectbom.ProjectBomPartRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BomOperation is mantigi.
 *
 * - code UNIQUE: create/update'te kontrol edilir, cakisma varsa BusinessException
 * - delete: bom_parts.operations jsonb icinde bu code geciyor mu kontrol edilir,
 *           geciyorsa silinemez (BusinessException).
 *
 * NOT: bom_parts.operations jsonb List<Map<String,Object>> formatinda. Map
 * icinde "code" veya "operationCode" anahtarinin degerine bakariz. Frontend
 * tarafinda hangi anahtar kullanildigi netlestiginde burayi guncelleyebilirsin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomOperationService {

    private final BomOperationRepository bomOperationRepository;
    private final BomPartRepository bomPartRepository;
    private final ProjectBomPartRepository projectBomPartRepository;

    @Transactional(readOnly = true)
    public List<BomOperationResponse> listAll() {
        return bomOperationRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(BomOperationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BomOperationResponse getById(UUID id) {
        BomOperation op = bomOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomOperation", "id", id));
        return BomOperationResponse.fromEntity(op);
    }

    @Transactional
    public BomOperationResponse create(BomOperationRequest request) {
        // code UNIQUE kontrolu
        if (bomOperationRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                    "Bu kod zaten kullaniliyor: " + request.getCode(),
                    "DUPLICATE_CODE"
            );
        }

        BomOperation op = BomOperation.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .departmentName(trimToNull(request.getDepartmentName()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        BomOperation saved = bomOperationRepository.save(op);
        log.info("BomOperation created: id={}, code={}", saved.getId(), saved.getCode());
        return BomOperationResponse.fromEntity(saved);
    }

    /**
     * (7. tur #3) Bu islem tanimi kac yerde kullaniliyor? Frontend, kod
     * degistirmeden ONCE "12 sablon parcasi + 5 proje parcasinin kodu
     * degisecek" onizlemesini bununla gosterir.
     */
    @Transactional(readOnly = true)
    public OperationUsage usage(UUID id) {
        BomOperation op = bomOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomOperation", "id", id));
        return new OperationUsage(
                bomPartRepository.findByOperationCode(op.getCode()).size(),
                projectBomPartRepository.findByOperationCode(op.getCode()).size()
        );
    }

    /** Kullanim sayilari (onizleme icin). */
    public record OperationUsage(long bomParts, long projectBomParts) {}

    /**
     * (7. tur #3) Islem tanimini gunceller. KOD DEGISIRSE, bu islemi tasiyan
     * TUM parcalarin (hem sablon agaclari hem yayinlanmis proje agaclari)
     * operations dizisi ve KODU otomatik guncellenir.
     *
     * Kod yeniden insasi: islem kodlari parca kodunun sonuna sirayla eklenir
     * (GP-001 + WLD + PNT -> GP-001WLDPNT). Bu yuzden eski soneklerin hepsi
     * SONDAN sokulur, sonra yeni kodlar sirayla geri eklenir. Boylece ortadaki
     * bir islemin kodu degisse bile kalinti kalmaz (bkz. frontend
     * rebuildCodeWithOps — ayni mantik).
     *
     * Tumu tek @Transactional icinde: yarida kalirsa hicbiri yazilmaz.
     */
    @Transactional
    public BomOperationResponse update(UUID id, BomOperationRequest request) {
        BomOperation op = bomOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomOperation", "id", id));

        // Code degisiyor ve yeni code baska bir kayitta varsa hata
        if (!op.getCode().equals(request.getCode())
                && bomOperationRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                    "Bu kod zaten kullaniliyor: " + request.getCode(),
                    "DUPLICATE_CODE"
            );
        }

        final String oldCode = op.getCode();
        final String newCode = request.getCode();
        final String newName = request.getName();
        final boolean codeChanged = !oldCode.equals(newCode);

        if (codeChanged) {
            cascadeCodeChange(oldCode, newCode, newName);
        } else {
            // Kod ayni ama AD degismis olabilir: parcalardaki etiket de guncellensin
            cascadeNameChange(oldCode, newName);
        }

        op.setName(newName);
        op.setCode(newCode);
        op.setDescription(request.getDescription());
        op.setDepartmentName(trimToNull(request.getDepartmentName()));
        if (request.getSortOrder() != null) {
            op.setSortOrder(request.getSortOrder());
        }

        BomOperation saved = bomOperationRepository.save(op);
        log.info("BomOperation updated: id={}, code {} -> {}", saved.getId(), oldCode, newCode);
        return BomOperationResponse.fromEntity(saved);
    }

    /** Kod degisti: hem operations dizisi hem parca kodu guncellenir. */
    private void cascadeCodeChange(String oldCode, String newCode, String newName) {
        for (BomPart p : bomPartRepository.findByOperationCode(oldCode)) {
            List<Map<String, Object>> oldOps = safeOps(p.getOperations());
            List<Map<String, Object>> newOps = replaceOpCode(oldOps, oldCode, newCode, newName);
            p.setCode(rebuildCode(p.getCode(), oldOps, newOps));
            p.setOperations(newOps);
            bomPartRepository.save(p);
        }
        for (ProjectBomPart p : projectBomPartRepository.findByOperationCode(oldCode)) {
            List<Map<String, Object>> oldOps = safeOps(p.getOperations());
            List<Map<String, Object>> newOps = replaceOpCode(oldOps, oldCode, newCode, newName);
            // DIKKAT: custom_code bir OVERRIDE'dir; bos ise etkin kod bagli sablon
            // parcasindan turetilir (effectiveCode). Bos kodun uzerine yazarsak
            // parcanin taban kodu YOK OLUR ("XZPNT" gibi kokunu kaybetmis kod).
            // Bu durumda sablon zaten yukarida guncellendi; sadece etiketler yenilenir.
            if (p.getCustomCode() != null && !p.getCustomCode().isBlank()) {
                p.setCustomCode(rebuildCode(p.getCustomCode(), oldOps, newOps));
            }
            p.setOperations(newOps);
            projectBomPartRepository.save(p);
        }
    }

    /** Yalnizca ad degisti: parca kodu aynen kalir, etiket adi guncellenir. */
    private void cascadeNameChange(String code, String newName) {
        for (BomPart p : bomPartRepository.findByOperationCode(code)) {
            p.setOperations(replaceOpCode(safeOps(p.getOperations()), code, code, newName));
            bomPartRepository.save(p);
        }
        for (ProjectBomPart p : projectBomPartRepository.findByOperationCode(code)) {
            p.setOperations(replaceOpCode(safeOps(p.getOperations()), code, code, newName));
            projectBomPartRepository.save(p);
        }
    }

    private List<Map<String, Object>> safeOps(List<Map<String, Object>> ops) {
        return ops == null ? new ArrayList<>() : new ArrayList<>(ops);
    }

    /** operations dizisindeki eslesen kaydin code/name alanlarini yeniler. */
    private List<Map<String, Object>> replaceOpCode(List<Map<String, Object>> ops,
                                                    String oldCode, String newCode,
                                                    String newName) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> o : ops) {
            Map<String, Object> copy = new LinkedHashMap<>(o);
            if (oldCode.equals(copy.get("code"))) {
                copy.put("code", newCode);
                if (newName != null) {
                    copy.put("name", newName);
                }
            }
            out.add(copy);
        }
        return out;
    }

    /**
     * Eski islem soneklerini SONDAN sokup yeni kodlari sirayla geri ekler.
     * Frontend'deki rebuildCodeWithOps ile AYNI mantik — ikisi senkron kalmali.
     */
    private String rebuildCode(String code,
                               List<Map<String, Object>> oldOps,
                               List<Map<String, Object>> newOps) {
        String base = code == null ? "" : code;
        for (int i = oldOps.size() - 1; i >= 0; i--) {
            Object c = oldOps.get(i).get("code");
            if (c instanceof String s && !s.isEmpty() && base.endsWith(s)) {
                base = base.substring(0, base.length() - s.length());
            }
        }
        StringBuilder sb = new StringBuilder(base);
        for (Map<String, Object> o : newOps) {
            Object c = o.get("code");
            if (c instanceof String s) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Transactional
    public void delete(UUID id) {
        BomOperation op = bomOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BomOperation", "id", id));

        // Kullanimda mi? bom_parts.operations jsonb icinde bu code geciyor mu?
        long usageCount = bomPartRepository.countByOperationCode(op.getCode());
        if (usageCount > 0) {
            throw new BusinessException(
                    "Bu operasyon " + usageCount + " parcada kullaniliyor, silinemez",
                    "OPERATION_IN_USE"
            );
        }

        bomOperationRepository.delete(op);
        log.info("BomOperation deleted: id={}, code={}", id, op.getCode());
    }
}