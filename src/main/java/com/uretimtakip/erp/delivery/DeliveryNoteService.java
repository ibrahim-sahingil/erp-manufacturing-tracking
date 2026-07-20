package com.uretimtakip.erp.delivery;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteRequest;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteResponse;
import com.uretimtakip.erp.delivery.dto.DeliveryNoteUpdateRequest;
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
 * DeliveryNote is mantigi.
 *
 * NOTE_NO: "IRS-<yil>-<4 haneli sira>" create'te uretilir (yil bazli sayac).
 * STATUS gecis kurallari DeliveryNoteUpdateRequest doc'unda; alan
 * duzenlemeleri yalniz DRAFT'ta yapilir (belge sevkten sonra sabit).
 * DEPO HAREKETLERI backend'de YAZILMAZ - mevcut depo deseni geregi frontend
 * sevk aninda warehouse_movements'e OUT (delivery_note_id dolu) ekler,
 * geri alista ayni id'li hareketleri siler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryNoteService {

    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "SHIPPED", "CANCELLED");

    private final DeliveryNoteRepository deliveryNoteRepository;

    @Transactional(readOnly = true)
    public List<DeliveryNoteResponse> listAll() {
        return deliveryNoteRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(DeliveryNoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryNoteResponse getById(UUID id) {
        return DeliveryNoteResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public DeliveryNoteResponse create(DeliveryNoteRequest request) {
        DeliveryNote note = DeliveryNote.builder()
                .noteNo(nextNoteNo())
                .orderId(request.getOrderId())
                .recipientName(request.getRecipientName())
                .taxNumber(blankToNull(request.getTaxNumber()))
                .taxOffice(blankToNull(request.getTaxOffice()))
                .address(blankToNull(request.getAddress()))
                .city(blankToNull(request.getCity()))
                .district(blankToNull(request.getDistrict()))
                .scenario(request.getScenario() != null && !request.getScenario().isBlank()
                        ? request.getScenario() : "TEMEL")
                .noteType(request.getNoteType() != null && !request.getNoteType().isBlank()
                        ? request.getNoteType() : "SEVK")
                .carrier(blankToNull(request.getCarrier()))
                .vehiclePlate(blankToNull(request.getVehiclePlate()))
                .driverName(blankToNull(request.getDriverName()))
                .containerNo(blankToNull(request.getContainerNo()))
                .tirNo(blankToNull(request.getTirNo()))
                .cargoTrackingNo(blankToNull(request.getCargoTrackingNo()))
                .etaDate(request.getEtaDate())
                .deliveryTerms(blankToNull(request.getDeliveryTerms()))
                .originCountry(blankToNull(request.getOriginCountry()))
                .shipDate(request.getShipDate())
                .notes(blankToNull(request.getNotes()))
                .createdBy(blankToNull(request.getCreatedBy()))
                .build();

        DeliveryNote saved = deliveryNoteRepository.save(note);
        log.info("DeliveryNote created: id={}, noteNo={}, recipient={}",
                saved.getId(), saved.getNoteNo(), saved.getRecipientName());
        return DeliveryNoteResponse.fromEntity(saved);
    }

    @Transactional
    public DeliveryNoteResponse update(UUID id, DeliveryNoteUpdateRequest request) {
        DeliveryNote note = findEntityById(id);

        // Durum gecisi (varsa) once dogrulanir
        if (request.getStatus() != null && !request.getStatus().isBlank()
                && !request.getStatus().equals(note.getStatus())) {
            applyStatusChange(note, request.getStatus());
        }

        // Alan duzenlemeleri yalniz DRAFT'ta (belge sevkten sonra sabit kalir)
        boolean fieldsTouched =
                request.getRecipientName() != null || request.getTaxNumber() != null
                        || request.getTaxOffice() != null || request.getAddress() != null
                        || request.getCity() != null || request.getDistrict() != null
                        || request.getScenario() != null || request.getNoteType() != null
                        || request.getCarrier() != null || request.getShipDate() != null
                        || request.getVehiclePlate() != null || request.getDriverName() != null
                        || request.getContainerNo() != null || request.getTirNo() != null
                        || request.getCargoTrackingNo() != null || request.getEtaDate() != null
                        || request.getDeliveryTerms() != null || request.getOriginCountry() != null
                        || request.getNotes() != null;
        if (fieldsTouched && !"DRAFT".equals(note.getStatus())) {
            throw new BusinessException(
                    "Yalniz taslak (DRAFT) irsaliye duzenlenebilir. Once sevki geri alin.",
                    "DELIVERY_NOTE_NOT_DRAFT");
        }

        if (request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
            note.setRecipientName(request.getRecipientName());
        }
        if (request.getTaxNumber() != null) note.setTaxNumber(blankToNull(request.getTaxNumber()));
        if (request.getTaxOffice() != null) note.setTaxOffice(blankToNull(request.getTaxOffice()));
        if (request.getAddress() != null) note.setAddress(blankToNull(request.getAddress()));
        if (request.getCity() != null) note.setCity(blankToNull(request.getCity()));
        if (request.getDistrict() != null) note.setDistrict(blankToNull(request.getDistrict()));
        if (request.getScenario() != null && !request.getScenario().isBlank()) {
            note.setScenario(request.getScenario());
        }
        if (request.getNoteType() != null && !request.getNoteType().isBlank()) {
            note.setNoteType(request.getNoteType());
        }
        if (request.getCarrier() != null) note.setCarrier(blankToNull(request.getCarrier()));
        if (request.getVehiclePlate() != null) note.setVehiclePlate(blankToNull(request.getVehiclePlate()));
        if (request.getDriverName() != null) note.setDriverName(blankToNull(request.getDriverName()));
        if (request.getContainerNo() != null) note.setContainerNo(blankToNull(request.getContainerNo()));
        if (request.getTirNo() != null) note.setTirNo(blankToNull(request.getTirNo()));
        if (request.getCargoTrackingNo() != null) note.setCargoTrackingNo(blankToNull(request.getCargoTrackingNo()));
        if (request.getEtaDate() != null) note.setEtaDate(request.getEtaDate());
        if (request.getDeliveryTerms() != null) note.setDeliveryTerms(blankToNull(request.getDeliveryTerms()));
        if (request.getOriginCountry() != null) note.setOriginCountry(blankToNull(request.getOriginCountry()));
        if (request.getShipDate() != null) note.setShipDate(request.getShipDate());
        if (request.getNotes() != null) note.setNotes(blankToNull(request.getNotes()));

        DeliveryNote saved = deliveryNoteRepository.save(note);
        log.info("DeliveryNote updated: id={}, noteNo={}, status={}",
                saved.getId(), saved.getNoteNo(), saved.getStatus());
        return DeliveryNoteResponse.fromEntity(saved);
    }

    private void applyStatusChange(DeliveryNote note, String target) {
        if (!VALID_STATUSES.contains(target)) {
            throw new BusinessException("Gecersiz durum: " + target
                    + ". Gecerli degerler: " + VALID_STATUSES, "DELIVERY_NOTE_BAD_STATUS");
        }
        String from = note.getStatus();
        boolean allowed =
                ("DRAFT".equals(from) && ("SHIPPED".equals(target) || "CANCELLED".equals(target)))
                        || ("SHIPPED".equals(from) && "DRAFT".equals(target))
                        || ("CANCELLED".equals(from) && "DRAFT".equals(target));
        if (!allowed) {
            throw new BusinessException(
                    "Gecersiz durum gecisi: " + from + " -> " + target,
                    "DELIVERY_NOTE_BAD_TRANSITION");
        }
        note.setStatus(target);
        if ("SHIPPED".equals(target)) {
            note.setShippedAt(LocalDateTime.now());
            if (note.getShipDate() == null) note.setShipDate(LocalDate.now());
        } else {
            note.setShippedAt(null);
        }
    }

    @Transactional
    public void delete(UUID id) {
        DeliveryNote note = findEntityById(id);
        if ("SHIPPED".equals(note.getStatus())) {
            throw new BusinessException(
                    "Sevk edilmis irsaliye silinemez. Once sevki geri alin.",
                    "DELIVERY_NOTE_SHIPPED");
        }
        deliveryNoteRepository.delete(note);
        log.info("DeliveryNote deleted: id={}, noteNo={}", id, note.getNoteNo());
    }

    /** IRS-<yil>-<sira>; ayni yil icindeki kayit sayisina gore artar. */
    private String nextNoteNo() {
        String prefix = "IRS-" + LocalDate.now().getYear() + "-";
        long seq = deliveryNoteRepository.countByNoteNoStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", seq);
        // Silinen kayitlar nedeniyle cakisma olursa ileri kaydir
        int guard = 0;
        while (deliveryNoteRepository.existsByNoteNo(candidate) && guard++ < 1000) {
            candidate = prefix + String.format("%04d", ++seq);
        }
        return candidate;
    }

    private DeliveryNote findEntityById(UUID id) {
        return deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryNote", "id", id));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
