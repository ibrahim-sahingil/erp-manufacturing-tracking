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
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
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
                .authorizeHttpRequests(auth -> {
                    // Public endpoint'ler (token gerektirmez)
                    auth.requestMatchers("/api/auth/**").permitAll();
                    auth.requestMatchers("/api/config").permitAll(); // (E3) QR base-url; hassas veri yok
                    auth.requestMatchers("/error").permitAll();
                    auth.requestMatchers("/actuator/health").permitAll();

                    // Statik frontend (static/index.html) - login ekrani
                    // API cagrilari yine JWT ister
                    auth.requestMatchers("/", "/index.html", "/favicon.ico").permitAll();

                    // (K3) Yikici uclar frontend'deki gorunurluk kurallariyla
                    // AYNI sartla backend'de de denetlenir — JWT'si olan
                    // herkes API'ye dogrudan istek atabilir (tunel acik).
                    // Siparis/proje yazma: developer VEYA orders yetkisi
                    // (frontend canEdit ile birebir; GET herkese acik kalir).
                    writeRule(auth, new String[]{"/api/orders/**"},
                            "ROLE_DEVELOPER", "orders_edit", "orders");
                    // Kullanici yazma uclari authenticated kalir; hesap/rol/
                    // yetki degisiklikleri UserService'te alan bazli denetlenir
                    // (personel karti ekleme + kendi sifreni degistirme serbest).

                    // ═══ (5. denetim turu) MODUL BAZLI YAZMA KILIDI ═══
                    // Sorun: K3 turu yalnizca orders + users icin kilit koydu;
                    // diger 12 modulun POST/PUT/DELETE uclari "sadece giris
                    // yapmis olmak" ile acikti. Sadece 'dashboard' yetkili bir
                    // isci hesabi, tarayici konsolundan urun agacini, depolari,
                    // malzeme/tedarikci kartlarini SILEBILIYORDU (canli olarak
                    // dogrulandi: 11 acik kapi).
                    //
                    // Kural: YAZMA (POST/PUT/DELETE) ilgili SEKME yetkisini ister
                    // (yetki adlari = frontend sekme adlari), OKUMA (GET) giris
                    // yapmis herkese acik kalir. Boylece panolar/QR akisi kirilmaz.
                    //
                    // KAPSAM DISI (bilincli): uretim akisi uclari — part-logs,
                    // parts, work-orders*, warehouse-movements, purchase-items.
                    // Bunlari QR okutan isci ve saha kullanicilari yaziyor; kilit
                    // eslemesi netlesmeden kapatmak gunluk isi durdurur.
                    writeRule(auth, new String[]{"/api/bom-products/**", "/api/bom-parts/**",
                                    "/api/bom-operations/**", "/api/project-bom/**",
                                    "/api/project-bom-parts/**"},
                            "ROLE_DEVELOPER", "bom");
                    writeRule(auth, new String[]{"/api/bom-documents/**"},
                            "ROLE_DEVELOPER", "bom", "docs");
                    writeRule(auth, new String[]{"/api/warehouses/**"},
                            "ROLE_DEVELOPER", "warehouse");
                    writeRule(auth, new String[]{"/api/purchase-orders/**",
                                    "/api/purchase-order-quotes/**"},
                            "ROLE_DEVELOPER", "purchasing");
                    writeRule(auth, new String[]{"/api/delivery-notes/**",
                                    "/api/delivery-note-items/**"},
                            "ROLE_DEVELOPER", "delivery");
                    writeRule(auth, new String[]{"/api/project-dates/**",
                                    "/api/project-date-revisions/**", "/api/stock-sheets/**"},
                            "ROLE_DEVELOPER", "planning");
                    // Kartotekler: birden fazla ekrandan "listede yoksa ekle"
                    // deseniyle otomatik olusturuluyor (ensureSupplier vb.) —
                    // bu yuzden ilgili tum sekmelere acik, ama yetkisize kapali.
                    writeRule(auth, new String[]{"/api/materials/**"},
                            "ROLE_DEVELOPER", "bom", "purchasing", "warehouse");
                    writeRule(auth, new String[]{"/api/suppliers/**"},
                            "ROLE_DEVELOPER", "purchasing", "delivery", "warehouse");
                    // Yonetimsel tanimlar: yalnizca gelistirici
                    writeRule(auth, new String[]{"/api/departments/**", "/api/workspaces/**",
                                    "/api/workspace-members/**"},
                            "ROLE_DEVELOPER");

                    // DENETIM IZI: uretim gecmisi (part_logs) SILME yalnizca
                    // gelistiricide. POST acik kalir — QR okutan isci uretim
                    // kaydi yazar. Frontend bu DELETE ucunu hic kullanmiyor,
                    // dolayisiyla kimsenin gunluk isi kirilmaz. PartLogService'te
                    // guard yok: silinen kayit geri gelmez, iz de birakmaz.
                    auth.requestMatchers(HttpMethod.DELETE, "/api/part-logs/**")
                            .hasAuthority("ROLE_DEVELOPER");

                    // Diger her endpoint icin authentication gerekli
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Yazma sayilan HTTP metodlari (PATCH kullanilmiyor — hicbir controller'da yok). */
    private static final HttpMethod[] WRITE_METHODS = {
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE
    };

    /**
     * Verilen yollarin TUM yazma metodlarina ayni yetki kuralini uygular.
     * requestMatchers(HttpMethod, String...) tek bir metod aldigindan, kural
     * POST/PUT/DELETE icin ayri ayri kaydedilir. GET'e dokunulmaz — okuma
     * "authenticated" olarak anyRequest() kuralina duser.
     */
    private static void writeRule(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            String[] paths, String... authorities) {
        for (HttpMethod method : WRITE_METHODS) {
            auth.requestMatchers(method, paths).hasAnyAuthority(authorities);
        }
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

        // Desen bazli izinler: Cloudflare Tunnel (demo), Tailscale (kalici
        // test erisimi — tailscale serve, 2026-07-09) ve yerel ag erisimi
        configuration.setAllowedOriginPatterns(List.of(
                "https://*.trycloudflare.com",
                "https://*.ts.net",
                "http://192.168.*.*:8080",
                "http://100.*:8080"
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