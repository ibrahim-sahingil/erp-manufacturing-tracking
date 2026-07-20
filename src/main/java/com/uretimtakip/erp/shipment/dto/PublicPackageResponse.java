package com.uretimtakip.erp.shipment.dto;

import com.uretimtakip.erp.delivery.DeliveryNote;
import com.uretimtakip.erp.shipment.ShipmentPackage;
import com.uretimtakip.erp.shipment.ShipmentPackageItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Halka acik tek-paket gorunumu (15. tur T1 — ?paket= girissiz acilir).
 *
 * QR etiketi okutan HERKESE doner; bu yuzden alan kumesi SINIRLI:
 * ic notlar (notes), kayit sahibi (created_by) ve ic UUID baglari DONMEZ.
 * Liste uclari kilitli kalir — bu DTO paketi icerigi + depo adi + bagli
 * irsaliyenin arac ozetiyle TEK istekte toplar (frontend'in 4 liste cagrisi
 * cekmesine gerek kalmaz, veri sizintisi olmaz).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicPackageResponse {

    private String packageNo;
    private String projectName;
    private String name;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal netWeightKg;
    private String packageType;
    private String status;
    private String packedBy;
    private LocalDateTime packedAt;
    private String warehouseName;
    private List<Item> items;
    private NoteSummary deliveryNote;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String itemName;
        private String itemCode;
        private BigDecimal quantity;
        private String unit;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoteSummary {
        private String noteNo;
        private String status;
        private String carrier;
        private String vehiclePlate;
        private String driverName;
        private String containerNo;
        private String tirNo;
        private String cargoTrackingNo;
        private LocalDate shipDate;
        private LocalDate etaDate;
    }

    public static PublicPackageResponse of(ShipmentPackage pkg,
                                           List<ShipmentPackageItem> items,
                                           String warehouseName,
                                           DeliveryNote note) {
        return PublicPackageResponse.builder()
                .packageNo(pkg.getPackageNo())
                .projectName(pkg.getProjectName())
                .name(pkg.getName())
                .lengthCm(pkg.getLengthCm())
                .widthCm(pkg.getWidthCm())
                .heightCm(pkg.getHeightCm())
                .weightKg(pkg.getWeightKg())
                .netWeightKg(pkg.getNetWeightKg())
                .packageType(pkg.getPackageType())
                .status(pkg.getStatus())
                .packedBy(pkg.getPackedBy())
                .packedAt(pkg.getPackedAt())
                .warehouseName(warehouseName)
                .items(items.stream().map(i -> Item.builder()
                        .itemName(i.getItemName())
                        .itemCode(i.getItemCode())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .build()).toList())
                .deliveryNote(note == null ? null : NoteSummary.builder()
                        .noteNo(note.getNoteNo())
                        .status(note.getStatus())
                        .carrier(note.getCarrier())
                        .vehiclePlate(note.getVehiclePlate())
                        .driverName(note.getDriverName())
                        .containerNo(note.getContainerNo())
                        .tirNo(note.getTirNo())
                        .cargoTrackingNo(note.getCargoTrackingNo())
                        .shipDate(note.getShipDate())
                        .etaDate(note.getEtaDate())
                        .build())
                .build();
    }
}
