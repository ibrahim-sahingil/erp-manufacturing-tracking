package com.uretimtakip.erp.user;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.security.SecurityUtils;
import com.uretimtakip.erp.user.dto.UserRequest;
import com.uretimtakip.erp.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kullanici is mantigi.
 *
 * ALIAS COZUMU (frontend'in iki formati icin):
 *   fullName  = fullName || displayName || name
 *   department = department || dept
 *   sifre      = password || passwordHash (duz metin gelir, BCrypt'lenir)
 *
 * USERNAME:
 *   - users tablosunda UNIQUE. Create'te bos gelirse fullName'den turetilir
 *     (personel formu username gondermiyor).
 *   - Cakisma durumunda sonuna sayi eklenir (ali, ali2, ali3...).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        // (K3) developer olmayan yalniz SADE personel karti ekleyebilir:
        // hesap alanlari (username/sifre/permissions/is_active) veya
        // developer rolu iceren istek hesap acma girisimidir.
        if (!SecurityUtils.isDeveloper()) {
            boolean accountFields =
                    (request.getUsername() != null && !request.getUsername().isBlank())
                    || resolveRawPassword(request) != null
                    || (request.getPermissions() != null && !request.getPermissions().isEmpty())
                    || request.getIsActive() != null;
            if (accountFields || (request.getRole() != null
                    && request.getRole().equalsIgnoreCase("developer"))) {
                throw new BusinessException(
                        "Hesap acma/yetki atama yalnizca gelistirici rolunun yetkisindedir.",
                        "USER_ADMIN_ONLY");
            }
        }

        String fullName = resolveFullName(request);
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Ad (name/display_name) zorunlu", "USER_NAME_REQUIRED");
        }

        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = generateUniqueUsername(fullName);
        } else if (userRepository.existsByUsername(username)) {
            throw new BusinessException(
                    "Bu kullanici adi zaten kullaniliyor: " + username,
                    "USERNAME_ALREADY_EXISTS"
            );
        }

        String rawPassword = resolveRawPassword(request);
        validatePasswordStrength(rawPassword);

        User user = User.builder()
                .fullName(fullName)
                .department(resolveDepartment(request))
                .role(request.getRole() != null ? request.getRole() : "user")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .username(username)
                .passwordHash(rawPassword != null ? passwordEncoder.encode(rawPassword) : null)
                .pinCode(request.getPinCode())
                .permissions(request.getPermissions() != null
                        ? request.getPermissions() : new ArrayList<>())
                .build();

        User saved = userRepository.save(user);
        log.info("User created: id={}, username={}", saved.getId(), saved.getUsername());
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        User user = findEntityById(id);

        // (K3) developer olmayan yalniz KENDI sifresini degistirebilir —
        // rol/permission/username/is_active degisikligi (kendi kaydinda bile)
        // yetki yukseltme kapisidir, reddedilir.
        if (!SecurityUtils.isDeveloper()) {
            boolean self = user.getUsername() != null
                    && user.getUsername().equals(SecurityUtils.username());
            boolean onlyPassword = resolveRawPassword(request) != null
                    && resolveFullName(request) == null
                    && resolveDepartment(request) == null
                    && request.getRole() == null
                    && request.getIsActive() == null
                    && request.getPermissions() == null
                    && request.getPinCode() == null
                    && (request.getUsername() == null || request.getUsername().isBlank());
            if (!self || !onlyPassword) {
                throw new BusinessException(
                        "Kullanici duzenleme yalnizca gelistirici rolunun yetkisindedir "
                                + "(kendi sifreni degistirmek haric).",
                        "USER_ADMIN_ONLY");
            }
        }

        String fullName = resolveFullName(request);
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        String department = resolveDepartment(request);
        if (department != null) {
            user.setDepartment(department);
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getPermissions() != null) {
            user.setPermissions(request.getPermissions());
        }
        if (request.getPinCode() != null) {
            user.setPinCode(request.getPinCode());
        }

        String username = request.getUsername();
        if (username != null && !username.isBlank()
                && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new BusinessException(
                        "Bu kullanici adi zaten kullaniliyor: " + username,
                        "USERNAME_ALREADY_EXISTS"
                );
            }
            user.setUsername(username);
        }

        // Sifre sadece dolu gelirse degisir
        String rawPassword = resolveRawPassword(request);
        if (rawPassword != null && !rawPassword.isBlank()) {
            validatePasswordStrength(rawPassword);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        User updated = userRepository.save(user);
        log.info("User updated: id={}, username={}", updated.getId(), updated.getUsername());
        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        User user = findEntityById(id);

        // (K3) developer olmayan yalniz giris HESABI OLMAYAN personel kartini
        // silebilir (personel sekmesi akisi). Sifreli hesap silme = admin isi.
        if (!SecurityUtils.isDeveloper()
                && (user.getPasswordHash() != null
                    || "developer".equalsIgnoreCase(user.getRole()))) {
            throw new BusinessException(
                    "Hesap silme yalnizca gelistirici rolunun yetkisindedir.",
                    "USER_ADMIN_ONLY");
        }

        userRepository.delete(user);
        log.info("User deleted: id={}, username={}", id, user.getUsername());
    }

    // ============ HELPERS ============

    private User findEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private String resolveFullName(UserRequest request) {
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            return request.getFullName();
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            return request.getDisplayName();
        }
        return request.getName();
    }

    private String resolveDepartment(UserRequest request) {
        if (request.getDepartment() != null) {
            return request.getDepartment();
        }
        return request.getDept();
    }

    private void validatePasswordStrength(String rawPassword) {
        if (rawPassword != null && rawPassword.length() < 6) {
            throw new BusinessException(
                    "Sifre en az 6 karakter olmali",
                    "PASSWORD_TOO_SHORT"
            );
        }
    }

    private String resolveRawPassword(UserRequest request) {
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            return request.getPassword();
        }
        // Frontend eski aliskanlikla password_hash alaninda duz metin gonderebilir
        if (request.getPasswordHash() != null && !request.getPasswordHash().isBlank()) {
            return request.getPasswordHash();
        }
        return null;
    }

    /**
     * Ad soyaddan benzersiz username uret: "Ali Veli" -> "ali.veli",
     * cakisirsa "ali.veli2", "ali.veli3"...
     */
    private String generateUniqueUsername(String fullName) {
        String base = Normalizer.normalize(fullName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")           // aksanlari at (ü -> u)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[ıi̇]", "i")
                .replaceAll("[^a-z0-9]+", ".")      // harf/rakam disi -> nokta
                .replaceAll("^\\.|\\.$", "");
        if (base.isBlank()) {
            base = "kullanici";
        }
        String candidate = base;
        int suffix = 2;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
