package com.uretimtakip.erp.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Department icin Spring Data JPA repository.
 *
 * Otomatik metotlar (yazmana gerek yok):
 *   save, findById, findAll, delete, count
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    /**
     * Belirli bir siparise bagli departmanlari getir.
     */
    List<Department> findByOrderId(UUID orderId);

    /**
     * Departman adi ile bul.
     */
    Optional<Department> findByName(String name);

    /**
     * Departman adi var mi kontrol et (tekrar olmasin diye).
     */
    boolean existsByName(String name);

    /**
     * Ayni siparis (proje) icindeki isim cakismasi kontrolu.
     * Farkli projelerde ayni bolum adi (ör. "Kaynak") kullanilabilir.
     */
    boolean existsByNameAndOrderId(String name, UUID orderId);

    /**
     * Tum departmanlari sort_order'a gore getir.
     */
    List<Department> findAllByOrderBySortOrderAsc();
}