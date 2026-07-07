package com.uretimtakip.erp.stocksheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockSheetRepository extends JpaRepository<StockSheet, UUID> {

    List<StockSheet> findAllByOrderByKindAscNameAsc();

    List<StockSheet> findByKindOrderByNameAsc(String kind);
}
