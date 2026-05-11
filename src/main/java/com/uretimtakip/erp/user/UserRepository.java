package com.uretimtakip.erp.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User icin Spring Data JPA repository.
 *
 * Spring otomatik olarak su metotlari sunar (yazmana gerek yok):
 *   - save(User)        : kaydet veya guncelle
 *   - findById(UUID)    : id ile bul
 *   - findAll()         : tumunu getir
 *   - delete(User)      : sil
 *   - count()           : toplam sayi
 *
 * Asagidaki metotlar bizim ozel sorgularimiz - method ismine bakip
 * Spring otomatik SQL uretiyor.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Kullanici adi ile bul (login icin).
     */
    Optional<User> findByUsername(String username);

    /**
     * Sadece aktif kullanicilari getir.
     */
    List<User> findByIsActiveTrue();

    /**
     * Username daha once kullanilmis mi? (kayit sirasinda kontrol icin)
     */
    boolean existsByUsername(String username);

    /**
     * Belirli bir role'deki kullanicilari getir.
     * Ornek: findByRole("Operator")
     */
    List<User> findByRole(String role);

    /**
     * Belirli bir departmandaki aktif kullanicilari getir.
     */
    List<User> findByDepartmentAndIsActiveTrue(String department);
}