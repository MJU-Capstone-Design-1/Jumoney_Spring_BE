package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockIndicatorRepository extends JpaRepository<StockIndicator, Long> {

    Optional<StockIndicator> findByStockAndBaseTime(Stock stock, String baseTime);

    Optional<StockIndicator> findByStockIdAndBaseTime(Long stockId, String baseTime);

    long countByBaseTime(String baseTime);

    @Query("""
            select si
            from StockIndicator si
            join fetch si.stock
            where si.baseTime = :baseTime
              and si.stock.id in :stockIds
            """)
    List<StockIndicator> findByBaseTimeAndStockIdsWithStock(
            @Param("baseTime") String baseTime,
            @Param("stockIds") List<Long> stockIds
    );

    @Query("""
            select max(si.baseTime)
            from StockIndicator si
            """)
    Optional<String> findLatestBaseTime();

    @Query("""
            select si
            from StockIndicator si
            join fetch si.stock s
            join fetch s.sector
            where si.baseTime = :baseTime
            """)
    List<StockIndicator> findByBaseTimeWithStockAndSector(@Param("baseTime") String baseTime);

    @Query("""
            select si.stock.id
            from StockIndicator si
            where si.baseTime = :baseTime
            order by si.marketCap desc
            """)
    List<Long> findTopMarketCapStockIds(
            @Param("baseTime") String baseTime,
            Pageable pageable
    );

    @Query("""
            select si
            from StockIndicator si
            join fetch si.stock
            where si.baseTime = :baseTime
              and si.stock.id in :stockIds
              and si.debtRatio <= :maxDebtRatio
              and si.operatingProfit >= 0
            """)
    List<StockIndicator> findCapitalProtectionCandidatesInTopMarketCapStocks(
            @Param("baseTime") String baseTime,
            @Param("stockIds") List<Long> stockIds,
            @Param("maxDebtRatio") BigDecimal maxDebtRatio
    );

    @Query("""
            select si
            from StockIndicator si
            join fetch si.stock
            where si.baseTime = :baseTime
              and si.dividendYield between :minDividendYield and :maxDividendYield
              and si.payoutRatio is not null
              and si.payoutRatio between :minPayoutRatio and :maxPayoutRatio
            """)
    List<StockIndicator> findDividendIncomeCandidates(
            @Param("baseTime") String baseTime,
            @Param("minDividendYield") BigDecimal minDividendYield,
            @Param("maxDividendYield") BigDecimal maxDividendYield,
            @Param("minPayoutRatio") BigDecimal minPayoutRatio,
            @Param("maxPayoutRatio") BigDecimal maxPayoutRatio
    );

    @Query("""
            select si
            from StockIndicator si
            join fetch si.stock
            where si.baseTime = :baseTime
              and si.roe >= :minRoe
              and si.lastYearEps is not null
              and si.lastYearEps > 0
              and ((si.currentEps - si.lastYearEps) / si.lastYearEps * 100) >= :minEpsGrowthRate
            """)
    List<StockIndicator> findSteadyGrowthCandidates(
            @Param("baseTime") String baseTime,
            @Param("minRoe") BigDecimal minRoe,
            @Param("minEpsGrowthRate") BigDecimal minEpsGrowthRate
    );

    @Query("""
            select si
            from StockIndicator si
            join fetch si.stock
            where si.baseTime = :baseTime
              and si.per > 0
              and si.per <= :maxPer
              and si.pbr <= :maxPbr
              and si.operatingProfitGrowthRate >= 0
            """)
    List<StockIndicator> findCapitalGainCandidates(
            @Param("baseTime") String baseTime,
            @Param("maxPer") BigDecimal maxPer,
            @Param("maxPbr") BigDecimal maxPbr
    );

    @Query("""
            select s
            from Stock s
            where not exists (
                select 1
                from StockIndicator si
                where si.stock = s
                  and si.baseTime = :baseTime
            )
            order by s.stockCode
            """)
    List<Stock> findStocksWithoutIndicator(@Param("baseTime") String baseTime);

    @Query("""
            select count(si)
            from StockIndicator si
            where si.baseTime = :baseTime
              and (
                   si.marketCap is null
                or si.accumulatedTradeAmount is null
                or si.executionStrength is null
                or si.debtRatio is null
                or si.operatingProfit is null
                or si.operatingProfitGrowthRate is null
                or si.dps is null
                or si.dividendYield is null
                or si.roe is null
                or si.per is null
                or si.pbr is null
                or si.currentEps is null
                or si.currentSales is null
                or si.marginDebtRate is null
                or si.high52WeekRate is null
                or si.instNetBuy20Days is null
              )
            """)
    long countInvalidRequiredFieldsByBaseTime(@Param("baseTime") String baseTime);
}
