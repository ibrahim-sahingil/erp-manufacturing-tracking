package com.uretimtakip.erp.carrier;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.carrier.dto.CarrierRequest;
import com.uretimtakip.erp.carrier.dto.CarrierResponse;
import com.uretimtakip.erp.carrier.dto.CarrierUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Carrier is mantigi (nakliye firmasi kartoteki).
 *
 * Kurallar:
 * - Nakliye firmasi adi benzersiz (harf duyarsiz) -> CARRIER_NAME_DUPLICATE
 *   NOT: kontrol Java tarafinda yapilir (equalsIgnoreCase) — DB'nin
 *   UPPER()'i Turkce ş/İ gibi harfleri locale'e gore katlamayabiliyor
 *   (smoke testte "A.Ş." / "a.ş." mukerrer gecti). Kartotek kucuk bir
 *   listedir, tum satirlari cekmek sorun degil.
 * - Silme serbest (purchase_items.carrier / teklif firma adi snapshot metin,
 *   FK yok); gecmiste kullanilan ad silinse de eski kayitlar etkilenmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierService {

    private final CarrierRepository carrierRepository;

    @Transactional(readOnly = true)
    public List<CarrierResponse> listAll(boolean onlyActive) {
        List<Carrier> list = onlyActive
                ? carrierRepository.findByIsActiveTrueOrderByNameAsc()
                : carrierRepository.findAllByOrderByNameAsc();
        return list.stream()
                .map(CarrierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CarrierResponse getById(UUID id) {
        return CarrierResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public CarrierResponse create(CarrierRequest request) {
        if (nameExists(request.getName().trim(), null)) {
            throw new BusinessException(
                    "Bu adla bir nakliye firmasi zaten var: " + request.getName(),
                    "CARRIER_NAME_DUPLICATE");
        }
        Carrier carrier = Carrier.builder()
                .name(request.getName().trim())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .taxOffice(request.getTaxOffice())
                .taxNumber(request.getTaxNumber())
                .notes(request.getNotes())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Carrier saved = carrierRepository.save(carrier);
        log.info("Carrier created: id={}, name={}", saved.getId(), saved.getName());
        return CarrierResponse.fromEntity(saved);
    }

    @Transactional
    public CarrierResponse update(UUID id, CarrierUpdateRequest request) {
        Carrier carrier = findEntityById(id);

        // PARTIAL update: sadece gonderilen alanlar islenir.
        if (request.getName() != null && !request.getName().isBlank()
                && !request.getName().trim().equalsIgnoreCase(carrier.getName())) {
            if (nameExists(request.getName().trim(), carrier.getId())) {
                throw new BusinessException(
                        "Bu adla bir nakliye firmasi zaten var: " + request.getName(),
                        "CARRIER_NAME_DUPLICATE");
            }
            carrier.setName(request.getName().trim());
        }
        // Presence takipli alanlar: explicit null = temizle
        if (request.isContactPersonPresent()) carrier.setContactPerson(request.getContactPerson());
        if (request.isPhonePresent())         carrier.setPhone(request.getPhone());
        if (request.isEmailPresent())         carrier.setEmail(request.getEmail());
        if (request.isAddressPresent())       carrier.setAddress(request.getAddress());
        if (request.isTaxOfficePresent())     carrier.setTaxOffice(request.getTaxOffice());
        if (request.isTaxNumberPresent())     carrier.setTaxNumber(request.getTaxNumber());
        if (request.isNotesPresent())         carrier.setNotes(request.getNotes());
        if (request.getIsActive() != null)    carrier.setIsActive(request.getIsActive());

        Carrier saved = carrierRepository.save(carrier);
        log.info("Carrier updated: id={}, name={}, active={}",
                saved.getId(), saved.getName(), saved.getIsActive());
        return CarrierResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Carrier carrier = findEntityById(id);
        carrierRepository.delete(carrier);
        log.info("Carrier deleted: id={}, name={}", id, carrier.getName());
    }

    private boolean nameExists(String name, UUID excludeId) {
        return carrierRepository.findAll().stream()
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .anyMatch(s -> s.getName().equalsIgnoreCase(name));
    }

    private Carrier findEntityById(UUID id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", id));
    }
}
