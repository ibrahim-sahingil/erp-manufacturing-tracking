package com.uretimtakip.erp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token uretici ve dogrulayici servis.
 *
 * Bu sinif:
 *   1. Login basarili oldugunda yeni token uretir (generateToken)
 *   2. Her request geldiginde token'i dogrular (isTokenValid)
 *   3. Token'dan kullanici adini cikarir (extractUsername)
 *
 * Token icerigi:
 *   - Subject: username
 *   - Custom claims: userId, role, fullName
 *   - Issued at: simdi
 *   - Expiration: simdi + 24 saat (application.properties'ten)
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Token uret. UserDetails icindeki username token'a yazilir.
     * Ekstra bilgiler (role, fullName) extraClaims ile eklenir.
     */
    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Sade versiyon: ekstra claim olmadan token uret.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, new HashMap<>());
    }

    /**
     * Token'in gecerli olup olmadigini kontrol et.
     * - Token bozulmamis mi? (signature dogru mu?)
     * - Suresi gecmis mi?
     * - Username eslesiyor mu?
     * - Hesap hala AKTIF mi? (5. denetim turu)
     *
     * Aktiflik kontrolu olmadan "pasife alma" bir erisim iptali DEGILDI:
     * kullanici pasif edilse bile elindeki token suresi dolana kadar (24 saat)
     * tum API'yi kullanmaya devam ediyordu. JwtAuthFilter kimligi dogrudan
     * kurdugu icin Spring'in hesap-durumu denetimi de devreye girmiyor.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            if (!userDetails.isEnabled()) {
                log.warn("Pasif hesap token kullanmaya calisti: {}", userDetails.getUsername());
                return false;
            }
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Token'dan username'i (subject) cikarir.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Token'dan herhangi bir bilgi cikarmak icin generic metod.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * application.properties'teki secret string'i SecretKey'e cevirir.
     * BASE64 decode + HMAC-SHA256 algoritmasi.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}