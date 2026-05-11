package com.uretimtakip.erp.projectdate;

import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.projectdate.dto.ProjectDateRequest;
import com.uretimtakip.erp.projectdate.dto.ProjectDateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ProjectDate is mantigi.
 *
 * NOT: Tarih degisikligi takibi (revizyon kaydi) ProjectDateRevisionService
 *      icindeki create() ile yapilir. UPDATE'i orada degil, burada tutuyoruz
 *      cunku bazi durumlarda tarih degisikligini revizyon olmadan da yapmak
 *      isteyebilirsin (mesela dogru girilmemisti).
 *
 *      Frontend, "tarih revize et" butonunu basarsa: hem PUT hem de
 *      ProjectDateRevision POST yapacak (iki ayri istek).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDateService {

    private final ProjectDateRepository projectDateRepository;

    @Transactional(readOnly = true)
    public List<ProjectDateResponse> listAll() {
        return projectDateRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ProjectDateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDateResponse getById(UUID id) {
        return ProjectDateResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<ProjectDateResponse> listByOrder(UUID orderId) {
        return projectDateRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(ProjectDateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDateResponse create(ProjectDateRequest request) {
        ProjectDate projectDate = ProjectDate.builder()
                .orderId(request.getOrderId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        ProjectDate saved = projectDateRepository.save(projectDate);
        log.info("ProjectDate created: id={}, orderId={}, start={}, end={}",
                saved.getId(), saved.getOrderId(), saved.getStartDate(), saved.getEndDate());

        return ProjectDateResponse.fromEntity(saved);
    }

    @Transactional
    public ProjectDateResponse update(UUID id, ProjectDateRequest request) {
        ProjectDate projectDate = findEntityById(id);

        projectDate.setOrderId(request.getOrderId());
        projectDate.setStartDate(request.getStartDate());
        projectDate.setEndDate(request.getEndDate());

        ProjectDate updated = projectDateRepository.save(projectDate);
        log.info("ProjectDate updated: id={}", updated.getId());

        return ProjectDateResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        ProjectDate projectDate = findEntityById(id);
        projectDateRepository.delete(projectDate);
        log.info("ProjectDate deleted: id={}", id);
    }

    /**
     * Internal helper - ProjectDateRevisionService bunu kullanir.
     */
    @Transactional(readOnly = true)
    public ProjectDate findEntityById(UUID id) {
        return projectDateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDate", "id", id));
    }
}