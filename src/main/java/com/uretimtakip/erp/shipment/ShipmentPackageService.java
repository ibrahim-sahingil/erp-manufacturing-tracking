package com.uretimtakip.erp.shipment;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageRequest;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageResponse;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ShipmentPackage is mantigi (13. tur madde 4 — Sevkiyat paketleme).
 *
 * PACKAGE_NO: "PKT-<yil>-<4 haneli sira>" create'te uretilir; asil mukerrer
 * garantisi DB UNIQUE (DeliveryNoteService.nextNoteNo deseni).
 *
 * DURUM WHITELIST'i (komsu gecisler iki yonlu — geri alma akislari icin):
 *   OPEN <-> CLOSED <-> LOADED <-> SHIPPED
 *   - CLOSED'a gecis: packed_by zorunlu (istekte veya kayitta), packed_at=now
 *   - CLOSED -> OPEN (yeniden ac): packed_by/packed_at temizlenir
 *   - LOADED'a gecis: delivery_note_id dolu olmali (ayni istekte verilebilir)
 *   - Olcu/ad duzenlemesi yalniz OPEN ve CLOSED'da (LOADED/SHIPPED sabit belge)
 *
 * Icerik satirlari ShipmentPackageItemService'te; orada paket OPEN guard'i var.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentPackageService {

    private static final Set<String> VALID_STATUSES =
            Set.of("OPEN", "CLOSED", "LOADED", "SHIPPED");

    private final ShipmentPackageRepository shipmentPackageRepository;
    private final ShipmentPackageItemRepository shipmentPackageItemRepository;

    @Transactional(readOnly = true)
    public List<ShipmentPackageResponse> listAll() {
        return shipmentPackageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ShipmentPackageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShipmentPackageResponse getById(UUID id) {
        return ShipmentPackageResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public ShipmentPackageResponse create(ShipmentPackageRequest request) {
        ShipmentPackage pkg = ShipmentPackage.builder()
                .packageNo(nextPackageNo())
                .projectName(request.getProjectName())
                .name(blankToNull(request.getName()))
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                // (14. tur S2+S3) net agirlik + tip + depo
                .netWeightKg(request.getNetWeightKg())
                .packageType(request.getPackageType() != null && !request.getPackageType().isBlank()
                        ? request.getPackageType() : "PACKAGE")
                .warehouseId(request.getWarehouseId())
                .notes(blankToNull(request.getNotes()))
                .createdBy(blankToNull(request.getCreatedBy()))
                .build();

        ShipmentPackage saved = shipmentPackageRepository.save(pkg);
        log.info("ShipmentPackage created: id={}, packageNo={}, project={}",
                saved.getId(), saved.getPackageNo(), saved.getProjectName());
        return ShipmentPackageResponse.fromEntity(saved);
    }

    @Transactional
    public ShipmentPackageResponse update(UUID id, ShipmentPackageUpdateRequest request) {
        ShipmentPackage pkg = findEntityById(id);

        // Irsaliye bagi (presence takipli; LOADED gecisinden ONCE islenir ki
        // ayni istekte delivery_note_id + status:LOADED birlikte gelebilsin)
        if (request.isDeliveryNoteIdPresent()) {
            if (request.getDeliveryNoteId() == null
                    && ("LOADED".equals(pkg.getStatus()) || "SHIPPED".equals(pkg.getStatus()))
                    && !"CLOSED".equals(request.getStatus())) {
                throw new BusinessException(
                        "Yuklu/sevk edilmis paketin irsaliye bagi ancak durum geri alinirken kaldirilabilir.",
                        "SHIPMENT_PACKAGE_NOTE_LINKED");
            }
            pkg.setDeliveryNoteId(request.getDeliveryNoteId());
        }

        // packed_by (CLOSED gecisiyle ayni istekte gelebilir)
        if (request.getPackedBy() != null && !request.getPackedBy().isBlank()) {
            pkg.setPackedBy(request.getPackedBy());
        }

        // Durum gecisi
        if (request.getStatus() != null && !request.getStatus().isBlank()
                && !request.getStatus().equals(pkg.getStatus())) {
            applyStatusChange(pkg, request.getStatus());
        }

        // Alan duzenlemeleri: yalniz OPEN/CLOSED (yuklu/sevkli paket sabit belge)
        boolean fieldsTouched = request.getName() != null
                || request.getLengthCm() != null || request.getWidthCm() != null
                || request.getHeightCm() != null || request.getWeightKg() != null
                || request.getNetWeightKg() != null || request.getPackageType() != null
                || request.isWarehouseIdPresent()
                || request.getNotes() != null;
        if (fieldsTouched
                && !("OPEN".equals(pkg.getStatus()) || "CLOSED".equals(pkg.getStatus()))) {
            throw new BusinessException(
                    "Araca yuklenmis/sevk edilmis paket duzenlenemez. Once durumu geri alin.",
                    "SHIPMENT_PACKAGE_LOCKED");
        }
        if (request.getName() != null) pkg.setName(blankToNull(request.getName()));
        if (request.getLengthCm() != null) pkg.setLengthCm(request.getLengthCm());
        if (request.getWidthCm() != null) pkg.setWidthCm(request.getWidthCm());
        if (request.getHeightCm() != null) pkg.setHeightCm(request.getHeightCm());
        if (request.getWeightKg() != null) pkg.setWeightKg(request.getWeightKg());
        if (request.getNetWeightKg() != null) pkg.setNetWeightKg(request.getNetWeightKg());
        if (request.getPackageType() != null && !request.getPackageType().isBlank()) {
            pkg.setPackageType(request.getPackageType());
        }
        // (14. tur S2) depo — presence takipli (explicit null = depodan cikar)
        if (request.isWarehouseIdPresent()) pkg.setWarehouseId(request.getWarehouseId());
        if (request.getNotes() != null) pkg.setNotes(blankToNull(request.getNotes()));

        ShipmentPackage saved = shipmentPackageRepository.save(pkg);
        log.info("ShipmentPackage updated: id={}, packageNo={}, status={}",
                saved.getId(), saved.getPackageNo(), saved.getStatus());
        return ShipmentPackageResponse.fromEntity(saved);
    }

    private void applyStatusChange(ShipmentPackage pkg, String target) {
        if (!VALID_STATUSES.contains(target)) {
            throw new BusinessException("Gecersiz durum: " + target
                    + ". Gecerli degerler: " + VALID_STATUSES, "SHIPMENT_PACKAGE_BAD_STATUS");
        }
        String from = pkg.getStatus();
        boolean allowed =
                ("OPEN".equals(from) && "CLOSED".equals(target))
                        || ("CLOSED".equals(from) && ("OPEN".equals(target) || "LOADED".equals(target)))
                        || ("LOADED".equals(from) && ("CLOSED".equals(target) || "SHIPPED".equals(target)))
                        || ("SHIPPED".equals(from) && "LOADED".equals(target));
        if (!allowed) {
            throw new BusinessException(
                    "Gecersiz durum gecisi: " + from + " -> " + target,
                    "SHIPMENT_PACKAGE_BAD_TRANSITION");
        }
        if ("CLOSED".equals(target) && "OPEN".equals(from)) {
            if (pkg.getPackedBy() == null || pkg.getPackedBy().isBlank()) {
                throw new BusinessException(
                        "Paket kapatilirken paketleyen (packed_by) zorunlu.",
                        "SHIPMENT_PACKAGE_PACKER_REQUIRED");
            }
            pkg.setPackedAt(LocalDateTime.now());
        }
        if ("OPEN".equals(target)) {
            pkg.setPackedAt(null);
            pkg.setPackedBy(null);
        }
        if ("LOADED".equals(target) && "CLOSED".equals(from)
                && pkg.getDeliveryNoteId() == null) {
            throw new BusinessException(
                    "Paket araca yuklenirken irsaliye bagi (delivery_note_id) zorunlu.",
                    "SHIPMENT_PACKAGE_NOTE_REQUIRED");
        }
        pkg.setStatus(target);
    }

    @Transactional
    public void delete(UUID id) {
        ShipmentPackage pkg = findEntityById(id);
        if ("LOADED".equals(pkg.getStatus()) || "SHIPPED".equals(pkg.getStatus())) {
            throw new BusinessException(
                    "Araca yuklenmis/sevk edilmis paket silinemez. Once durumu geri alin.",
                    "SHIPMENT_PACKAGE_LOCKED");
        }
        // Icerik satirlari DB CASCADE ile silinir
        shipmentPackageRepository.delete(pkg);
        log.info("ShipmentPackage deleted: id={}, packageNo={}", id, pkg.getPackageNo());
    }

    /** PKT-<yil>-<sira>; DB UNIQUE asil garanti (nextNoteNo deseni). */
    private String nextPackageNo() {
        String prefix = "PKT-" + LocalDate.now().getYear() + "-";
        long seq = shipmentPackageRepository.countByPackageNoStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", seq);
        int guard = 0;
        while (shipmentPackageRepository.existsByPackageNo(candidate) && guard++ < 1000) {
            candidate = prefix + String.format("%04d", ++seq);
        }
        return candidate;
    }

    ShipmentPackage findEntityById(UUID id) {
        return shipmentPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentPackage", "id", id));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
