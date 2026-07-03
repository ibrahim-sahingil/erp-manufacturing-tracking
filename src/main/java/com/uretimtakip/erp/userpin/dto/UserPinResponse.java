package com.uretimtakip.erp.userpin.dto;

import com.uretimtakip.erp.userpin.UserPin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserPin icin response DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPinResponse {

    private UUID id;
    private UUID userId;
    private String pinType;
    private String pinKey;
    private LocalDateTime createdAt;

    public static UserPinResponse fromEntity(UserPin e) {
        return UserPinResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .pinType(e.getPinType())
                .pinKey(e.getPinKey())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
