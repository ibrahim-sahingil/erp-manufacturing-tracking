package com.uretimtakip.erp.material;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.material.dto.MaterialRequest;
import com.uretimtakip.erp.material.dto.MaterialResponse;
import com.uretimtakip.erp.material.dto.MaterialUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Material is mantigi (malzeme kartoteki — arkadas istegi 5. tur #1).
 *
 * Kurallar:
 * - Malzeme adi benzersiz (harf duyarsiz) -> MATERIAL_NAME_DUPLICATE
 *   NOT: kontrol Java tarafinda equalsIgnoreCase ile (supplier'daki gibi —
 *   DB'nin UPPER()'i Turkce ş/İ harflerini locale'e gore katlamayabiliyor).
 *   Kartotek kucuk listedir, tum satirlari cekmek sorun degil.
 * - Silme serbest (bom_parts.material / custom_material snapshot metin,
 *   FK yok); gecmiste kullanilan ad silinse de eski kayitlar etkilenmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    @Transactional(readOnly = true)
    public List<MaterialResponse> listAll(boolean onlyActive) {
        List<Material> list = onlyActive
                ? materialRepository.findByIsActiveTrueOrderByNameAsc()
                : materialRepository.findAllByOrderByNameAsc();
        return list.stream()
                .map(MaterialResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialResponse getById(UUID id) {
        return MaterialResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        if (nameExists(request.getName().trim(), null)) {
            throw new BusinessException(
                    "Bu adla bir malzeme zaten var: " + request.getName(),
                    "MATERIAL_NAME_DUPLICATE");
        }
        Material material = Material.builder()
                .name(request.getName().trim())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Material saved = materialRepository.save(material);
        log.info("Material created: id={}, name={}", saved.getId(), saved.getName());
        return MaterialResponse.fromEntity(saved);
    }

    @Transactional
    public MaterialResponse update(UUID id, MaterialUpdateRequest request) {
        Material material = findEntityById(id);

        // PARTIAL update: sadece gonderilen alanlar islenir.
        if (request.getName() != null && !request.getName().isBlank()
                && !request.getName().trim().equalsIgnoreCase(material.getName())) {
            if (nameExists(request.getName().trim(), material.getId())) {
                throw new BusinessException(
                        "Bu adla bir malzeme zaten var: " + request.getName(),
                        "MATERIAL_NAME_DUPLICATE");
            }
            material.setName(request.getName().trim());
        }
        if (request.getIsActive() != null) material.setIsActive(request.getIsActive());

        Material saved = materialRepository.save(material);
        log.info("Material updated: id={}, name={}, active={}",
                saved.getId(), saved.getName(), saved.getIsActive());
        return MaterialResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Material material = findEntityById(id);
        materialRepository.delete(material);
        log.info("Material deleted: id={}, name={}", id, material.getName());
    }

    private boolean nameExists(String name, UUID excludeId) {
        return materialRepository.findAll().stream()
                .filter(m -> excludeId == null || !m.getId().equals(excludeId))
                .anyMatch(m -> m.getName().equalsIgnoreCase(name));
    }

    private Material findEntityById(UUID id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", id));
    }
}
