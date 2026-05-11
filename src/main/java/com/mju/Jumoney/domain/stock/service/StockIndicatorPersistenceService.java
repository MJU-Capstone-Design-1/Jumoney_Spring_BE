package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockIndicatorPersistenceService {

    private final StockIndicatorRepository stockIndicatorRepository;

    // REQUIRES_NEW -> 새로운 트랜잭션 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(StockIndicatorMetrics metrics) {
        StockIndicator stockIndicator = stockIndicatorRepository.findByStockAndBaseTime(metrics.stock(), metrics.baseTime())
                .orElseGet(() -> StockIndicator.create(
                        metrics.stock(),
                        metrics.baseTime(),
                        metrics.marketCap(),
                        metrics.debtRatio(),
                        metrics.operatingProfit(),
                        metrics.operatingProfitGrowthRate(),
                        metrics.dps(),
                        metrics.dividendYield(),
                        metrics.payoutRatio(),
                        metrics.roe(),
                        metrics.per(),
                        metrics.pbr(),
                        metrics.currentEps(),
                        metrics.lastYearEps(),
                        metrics.currentSales(),
                        metrics.lastYearSales(),
                        metrics.marginDebtRate(),
                        metrics.high52WeekRate(),
                        metrics.instNetBuy20Days()
                ));

        stockIndicator.updateMetrics(
                metrics.marketCap(),
                metrics.debtRatio(),
                metrics.operatingProfit(),
                metrics.operatingProfitGrowthRate(),
                metrics.dps(),
                metrics.dividendYield(),
                metrics.payoutRatio(),
                metrics.roe(),
                metrics.per(),
                metrics.pbr(),
                metrics.currentEps(),
                metrics.lastYearEps(),
                metrics.currentSales(),
                metrics.lastYearSales(),
                metrics.marginDebtRate(),
                metrics.high52WeekRate(),
                metrics.instNetBuy20Days()
        );

        stockIndicatorRepository.save(stockIndicator);
    }

    public record StockIndicatorMetrics(
            Stock stock,
            String baseTime,
            Long marketCap,
            BigDecimal debtRatio,
            Long operatingProfit,
            BigDecimal operatingProfitGrowthRate,
            BigDecimal dps,
            BigDecimal dividendYield,
            BigDecimal payoutRatio,
            BigDecimal roe,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal currentEps,
            BigDecimal lastYearEps,
            Long currentSales,
            Long lastYearSales,
            BigDecimal marginDebtRate,
            BigDecimal high52WeekRate,
            Long instNetBuy20Days
    ) {
    }
}
