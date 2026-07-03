package com.uretimtakip.erp.userpin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * UserPin icin JPA repository.
 */
@Repository
public interface UserPinRepository extends JpaRepository<UserPin, UUID> {

    List<UserPin> findByUserIdOrderByCreatedAtAsc(UUID userId);

    boolean existsByUserIdAndPinTypeAndPinKey(UUID userId, String pinType, String pinKey);
}
