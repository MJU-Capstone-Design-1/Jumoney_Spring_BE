package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockIndicatorRepository extends JpaRepository<StockIndicator, Long> {

    Optional<StockIndicator> findByStockAndBaseTime(Stock stock, String baseTime);

    Optional<StockIndicator> findByStockIdAndBaseTime(Long stockId, String baseTime);
}
