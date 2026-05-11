package com.uretimtakip.erp.security;

import com.uretimtakip.erp.user.User;
import com.uretimtakip.erp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Boot HER ACILDIGINDA bu sinif calisir.
 *
 * Gorevi:
 *   - DB'deki tum kullanicilarin sifrelerini kontrol et
 *   - Eger sifre DUZ METIN ise (BCrypt formatinda degil)
 *   - Onu BCrypt ile hash'le ve geri kaydet
 *
 * Boylece:
 *   - admin / 1234     -> ilk acilista BCrypt'e cevirir
 *   - yonetici / 4321  -> BCrypt'e cevirir
 *   - ali.yilmaz / sifre1234 -> BCrypt'e cevirir
 *
 * Sonraki acilislarda zaten BCrypt formatinda olduklari icin atlar.
 *
 * BCrypt formatinda olanlar "$2a$..." ile baslar, kontrolu kolay.
 *
 * @Order(1): En once calissin (diger runner'lardan once).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Password Migration Runner Started ===");

        List<User> users = userRepository.findAll();
        int migratedCount = 0;
        int skippedCount = 0;

        for (User user : users) {
            String currentPassword = user.getPasswordHash();

            if (currentPassword == null || currentPassword.isBlank()) {
                log.warn("User '{}' has no password, skipping", user.getUsername());
                skippedCount++;
                continue;
            }

            // BCrypt formatinda mi kontrol et
            // BCrypt hash'leri "$2a$", "$2b$" veya "$2y$" ile baslar ve 60 karakter
            if (isBCryptHash(currentPassword)) {
                log.debug("User '{}' password already BCrypt encoded, skipping", user.getUsername());
                skippedCount++;
                continue;
            }

            // Duz metin sifreyi BCrypt'e cevir
            String hashedPassword = passwordEncoder.encode(currentPassword);
            user.setPasswordHash(hashedPassword);
            userRepository.save(user);

            log.info("Migrated password for user '{}' (was plain text, now BCrypt)",
                    user.getUsername());
            migratedCount++;
        }

        log.info("=== Password Migration Completed ===");
        log.info("  Migrated: {} users", migratedCount);
        log.info("  Skipped:  {} users (already BCrypt or no password)", skippedCount);
        log.info("  Total:    {} users", users.size());
    }

    /**
     * Verilen string BCrypt hash formatinda mi kontrol eder.
     * BCrypt: $2a$10$... veya $2b$10$... (60 karakter)
     */
    private boolean isBCryptHash(String password) {
        if (password == null || password.length() != 60) {
            return false;
        }
        return password.startsWith("$2a$") ||
                password.startsWith("$2b$") ||
                password.startsWith("$2y$");
    }
}