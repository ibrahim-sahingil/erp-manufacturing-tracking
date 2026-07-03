package com.uretimtakip.erp.userpin;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.userpin.dto.UserPinRequest;
import com.uretimtakip.erp.userpin.dto.UserPinResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UserPin is mantigi.
 *
 * - Ayni kullanici ayni ogeyi iki kez pinleyemez
 *   (DB'de UNIQUE(user_id, pin_type, pin_key) var, once biz kontrol ederiz).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPinService {

    private final UserPinRepository userPinRepository;

    @Transactional(readOnly = true)
    public List<UserPinResponse> listByUser(UUID userId) {
        return userPinRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(UserPinResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserPinResponse create(UserPinRequest request) {
        if (userPinRepository.existsByUserIdAndPinTypeAndPinKey(
                request.getUserId(), request.getPinType(), request.getPinKey())) {
            throw new BusinessException(
                    "Bu oge zaten sabitlenmis: " + request.getPinKey(),
                    "PIN_ALREADY_EXISTS"
            );
        }

        UserPin pin = UserPin.builder()
                .userId(request.getUserId())
                .pinType(request.getPinType())
                .pinKey(request.getPinKey())
                .build();

        UserPin saved = userPinRepository.save(pin);
        log.info("UserPin created: id={}, userId={}, key={}",
                saved.getId(), saved.getUserId(), saved.getPinKey());
        return UserPinResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UserPin pin = userPinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserPin", "id", id));
        userPinRepository.delete(pin);
        log.info("UserPin deleted: id={}", id);
    }
}
