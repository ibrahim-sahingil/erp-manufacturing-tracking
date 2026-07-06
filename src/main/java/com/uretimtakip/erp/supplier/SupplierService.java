package com.uretimtakip.erp.supplier;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.supplier.dto.SupplierRequest;
import com.uretimtakip.erp.supplier.dto.SupplierResponse;
import com.uretimtakip.erp.supplier.dto.SupplierUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Supplier is mantigi (tedarikci kartoteki).
 *
 * Kurallar:
 * - Tedarikci adi benzersiz (harf duyarsiz) -> SUPPLIER_NAME_DUPLICATE
 *   NOT: kontrol Java tarafinda yapilir (equalsIgnoreCase) — DB'nin
 *   UPPER()'i Turkce ş/İ gibi harfleri locale'e gore katlamayabiliyor
 *   (smoke testte "A.Ş." / "a.ş." mukerrer gecti). Kartotek kucuk bir
 *   listedir, tum satirlari cekmek sorun degil.
 * - Silme serbest (purchase_items.supplier / teklif firma adi snapshot metin,
 *   FK yok); gecmiste kullanilan ad silinse de eski kayitlar etkilenmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierResponse> listAll(boolean onlyActive) {
        List<Supplier> list = onlyActive
                ? supplierRepository.findByIsActiveTrueOrderByNameAsc()
                : supplierRepository.findAllByOrderByNameAsc();
        return list.stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(UUID id) {
        return SupplierResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (nameExists(request.getName().trim(), null)) {
            throw new BusinessException(
                    "Bu adla bir tedarikci zaten var: " + request.getName(),
                    "SUPPLIER_NAME_DUPLICATE");
        }
        Supplier supplier = Supplier.builder()
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

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created: id={}, name={}", saved.getId(), saved.getName());
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierUpdateRequest request) {
        Supplier supplier = findEntityById(id);

        // PARTIAL update: sadece gonderilen alanlar islenir.
        if (request.getName() != null && !request.getName().isBlank()
                && !request.getName().trim().equalsIgnoreCase(supplier.getName())) {
            if (nameExists(request.getName().trim(), supplier.getId())) {
                throw new BusinessException(
                        "Bu adla bir tedarikci zaten var: " + request.getName(),
                        "SUPPLIER_NAME_DUPLICATE");
            }
            supplier.setName(request.getName().trim());
        }
        // Presence takipli alanlar: explicit null = temizle
        if (request.isContactPersonPresent()) supplier.setContactPerson(request.getContactPerson());
        if (request.isPhonePresent())         supplier.setPhone(request.getPhone());
        if (request.isEmailPresent())         supplier.setEmail(request.getEmail());
        if (request.isAddressPresent())       supplier.setAddress(request.getAddress());
        if (request.isTaxOfficePresent())     supplier.setTaxOffice(request.getTaxOffice());
        if (request.isTaxNumberPresent())     supplier.setTaxNumber(request.getTaxNumber());
        if (request.isNotesPresent())         supplier.setNotes(request.getNotes());
        if (request.getIsActive() != null)    supplier.setIsActive(request.getIsActive());

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier updated: id={}, name={}, active={}",
                saved.getId(), saved.getName(), saved.getIsActive());
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Supplier supplier = findEntityById(id);
        supplierRepository.delete(supplier);
        log.info("Supplier deleted: id={}, name={}", id, supplier.getName());
    }

    private boolean nameExists(String name, UUID excludeId) {
        return supplierRepository.findAll().stream()
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .anyMatch(s -> s.getName().equalsIgnoreCase(name));
    }

    private Supplier findEntityById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
    }
}
