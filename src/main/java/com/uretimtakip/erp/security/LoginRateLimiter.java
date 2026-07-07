package com.uretimtakip.erp.security;

import com.uretimtakip.erp.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basit login hiz siniri (4. tur U5).
 *
 * Amac: tunel disariya acikken (cloudflared) sifre brute-force denemesini
 * yavaslatmak. IP basina ardisik BASARISIZ giris sayilir; esik asilinca IP
 * kisa sure bloklanir. BASARILI giris sayaci sifirlar (AuthController).
 *
 * Bellek: in-memory ConcurrentHashMap; kucuk olcekte (tek sirket) yeterli,
 * ayri bir store/temizlik gerekmez. Basarili girisler kaydi siler.
 *
 * NOT: forwarded-headers-strategy=framework oldugundan getRemoteAddr()
 * proxy/tunel arkasindan gercek IP'yi verir. Tunelde tum istekler tek
 * IP'den gelebilir (o zaman blok tum kullanicilari etkiler) — kucuk
 * olcekte kabul edilebilir; brute-force korumasi oncelikli.
 */
@Component
public class LoginRateLimiter {

    /** Pencere icinde bu kadar basarisiz denemeden sonra IP bloklanir. */
    private static final int MAX_FAILURES = 10;
    /** Basarisizlik penceresi: bu sureden eski sayac sifirlanir. */
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    /** Blok suresi. */
    private static final long LOCKOUT_MS = 15 * 60 * 1000L;

    private static final class Attempt {
        int failures;
        long firstFailureAt;
        long lockedUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** Login denemesinden ONCE cagrilir; IP bloklu ise hata firlatir. */
    public synchronized void assertNotBlocked(String ip) {
        Attempt a = attempts.get(ip);
        if (a == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (a.lockedUntil > now) {
            long mins = Math.max(1, (a.lockedUntil - now + 59_999) / 60_000);
            throw new BusinessException(
                    "Cok fazla basarisiz giris denemesi. Lutfen " + mins
                            + " dakika sonra tekrar deneyin.",
                    "TOO_MANY_LOGIN_ATTEMPTS");
        }
    }

    /** Basarisiz login sonrasi cagrilir; esik asilirsa IP'yi bloklar. */
    public synchronized void recordFailure(String ip) {
        long now = System.currentTimeMillis();
        Attempt a = attempts.computeIfAbsent(ip, k -> new Attempt());
        if (a.failures == 0 || now - a.firstFailureAt > WINDOW_MS) {
            a.failures = 0;
            a.firstFailureAt = now;
        }
        a.failures++;
        if (a.failures >= MAX_FAILURES) {
            a.lockedUntil = now + LOCKOUT_MS;
            a.failures = 0; // blok sonrasi temiz baslasin
        }
    }

    /** Basarili login sonrasi cagrilir; IP'nin sayacini/blokunu temizler. */
    public synchronized void reset(String ip) {
        attempts.remove(ip);
    }
}
