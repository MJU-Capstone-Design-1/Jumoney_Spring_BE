package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findByStockCodeIn(Collection<String> stockCodes);

    Optional<Stock> findFirstBySectorIdAndIsMarketLeaderTrue(Long sectorId);
}
