package com.uretimtakip.erp.delivery.dto;

import com.uretimtakip.erp.delivery.DeliveryNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DeliveryNote API cevabi. JSON'da global SNAKE_CASE ile doner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNoteResponse {

    private UUID id;
    private String noteNo;
    private UUID orderId;
    private String recipientName;
    private String taxNumber;
    private String taxOffice;
    private String address;
    private String city;
    private String district;
    private String scenario;
    private String noteType;
    private String carrier;
    private String vehiclePlate;
    private String driverName;
    private String containerNo;
    private String tirNo;
    private String cargoTrackingNo;
    private LocalDate etaDate;
    private String status;
    private LocalDate shipDate;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;

    public static DeliveryNoteResponse fromEntity(DeliveryNote e) {
        return DeliveryNoteResponse.builder()
                .id(e.getId())
                .noteNo(e.getNoteNo())
                .orderId(e.getOrderId())
                .recipientName(e.getRecipientName())
                .taxNumber(e.getTaxNumber())
                .taxOffice(e.getTaxOffice())
                .address(e.getAddress())
                .city(e.getCity())
                .district(e.getDistrict())
                .scenario(e.getScenario())
                .noteType(e.getNoteType())
                .carrier(e.getCarrier())
                .vehiclePlate(e.getVehiclePlate())
                .driverName(e.getDriverName())
                .containerNo(e.getContainerNo())
                .tirNo(e.getTirNo())
                .cargoTrackingNo(e.getCargoTrackingNo())
                .etaDate(e.getEtaDate())
                .status(e.getStatus())
                .shipDate(e.getShipDate())
                .notes(e.getNotes())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .shippedAt(e.getShippedAt())
                .build();
    }
}
