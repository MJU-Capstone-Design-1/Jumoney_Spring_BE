package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockIndicatorRepository extends JpaRepository<StockIndicator, Long> {

    Optional<StockIndicator> findByStockAndBaseTime(Stock stock, String baseTime);

    Optional<StockIndicator> findByStockIdAndBaseTime(Long stockId, String baseTime);

    long countByBaseTime(String baseTime);

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
