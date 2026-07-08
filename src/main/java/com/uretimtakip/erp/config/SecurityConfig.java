package com.uretimtakip.erp.config;

import com.uretimtakip.erp.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security ana konfigurasyonu.
 *
 * Yapilanlar:
 *   1. CSRF kapatildi (REST API icin gerekli degil)
 *   2. Session kapatildi (JWT stateless)
 *   3. CORS acildi (frontend baska port'tan)
 *   4. /api/auth/** public (login icin)
 *   5. Diger endpoint'ler authentication ister
 *   6. JWT filter UsernamePasswordAuthenticationFilter'dan once cagrilir
 *   7. PasswordEncoder = BCrypt
 *
 * @EnableMethodSecurity: @PreAuthorize, @PostAuthorize gibi anotasyonlari aktif eder.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoint'ler (token gerektirmez)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/config").permitAll() // (E3) QR base-url; hassas veri yok
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Statik frontend (static/index.html) - login ekrani
                        // API cagrilari yine JWT ister
                        .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()

                        // (K3) Yikici uclar frontend'deki gorunurluk kurallariyla
                        // AYNI sartla backend'de de denetlenir — JWT'si olan
                        // herkes API'ye dogrudan istek atabilir (tunel acik).
                        // Siparis/proje yazma: developer VEYA orders yetkisi
                        // (frontend canEdit ile birebir; GET herkese acik kalir).
                        .requestMatchers(HttpMethod.POST, "/api/orders/**")
                            .hasAnyAuthority("ROLE_DEVELOPER", "orders_edit", "orders")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**")
                            .hasAnyAuthority("ROLE_DEVELOPER", "orders_edit", "orders")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/**")
                            .hasAnyAuthority("ROLE_DEVELOPER", "orders_edit", "orders")
                        // Kullanici yazma uclari authenticated kalir; hesap/rol/
                        // yetki degisiklikleri UserService'te alan bazli denetlenir
                        // (personel karti ekleme + kendi sifreni degistirme serbest).

                        // Diger her endpoint icin authentication gerekli
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder - sifreleri hash'lemek icin.
     * Strength 10: production icin uygun (varsayilan).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthService'in login icin kullanacagi AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS ayarlari - frontend baska port'tan istek atabilsin diye.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5500",
                "http://localhost:5500",
                "http://127.0.0.1:8080",
                "http://localhost:8080"
        ));

        // Desen bazli izinler: Cloudflare Tunnel (demo) ve yerel ag erisimi
        configuration.setAllowedOriginPatterns(List.of(
                "https://*.trycloudflare.com",
                "http://192.168.*.*:8080"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}