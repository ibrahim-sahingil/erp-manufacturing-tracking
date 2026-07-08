package com.uretimtakip.erp.part;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.part.dto.PartLogRequest;
import com.uretimtakip.erp.part.dto.PartLogResponse;
import com.uretimtakip.erp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PartLog is mantigi.
 *
 * AKILLI OZELLIK:
 *   Yeni log olusturuldugunda, ayni zamanda Part'in
 *   qty_done/pending/reject degerleri de guncellenir.
 *
 *   Yani frontend QR ile "5 adet yapildi" derse:
 *     1. PartLog'a kayit dusulur (tarihce icin)
 *     2. Part'in qty_done degeri 5 artirilir (anlik durum icin)
 *
 *   Bu sayede dashboardda Part'tan o anki durumu, log'lardan
 *   tarihceyi gorebilirsin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartLogService {

    private final PartLogRepository partLogRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PartLogResponse> listAll() {
        return partLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PartLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PartLogResponse> listByPart(UUID partId) {
        return partLogRepository.findByPartIdOrderByCreatedAtDesc(partId)
                .stream()
                .map(PartLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PartLogResponse> listByUser(UUID userId) {
        return partLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PartLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PartLogResponse getById(UUID id) {
        PartLog log = partLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PartLog", "id", id));
        return PartLogResponse.fromEntity(log);
    }

    /**
     * Yeni log olustur VE Part'in qty alanlarini guncelle.
     * Bu method @Transactional - ya hep ya hic mantigi.
     */
    @Transactional
    public PartLogResponse create(PartLogRequest request) {
        // 1. Part var mi?
        Part part = partRepository.findById(request.getPartId())
                .orElseThrow(() -> new ResourceNotFoundException("Part", "id", request.getPartId()));

        // 2. User var mi?
        if (!userRepository.existsById(request.getUserId())) {
            throw new ResourceNotFoundException("User", "id", request.getUserId());
        }

        int qtyDoneInc = request.getQtyDone() != null ? request.getQtyDone() : 0;
        int qtyPendingInc = request.getQtyPending() != null ? request.getQtyPending() : 0;
        int qtyRejectInc = request.getQtyReject() != null ? request.getQtyReject() : 0;

        // 3. En az bir miktar girilmis olmali
        if (qtyDoneInc == 0 && qtyPendingInc == 0 && qtyRejectInc == 0) {
            throw new BusinessException(
                    "qtyDone, qtyPending veya qtyReject'ten en az biri 0'dan buyuk olmali",
                    "INVALID_QTY"
            );
        }

        // 4. (U4) Part sayaclarini ATOMIK artir — Java oku-degistir-yaz yerine
        // tek SQL UPDATE (es zamanli QR girislerinde lost update olmasin).
        // qty_pending, yeni done/reject'ten turetilir; status'e DOKUNULMAZ
        // (frontend autoStatus'u ayri PUT ile yonetir — buradaki eski buyuk-harf
        // 'DONE'/'IN_PROGRESS' zaten o PUT tarafindan eziliyordu).
        partRepository.incrementQty(request.getPartId(), qtyDoneInc, qtyRejectInc);

        // 6. PartLog'u olustur
        PartLog partLog = PartLog.builder()
                .partId(request.getPartId())
                .userId(request.getUserId())
                .qtyDone(qtyDoneInc)
                .qtyPending(qtyPendingInc)
                .qtyReject(qtyRejectInc)
                .note(request.getNote())
                .build();

        PartLog saved = partLogRepository.save(partLog);
        log.info("PartLog created: partId={}, userId={}, qtyDone={}, qtyPending={}, qtyReject={}",
                request.getPartId(), request.getUserId(), qtyDoneInc, qtyPendingInc, qtyRejectInc);

        return PartLogResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PartLog logEntity = partLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PartLog", "id", id));

        // NOT: Sadece log'u sil. Part'in qty alanlari geri donmez.
        // Cunku bu islem genelde duzeltme/iptal anlamina geliyor,
        // duzeltmeyi ayri bir log kaydi ile yapmak daha temiz.
        partLogRepository.delete(logEntity);
        log.info("PartLog deleted: id={}", id);
    }
}