package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.purchasing.PurchaseItem;
import com.uretimtakip.erp.purchasing.PurchaseItemRepository;
import com.uretimtakip.erp.warehouse.dto.WarehouseReservationApproveRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseReservationRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseReservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WarehouseReservation is mantigi (MIP Asama 2, 7. tur #4).
 *
 * approve() TEK TRANSACTION'da: durum gecisi + OUT hareket(ler)i +
 * eksik icin satin alma kalemi. Hareketleri BACKEND yazar — frontend
 * mvInsert cagirmaz (cift OUT riski). Bu yuzden RESERVATION /
 * RESERVATION_ADJUST kaynak tipleri disaridan POST ile kabul edilmez
 * (WarehouseMovementService.create reddeder).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseReservationService {

    /** numeric(15,4) karsilastirmalarinda kayan nokta payi. */
    private static final BigDecimal EPS = new BigDecimal("0.0001");

    private final WarehouseReservationRepository warehouseReservationRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseMovementRepository warehouseMovementRepository;
    private final PurchaseItemRepository purchaseItemRepository;

    @Transactional(readOnly = true)
    public List<WarehouseReservationResponse> list(String status) {
        List<WarehouseReservation> rows = status != null && !status.isBlank()
                ? warehouseReservationRepository.findByStatusOrderByCreatedAtDesc(status)
                : warehouseReservationRepository.findAllByOrderByCreatedAtDesc();
        return rows.stream()
                .map(WarehouseReservationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseReservationResponse create(WarehouseReservationRequest request) {
        if (!warehouseRepository.existsById(request.getWarehouseId())) {
            throw new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId());
        }
        // Toplama deposu (istege bagli): kaynakla ayniysa anlamsiz -> NULL'a indirgenir
        UUID target = request.getTargetWarehouseId();
        if (target != null && target.equals(request.getWarehouseId())) {
            target = null;
        }
        if (target != null && !warehouseRepository.existsById(target)) {
            throw new ResourceNotFoundException("Warehouse", "id", target);
        }
        // Stok on-kontrolu YAPILMAZ: fiziksel sayim kayittan farkli cikabilir,
        // talep her halde depoya dusebilmeli (onay asamasi gercegi soyler).
        WarehouseReservation reservation = WarehouseReservation.builder()
                .projectName(request.getProjectName().trim())
                .warehouseId(request.getWarehouseId())
                .targetWarehouseId(target)
                .itemName(request.getItemName().trim())
                .itemCode(blankToNull(request.getItemCode()))
                .requestedQty(request.getRequestedQty())
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .requestedBy(blankToNull(request.getRequestedBy()))
                .notes(blankToNull(request.getNotes()))
                .build();

        WarehouseReservation saved = warehouseReservationRepository.save(reservation);
        log.info("WarehouseReservation created: id={}, project={}, item={}, qty={}",
                saved.getId(), saved.getProjectName(), saved.getItemName(),
                saved.getRequestedQty());
        return WarehouseReservationResponse.fromEntity(saved);
    }

    /**
     * Tam/kismi onay. Sira:
     *  1. Yalniz REQUESTED onaylanabilir (cift onay / yaris korumasi).
     *  2. 0 <= approved <= requested (BigDecimal compareTo — equals scale tuzagi).
     *  3. Kismi/red icin shortage_reason zorunlu.
     *  4. approved kayitli stogu asamaz (stok negatife dusmesin).
     *  5. approved > 0  -> OUT (RESERVATION): projeye mal edilir. Toplama
     *     deposu istendiyse (8. tur #1) once WAREHOUSE_TRANSFER cifti
     *     (kaynak OUT + hedef IN), RESERVATION OUT'u HEDEF depodan yazilir —
     *     "hem projeye islensin hem istenirse depolar arasi aktarilsin".
     *  6. eksik > 0     -> satin almaya duser: ayni proje+malzemenin siparis
     *     verilmemis PLANNED kalemi varsa miktari artirilir (9. tur M7 —
     *     "eksik 42 oldu ama siparis 40'ta kaldi" tutarsizligi), yoksa yeni
     *     PLANNED purchase_items kaydi acilir.
     *  7. write_adjustment && eksik > 0 -> ikinci OUT (RESERVATION_ADJUST,
     *     KAYNAK depodan), miktar = min(eksik, stok - approved): sayimda
     *     cikmayan kayit duzeltilir, stok eksiye dusurulmez.
     */
    @Transactional
    public WarehouseReservationResponse approve(UUID id, WarehouseReservationApproveRequest request) {
        WarehouseReservation reservation = findEntityById(id);
        if (!"REQUESTED".equals(reservation.getStatus())) {
            throw new BusinessException(
                    "Bu talep zaten sonuclanmis (durum: " + reservation.getStatus() + ")",
                    "RESERVATION_NOT_PENDING");
        }

        BigDecimal requested = reservation.getRequestedQty();
        BigDecimal approved = request.getApprovedQty();
        if (approved.compareTo(BigDecimal.ZERO) < 0 || approved.compareTo(requested) > 0) {
            throw new BusinessException(
                    "Onaylanan miktar 0 ile istenen miktar (" + requested.stripTrailingZeros()
                            .toPlainString() + ") arasinda olmali",
                    "RESERVATION_QTY_INVALID");
        }

        boolean partial = approved.compareTo(requested) < 0;
        String reason = blankToNull(request.getShortageReason());
        if (partial && reason == null) {
            throw new BusinessException(
                    "Kismi onayda / redde aciklama zorunlu (kayip / envanter yanlis vb.)",
                    "RESERVATION_REASON_REQUIRED");
        }

        BigDecimal stock = recordedStock(reservation.getWarehouseId(),
                reservation.getItemName(), reservation.getItemCode());
        if (approved.subtract(stock).compareTo(EPS) > 0) {
            throw new BusinessException(
                    "Onaylanan miktar kayitli stogu asiyor (kayitli stok: "
                            + stock.stripTrailingZeros().toPlainString()
                            + "). Once eksik girisleri defterden duzeltin.",
                    "RESERVATION_STOCK_EXCEEDED");
        }

        String approvedBy = blankToNull(request.getApprovedBy());

        if (approved.compareTo(BigDecimal.ZERO) > 0) {
            UUID target = reservation.getTargetWarehouseId();
            boolean toplama = target != null && !target.equals(reservation.getWarehouseId());
            String rezervNotu = "MIP rezervasyonu -> " + reservation.getProjectName()
                    + (reason != null ? " | " + reason : "");
            if (toplama) {
                // 8. tur #1: once fiziksel toplama izi (transfer cifti), sonra
                // projeye ayirma HEDEF depodan. Net stok tum depolarda ayni
                // (MIP cifte saymaz) ama defter B->A->proje yolunu gosterir.
                String xferNotu = "Rezervasyon toplamasi: " + warehouseName(reservation.getWarehouseId())
                        + " -> " + warehouseName(target)
                        + " (" + reservation.getProjectName() + ")";
                saveMovement(reservation, reservation.getWarehouseId(), "OUT", approved,
                        "WAREHOUSE_TRANSFER", approvedBy, xferNotu);
                saveMovement(reservation, target, "IN", approved,
                        "WAREHOUSE_TRANSFER", approvedBy, xferNotu);
                saveMovement(reservation, target, "OUT", approved,
                        "RESERVATION", approvedBy,
                        rezervNotu + " (toplama deposu uzerinden)");
            } else {
                saveMovement(reservation, reservation.getWarehouseId(), "OUT", approved,
                        "RESERVATION", approvedBy, rezervNotu);
            }
        }

        BigDecimal shortage = requested.subtract(approved);
        if (shortage.compareTo(EPS) > 0) {
            String shortageNote = "MIP rezervasyon eksigi (talep " + requested.stripTrailingZeros()
                    .toPlainString() + ", onay " + approved.stripTrailingZeros()
                    .toPlainString() + "): " + reason;
            // (9. tur M7) Ayni proje+malzeme icin siparis verilmemis PLANNED kalem
            // varsa miktari ARTIRILIR — onceden her eksik AYRI kucuk kalem aciyordu;
            // kullanici satin almada "eksik 42 oldu ama siparis 40'ta kaldi" goruyordu.
            // Dokunulmayanlar: ORDERED ve sonrasi, gruba bagli (purchase_order_id),
            // plaka planina bagli (stock_plan_id), planlama havuzundaki (needs_planning)
            // — bunlarin miktarini degistirmek siparis/plan butunlugunu bozar.
            String key = stockKey(reservation.getItemName(), reservation.getItemCode());
            PurchaseItem mevcut = null;
            for (PurchaseItem pi : purchaseItemRepository
                    .findByProjectNameOrderByCreatedAtAsc(reservation.getProjectName())) {
                if ("PLANNED".equals(pi.getStatus())
                        && pi.getPurchaseOrderId() == null
                        && pi.getStockPlanId() == null
                        && !Boolean.TRUE.equals(pi.getNeedsPlanning())
                        && stockKey(pi.getName(), pi.getCode()).equals(key)) {
                    mevcut = pi; // ASC sirali liste — en yenisi kazanir
                }
            }
            if (mevcut != null) {
                mevcut.setQuantity(mevcut.getQuantity().add(shortage));
                String eskiNot = mevcut.getNotes();
                mevcut.setNotes((eskiNot == null || eskiNot.isBlank() ? "" : eskiNot + "\n")
                        + "+" + shortage.stripTrailingZeros().toPlainString()
                        + " — " + shortageNote);
                purchaseItemRepository.save(mevcut);
                log.info("WarehouseReservation shortage -> existing PLANNED item increased: "
                                + "reservation={}, item={}, +{} -> {}",
                        id, mevcut.getId(), shortage, mevcut.getQuantity());
            } else {
                PurchaseItem purchaseItem = PurchaseItem.builder()
                        .projectName(reservation.getProjectName())
                        .name(reservation.getItemName())
                        .code(reservation.getItemCode())
                        .quantity(shortage)
                        .unit(reservation.getUnit())
                        .createdBy(approvedBy)
                        .notes(shortageNote)
                        .build();
                purchaseItemRepository.save(purchaseItem);
                log.info("WarehouseReservation shortage -> purchase item: reservation={}, qty={}",
                        id, shortage);
            }

            if (request.isWriteAdjustment()) {
                BigDecimal adjust = shortage.min(stock.subtract(approved));
                if (adjust.compareTo(EPS) > 0) {
                    // Duzeltme her zaman KAYNAK depodan: hayalet kayit orada
                    saveMovement(reservation, reservation.getWarehouseId(), "OUT", adjust,
                            "RESERVATION_ADJUST", approvedBy,
                            "Envanter duzeltmesi (rezervasyon kaybi, "
                                    + reservation.getProjectName() + "): " + reason);
                }
            }
        }

        reservation.setStatus(approved.compareTo(BigDecimal.ZERO) == 0 ? "REJECTED"
                : partial ? "PARTIAL" : "APPROVED");
        reservation.setApprovedQty(approved);
        reservation.setApprovedBy(approvedBy);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setShortageReason(reason);

        WarehouseReservation saved = warehouseReservationRepository.save(reservation);
        log.info("WarehouseReservation approved: id={}, status={}, requested={}, approved={}",
                id, saved.getStatus(), requested, approved);
        return WarehouseReservationResponse.fromEntity(saved);
    }

    @Transactional
    public WarehouseReservationResponse cancel(UUID id) {
        WarehouseReservation reservation = findEntityById(id);
        if (!"REQUESTED".equals(reservation.getStatus())) {
            throw new BusinessException(
                    "Yalnizca bekleyen (REQUESTED) talep iptal edilebilir (durum: "
                            + reservation.getStatus() + ")",
                    "RESERVATION_NOT_PENDING");
        }
        reservation.setStatus("CANCELLED");
        WarehouseReservation saved = warehouseReservationRepository.save(reservation);
        log.info("WarehouseReservation cancelled: id={}", id);
        return WarehouseReservationResponse.fromEntity(saved);
    }

    /**
     * SILME SERBEST (guard yok, bilincli): yanlis talep + e2e temizligi icin.
     * Bagli hareketlerin reservation_id'si SET NULL olur; hareket notes'u
     * proje/malzeme snapshot'i tasidigindan defter izi kaybolmaz. Frontend
     * yalnizca REQUESTED kayitta sil/iptal gosterir.
     */
    @Transactional
    public void delete(UUID id) {
        WarehouseReservation reservation = findEntityById(id);
        warehouseReservationRepository.delete(reservation);
        log.info("WarehouseReservation deleted: id={}, status={}, item={}",
                id, reservation.getStatus(), reservation.getItemName());
    }

    private void saveMovement(WarehouseReservation r, UUID warehouseId, String type,
                              BigDecimal qty, String sourceType, String performedBy,
                              String notes) {
        WarehouseMovement movement = WarehouseMovement.builder()
                .warehouseId(warehouseId)
                .reservationId(r.getId())
                .itemName(r.getItemName())
                .itemCode(r.getItemCode())
                .movementType(type)
                .quantity(qty)
                .unit(r.getUnit())
                .sourceType(sourceType)
                .performedBy(performedBy)
                .notes(notes)
                .build();
        warehouseMovementRepository.save(movement);
    }

    private String warehouseName(UUID id) {
        return warehouseRepository.findById(id)
                .map(Warehouse::getName).orElse("?");
    }

    /**
     * Kayitli stok = SUM(IN) - SUM(OUT), eslesme frontend mipKey ile birebir:
     * kod varsa kod, yoksa ad; trim + kucuk harf. Locale.ROOT ZORUNLU —
     * tr locale'de "I".toLowerCase() "i" yerine noktasiz "i" uretir ve
     * JS toLowerCase() ile sessizce ayrisir.
     */
    private BigDecimal recordedStock(UUID warehouseId, String itemName, String itemCode) {
        String key = stockKey(itemName, itemCode);
        BigDecimal net = BigDecimal.ZERO;
        for (WarehouseMovement m : warehouseMovementRepository
                .findByWarehouseIdOrderByCreatedAtDesc(warehouseId)) {
            if (!stockKey(m.getItemName(), m.getItemCode()).equals(key)) {
                continue;
            }
            net = "IN".equals(m.getMovementType())
                    ? net.add(m.getQuantity()) : net.subtract(m.getQuantity());
        }
        return net;
    }

    private static String stockKey(String name, String code) {
        String base = code != null && !code.trim().isEmpty() ? code
                : name != null ? name : "";
        return base.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }

    private WarehouseReservation findEntityById(UUID id) {
        return warehouseReservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseReservation", "id", id));
    }
}
