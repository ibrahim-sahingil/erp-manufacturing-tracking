package com.uretimtakip.erp.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, UUID> {

    /** Tek satirlik tablo — en eski (ilk) kayit "the" ayar satiridir. */
    Optional<CompanySettings> findFirstByOrderByCreatedAtAsc();
}
