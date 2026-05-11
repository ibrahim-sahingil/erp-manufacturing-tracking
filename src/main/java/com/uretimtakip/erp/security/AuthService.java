package com.uretimtakip.erp.security;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.security.dto.AuthResponse;
import com.uretimtakip.erp.security.dto.LoginRequest;
import com.uretimtakip.erp.user.User;
import com.uretimtakip.erp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Login is mantigini yoneten servis.
 *
 * Akis:
 *   1. Frontend POST /api/auth/login -> {username, password}
 *   2. AuthController -> AuthService.login(request)
 *   3. AuthenticationManager username+password'u dogrular
 *      (UserDetailsServiceImpl ile DB'den okur, BCrypt karsilastirir)
 *   4. Basariliysa: JwtService ile token uret
 *   5. Token + user bilgilerini AuthResponse olarak don
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        // 1. Username + Password dogrula (BCrypt karsilastirma dahil)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            log.warn("Authentication failed for user: {} - {}", request.getUsername(), e.getMessage());
            throw new BusinessException("Kullanici adi veya sifre hatali", "BAD_CREDENTIALS");
        }

        // 2. User'i DB'den tam olarak yukle (id, fullName vs icin)
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Kullanici bulunamadi", "USER_NOT_FOUND"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BusinessException("Kullanici aktif degil", "USER_INACTIVE");
        }

        // 3. UserDetails olustur (token icin)
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        // 4. Ekstra bilgileri token'a koy
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("fullName", user.getFullName());
        extraClaims.put("role", user.getRole());
        if (user.getPermissions() != null) {
            extraClaims.put("permissions", user.getPermissions());
        }

        // 5. Token uret
        String token = jwtService.generateToken(userDetails, extraClaims);

        log.info("Login successful for user: {} (id: {})", user.getUsername(), user.getId());

        // 6. Response hazirla
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .department(user.getDepartment())
                .permissions(user.getPermissions())
                .expiresInMs(jwtExpirationMs)
                .build();
    }
}