package com.uretimtakip.erp.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * User create/update icin request DTO.
 *
 * FRONTEND UYUMU:
 *   Frontend iki farkli sekilde kullanici gonderiyor (Supabase mirasi):
 *     - users sekmesi (app_users format): username, password_hash,
 *       display_name, role, permissions, is_active
 *     - personel formu (users format): name, dept, role
 *   Bu DTO iki formati da kabul eder; UserService alias'lari cozer:
 *     fullName = fullName || displayName || name
 *     department = department || dept
 *
 * SIFRE:
 *   password veya passwordHash alanindan gelen deger BCrypt'lenir.
 *   Update'te bos gelirse sifre DEGISMEZ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    private String username;

    /** Duz metin sifre (tercih edilen alan adi). */
    private String password;

    /** Frontend eski adiyla gonderirse de kabul et (duz metin gelir, hash'lenir). */
    private String passwordHash;

    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
    private String fullName;

    /** fullName alias'i (app_users formati). */
    private String displayName;

    /** fullName alias'i (users/personel formati). */
    private String name;

    @Size(max = 50, message = "Departman en fazla 50 karakter olabilir")
    private String department;

    /** department alias'i (users/personel formati). */
    private String dept;

    @Size(max = 20, message = "Rol en fazla 20 karakter olabilir")
    private String role;

    private List<String> permissions;

    private Boolean isActive;

    private String pinCode;
}
