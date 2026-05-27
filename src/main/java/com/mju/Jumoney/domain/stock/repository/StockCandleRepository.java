package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockCandleRepository extends JpaRepository<StockCandle, Long> {

    Optional<StockCandle> findByStockIdAndIntervalTypeAndCandleTime(Long stockId,
                                                                    StockCandleIntervalType intervalType,
                                                                    LocalDateTime candleTime);

    List<StockCandle> findByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(Long stockId,
                                                                                           StockCandleIntervalType intervalType,
                                                                                           LocalDateTime startTime,
                                                                                           LocalDateTime endTime);

    long countByStockIdAndIntervalTypeAndCandleTimeBetween(Long stockId,
                                                           StockCandleIntervalType intervalType,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime);

    Optional<StockCandle> findFirstByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(Long stockId,
                                                                                                    StockCandleIntervalType intervalType,
                                                                                                    LocalDateTime startTime,
                                                                                                    LocalDateTime endTime);

    Optional<StockCandle> findFirstByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeDesc(Long stockId,
                                                                                                     StockCandleIntervalType intervalType,
                                                                                                     LocalDateTime startTime,
                                                                                                     LocalDateTime endTime);

    Optional<StockCandle> findFirstByStockIdAndIntervalTypeOrderByCandleTimeDesc(Long stockId,
                                                                                 StockCandleIntervalType intervalType);
}
