package com.uretimtakip.erp.user.dto;

import com.uretimtakip.erp.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User icin response DTO.
 *
 * FRONTEND UYUMU:
 *   Frontend ayni kullaniciyi iki farkli "tablo" gibi okuyor:
 *     - app_users: username, display_name, role, permissions, is_active
 *     - users:     name, dept, role
 *   Tek DTO'da iki alan setini de donuyoruz (SNAKE_CASE global ayari ile
 *   displayName -> display_name, fullName -> full_name olarak serilesir).
 *
 * GUVENLIK: password_hash ASLA donulmez.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String username;
    private String fullName;

    /** fullName kopyasi - frontend app_users.display_name bekliyor. */
    private String displayName;

    /** fullName kopyasi - frontend users.name bekliyor. */
    private String name;

    private String department;

    /** department kopyasi - frontend users.dept bekliyor. */
    private String dept;

    private String role;
    private List<String> permissions;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User e) {
        return UserResponse.builder()
                .id(e.getId())
                .username(e.getUsername())
                .fullName(e.getFullName())
                .displayName(e.getFullName())
                .name(e.getFullName())
                .department(e.getDepartment())
                .dept(e.getDepartment())
                .role(e.getRole())
                .permissions(e.getPermissions())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
