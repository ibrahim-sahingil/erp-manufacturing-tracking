package com.uretimtakip.erp.warehouse;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.delivery.DeliveryNoteRepository;
import com.uretimtakip.erp.purchasing.PurchaseItemRepository;
import com.uretimtakip.erp.warehouse.dto.WarehouseMovementRequest;
import com.uretimtakip.erp.warehouse.dto.WarehouseMovementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WarehouseMovement is mantigi.
 *
 * UPDATE YOK: hareket defteri duzeltilmez; yanlis kayit silinip
 * yeniden girilir (DELETE + POST). Stok her zaman hareketlerden
 * turetildigi icin ayri bir stok senkronu gerekmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseMovementService {

    /** DB'deki warehouse_movements_type_check ile ayni liste. */
    private static final Set<String> VALID_TYPES = Set.of("IN", "OUT");

    /** DB'deki warehouse_movements_source_check ile ayni liste. */
    private static final Set<String> VALID_SOURCES = Set.of(
            "MANUAL", "PURCHASE_TRANSFER", "GOODS_RECEIPT", "DELIVERY",
            "WAREHOUSE_TRANSFER", "RESERVATION", "RESERVATION_ADJUST", "PACKAGE");

    /**
     * Yalniz backend akislarinin ayni transaction'da yazdigi kaynaklar —
     * disaridan POST kabul edilmez: RESERVATION* rezervasyon onayindan
     * (sahte rezervasyon cikisi engeli), PACKAGE (15. tur Y1) paket durum
     * gecislerinden (ShipmentPackageService.reconcilePackageMovements —
     * elle PACKAGE hareketi stok/paket tutarliligini bozar).
     */
    private static final Set<String> INTERNAL_SOURCES = Set.of(
            "RESERVATION", "RESERVATION_ADJUST", "PACKAGE");

    private final WarehouseMovementRepository warehouseMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final DeliveryNoteRepository deliveryNoteRepository;
    private final WarehouseReservationRepository warehouseReservationRepository;

    @Transactional(readOnly = true)
    public List<WarehouseMovementResponse> listAll() {
        return warehouseMovementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(WarehouseMovementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WarehouseMovementResponse> listByWarehouse(UUID warehouseId) {
        return warehouseMovementRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .stream()
                .map(WarehouseMovementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseMovementResponse create(WarehouseMovementRequest request) {
        if (!warehouseRepository.existsById(request.getWarehouseId())) {
            throw new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId());
        }
        if (!VALID_TYPES.contains(request.getMovementType())) {
            throw new BusinessException(
                    "Gecersiz hareket tipi: " + request.getMovementType()
                            + ". Gecerli degerler: " + VALID_TYPES,
                    "WAREHOUSE_MOVEMENT_INVALID_TYPE");
        }
        String sourceType = request.getSourceType() != null && !request.getSourceType().isBlank()
                ? request.getSourceType() : "MANUAL";
        if (!VALID_SOURCES.contains(sourceType)) {
            throw new BusinessException(
                    "Gecersiz kaynak tipi: " + sourceType
                            + ". Gecerli degerler: " + VALID_SOURCES,
                    "WAREHOUSE_MOVEMENT_INVALID_SOURCE");
        }
        if (INTERNAL_SOURCES.contains(sourceType)) {
            throw new BusinessException(
                    "Bu kaynak tipi yalnizca ilgili backend akisinda yazilir "
                            + "(rezervasyon onayi / paket durum gecisi): " + sourceType,
                    "WAREHOUSE_MOVEMENT_INVALID_SOURCE");
        }

        WarehouseMovement movement = WarehouseMovement.builder()
                .warehouseId(request.getWarehouseId())
                .purchaseItemId(request.getPurchaseItemId())
                .deliveryNoteId(request.getDeliveryNoteId())
                .itemName(request.getItemName())
                .itemCode(request.getItemCode())
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .sourceType(sourceType)
                .performedBy(request.getPerformedBy())
                .notes(request.getNotes())
                .build();

        WarehouseMovement saved = warehouseMovementRepository.save(movement);
        log.info("WarehouseMovement created: id={}, warehouse={}, type={}, item={}, qty={}",
                saved.getId(), saved.getWarehouseId(), saved.getMovementType(),
                saved.getItemName(), saved.getQuantity());
        return WarehouseMovementResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        WarehouseMovement movement = warehouseMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseMovement", "id", id));

        // (O5) Defter tutarliligi: bagli kaydi YASAYAN otomatik hareketler
        // silinemez — kalem IN_WAREHOUSE gorunurken stok ozetinden dusmesi
        // (veya sevk cikisinin geri sismesi) sessiz tutarsizlik yaratir.
        // MANUAL ve WAREHOUSE_TRANSFER serbesttir; kalem silinmisse bag
        // SET NULL oldugundan artik hareket de silinebilir.
        String src = movement.getSourceType();
        if (("GOODS_RECEIPT".equals(src) || "PURCHASE_TRANSFER".equals(src))
                && movement.getPurchaseItemId() != null
                && purchaseItemRepository.existsById(movement.getPurchaseItemId())) {
            throw new BusinessException(
                    "Bu hareket satin alma kalemine bagli (mal kabul / depo aktarimi), "
                            + "defterden silinemez. Duzeltme icin kalemi 'Depodan Geri Al' "
                            + "ile cikarin.",
                    "WAREHOUSE_MOVEMENT_LINKED");
        }
        // Rezervasyona bagli TUM hareketler korunur — RESERVATION/ADJUST
        // cikislarina ek olarak toplama deposunun WAREHOUSE_TRANSFER cifti de
        // (8. tur #1) yasayan rezervasyon varken silinemez; teki silinirse
        // stok sessizce kayar. Rezervasyon silinince bag SET NULL olur.
        if (movement.getReservationId() != null
                && warehouseReservationRepository.existsById(movement.getReservationId())) {
            throw new BusinessException(
                    "Bu hareket bir depo rezervasyonuna bagli, defterden silinemez. "
                            + "Once rezervasyon kaydini silin.",
                    "WAREHOUSE_MOVEMENT_LINKED");
        }
        // (15. tur Y1) Paket akisi hareketleri paket durumuyla yonetilir:
        // paket yasarken defterden silinirse stok sessizce kayar. Paket
        // silinirse FK CASCADE zaten hareketleri de goturur.
        if ("PACKAGE".equals(src) && movement.getShipmentPackageId() != null) {
            throw new BusinessException(
                    "Bu hareket bir sevkiyat paketine bagli, defterden silinemez. "
                            + "Duzeltme icin paketi yeniden acin / yuklemeyi geri alin.",
                    "WAREHOUSE_MOVEMENT_LINKED");
        }
        if ("DELIVERY".equals(src) && movement.getDeliveryNoteId() != null) {
            // Sevk cikisi yalniz irsaliye geri alma akisinda silinir:
            // dnUnship once irsaliyeyi DRAFT'a dondurur, sonra siler.
            boolean shipped = deliveryNoteRepository.findById(movement.getDeliveryNoteId())
                    .map(dn -> !"DRAFT".equals(dn.getStatus()))
                    .orElse(false);
            if (shipped) {
                throw new BusinessException(
                        "Sevk edilmis irsaliyenin depo cikisi silinemez. "
                                + "Once irsaliyede 'Sevki Geri Al' kullanin.",
                        "WAREHOUSE_MOVEMENT_LINKED");
            }
        }

        warehouseMovementRepository.delete(movement);
        log.info("WarehouseMovement deleted: id={}, item={}", id, movement.getItemName());
    }
}
