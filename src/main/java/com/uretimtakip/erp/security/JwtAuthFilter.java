package com.uretimtakip.erp.security;

import com.uretimtakip.erp.user.User;
import com.uretimtakip.erp.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Her HTTP request icin SADECE BIR KEZ calisan filter.
 *
 * Calisma:
 *   1. Authorization header'i oku ("Bearer xxx.yyy.zzz")
 *   2. Token'i JwtService ile dogrula
 *   3. Gecerliyse user'i DB'den yukle
 *   4. SecurityContext'e ekle (Spring Security'nin gormesi icin)
 *   5. FilterChain'e devam et (request'i controller'a gonder)
 *
 * Eger token yoksa veya gecersizse, request yine devam eder
 * ama SecurityContext bos kalir. Bu durumda controller-level
 * yetkilendirme (@PreAuthorize vs SecurityConfig) request'i reddeder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Token yoksa veya format yanlissa hicbir sey yapma, devam et
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7); // "Bearer " kismini kes
            final String username = jwtService.extractUsername(jwt);

            // Token'da username var ve henuz auth yapilmamissa
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

                UserDetails userDetails = buildUserDetails(user);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            // Hata olursa SecurityContext bos kalir - request devam eder
            // ama korumal endpoint'lere erisemez
        }

        filterChain.doFilter(request, response);
    }

    /**
     * User entity'sini Spring Security'nin anlayabilecegi UserDetails'e cevirir.
     * Permissions ve role'u GrantedAuthority olarak ekler.
     */
    private UserDetails buildUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = user.getPermissions() == null
                ? Collections.emptyList()
                : user.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Role'u de bir authority olarak ekle (ROLE_ prefix ile)
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(authorities)
                .disabled(user.getIsActive() != null && !user.getIsActive())
                .build();
    }
}