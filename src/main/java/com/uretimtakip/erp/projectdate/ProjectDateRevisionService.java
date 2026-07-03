package com.uretimtakip.erp.projectdate;

import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.projectdate.dto.ProjectDateRevisionRequest;
import com.uretimtakip.erp.projectdate.dto.ProjectDateRevisionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ProjectDateRevision is mantigi.
 *
 * AKILLI OZELLIK:
 *   Yeni revize kaydedilirken:
 *     1. Eger oldStart/oldEnd verilmemise, mevcut ProjectDate'in degerlerinden alir.
 *     2. ProjectDate'in start/end tarihlerini newStart/newEnd ile guncellestirir.
 *     3. Revize kaydini olusturur.
 *
 *   Boylece tek bir POST isteginde:
 *     - ProjectDate guncellenir (anlik durum)
 *     - Revize kaydi olusur (tarihce)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDateRevisionService {

    private final ProjectDateRevisionRepository revisionRepository;
    private final ProjectDateRepository projectDateRepository;

    @Transactional(readOnly = true)
    public List<ProjectDateRevisionResponse> listAll() {
        return revisionRepository.findAll()
                .stream()
                .map(ProjectDateRevisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDateRevisionResponse> listByProjectDate(UUID projectDateId) {
        return revisionRepository.findByProjectDateIdOrderByCreatedAtDesc(projectDateId)
                .stream()
                .map(ProjectDateRevisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDateRevisionResponse> listByUser(UUID userId) {
        return revisionRepository.findByRevisedByOrderByCreatedAtDesc(userId)
                .stream()
                .map(ProjectDateRevisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDateRevisionResponse create(ProjectDateRevisionRequest request) {
        // ProjectDate var mi?
        ProjectDate projectDate = projectDateRepository.findById(request.getProjectDateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ProjectDate", "id", request.getProjectDateId()));

        // Eger oldStart/oldEnd verilmemise, mevcut degerleri kullan
        java.time.LocalDate oldStart = request.getOldStart() != null
                ? request.getOldStart()
                : projectDate.getStartDate();
        java.time.LocalDate oldEnd = request.getOldEnd() != null
                ? request.getOldEnd()
                : projectDate.getEndDate();

        // Revize kaydini olustur
        ProjectDateRevision revision = ProjectDateRevision.builder()
                .projectDateId(request.getProjectDateId())
                .oldStart(oldStart)
                .oldEnd(oldEnd)
                .newStart(request.getNewStart())
                .newEnd(request.getNewEnd())
                .reason(request.getReason())
                .revisedBy(request.getRevisedBy())
                .build();

        ProjectDateRevision saved = revisionRepository.save(revision);

        // ProjectDate'i de guncelle (yeni tarihler artik aktif)
        projectDate.setStartDate(request.getNewStart());
        projectDate.setEndDate(request.getNewEnd());
        projectDateRepository.save(projectDate);

        log.info("ProjectDateRevision created: projectDateId={}, old=[{} to {}], new=[{} to {}]",
                request.getProjectDateId(), oldStart, oldEnd,
                request.getNewStart(), request.getNewEnd());

        return ProjectDateRevisionResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        ProjectDateRevision revision = revisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectDateRevision", "id", id));

        revisionRepository.delete(revision);
        log.info("ProjectDateRevision deleted: id={}", id);
    }
}