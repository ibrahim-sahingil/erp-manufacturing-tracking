package com.uretimtakip.erp.carrier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, UUID> {

    List<Carrier> findAllByOrderByNameAsc();

    List<Carrier> findByIsActiveTrueOrderByNameAsc();
}
