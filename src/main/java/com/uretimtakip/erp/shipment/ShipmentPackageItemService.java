package com.uretimtakip.erp.shipment;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageItemRequest;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageItemResponse;
import com.uretimtakip.erp.shipment.dto.ShipmentPackageItemUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ShipmentPackageItem is mantigi (13. tur madde 4).
 *
 * KURAL: satir ekleme/silme/miktar duzeltme yalniz paket OPEN iken —
 * CLOSED paket icerigi kilitlidir (paketleme tarihcesi bozulmasin);
 * once paket OPEN'a geri alinir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentPackageItemService {

    private final ShipmentPackageItemRepository itemRepository;
    private final ShipmentPackageService packageService;

    @Transactional(readOnly = true)
    public List<ShipmentPackageItemResponse> listAll(UUID packageId) {
        List<ShipmentPackageItem> rows = packageId != null
                ? itemRepository.findByPackageIdOrderByCreatedAtAsc(packageId)
                : itemRepository.findAllByOrderByCreatedAtAsc();
        return rows.stream()
                .map(ShipmentPackageItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShipmentPackageItemResponse getById(UUID id) {
        return ShipmentPackageItemResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public ShipmentPackageItemResponse create(ShipmentPackageItemRequest request) {
        requireOpenPackage(request.getPackageId());
        ShipmentPackageItem item = ShipmentPackageItem.builder()
                .packageId(request.getPackageId())
                .partId(request.getPartId())
                .projectBomPartId(request.getProjectBomPartId())
                .itemName(request.getItemName())
                .itemCode(request.getItemCode() != null && !request.getItemCode().isBlank()
                        ? request.getItemCode() : null)
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null && !request.getUnit().isBlank()
                        ? request.getUnit() : "adet")
                .build();
        ShipmentPackageItem saved = itemRepository.save(item);
        log.info("ShipmentPackageItem created: id={}, packageId={}, name={}, qty={}",
                saved.getId(), saved.getPackageId(), saved.getItemName(), saved.getQuantity());
        return ShipmentPackageItemResponse.fromEntity(saved);
    }

    @Transactional
    public ShipmentPackageItemResponse update(UUID id, ShipmentPackageItemUpdateRequest request) {
        ShipmentPackageItem item = findEntityById(id);
        requireOpenPackage(item.getPackageId());
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        ShipmentPackageItem saved = itemRepository.save(item);
        return ShipmentPackageItemResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        ShipmentPackageItem item = findEntityById(id);
        requireOpenPackage(item.getPackageId());
        itemRepository.delete(item);
        log.info("ShipmentPackageItem deleted: id={}, packageId={}", id, item.getPackageId());
    }

    private void requireOpenPackage(UUID packageId) {
        ShipmentPackage pkg = packageService.findEntityById(packageId);
        if (!"OPEN".equals(pkg.getStatus())) {
            throw new BusinessException(
                    "Paket " + pkg.getStatus() + " durumunda — icerik yalniz ACIK (OPEN) pakette degisir."
                            + " Once paketi yeniden acin.",
                    "SHIPMENT_PACKAGE_NOT_OPEN");
        }
    }

    private ShipmentPackageItem findEntityById(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentPackageItem", "id", id));
    }
}
