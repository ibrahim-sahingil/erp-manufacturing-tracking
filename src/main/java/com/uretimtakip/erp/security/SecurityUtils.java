package com.uretimtakip.erp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Servis katmaninda kimlik/rol sorgulari icin yardimci (4. tur K3).
 *
 * JwtAuthFilter her istekte kullaniciyi DB'den yukleyip authority'leri
 * doldurur: permissions listesi verbatim + "ROLE_<rol buyuk harf>".
 * Rota bazli kurallar SecurityConfig'te; alan bazli kurallar (or. "kendi
 * sifreni degistirebilirsin ama rolunu degistiremezsin") servislerde bu
 * sinif uzerinden denetlenir.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** Oturumdaki kullanicinin username'i (auth yoksa null). */
    public static String username() {
        Authentication auth = authentication();
        return auth != null ? auth.getName() : null;
    }

    /** developer rolu var mi? (ROLE_DEVELOPER authority'si) */
    public static boolean isDeveloper() {
        Authentication auth = authentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DEVELOPER".equals(a.getAuthority()));
    }
}
