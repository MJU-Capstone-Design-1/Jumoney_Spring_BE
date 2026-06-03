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
        StockIndicator existing = stockIndicatorRepository.findByStockAndBaseTime(metrics.stock(), metrics.baseTime())
                .orElse(null);
        if (existing == null) {
            stockIndicatorRepository.save(StockIndicator.create(
                    metrics.stock(),
                    metrics.baseTime(),
                    metrics.marketCap(),
                    metrics.accumulatedTradeAmount(),
                    metrics.executionStrength(),
                    metrics.debtRatio(),
                    metrics.operatingProfit(),
                    metrics.operatingProfitGrowthRate(),
                    metrics.salesGrowthRate(),
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
            return;
        }

        existing.updateMetrics(
                metrics.marketCap(),
                metrics.accumulatedTradeAmount(),
                metrics.executionStrength(),
                metrics.debtRatio(),
                metrics.operatingProfit(),
                metrics.operatingProfitGrowthRate(),
                metrics.salesGrowthRate(),
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
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateExecutionStrength(Stock stock, String baseTime, BigDecimal executionStrength) {
        StockIndicator existing = stockIndicatorRepository.findByStockAndBaseTime(stock, baseTime)
                .orElse(null);
        if (existing == null) {
            return false;
        }

        existing.updateExecutionStrength(executionStrength);
        return true;
    }

    public record StockIndicatorMetrics(
            Stock stock,
            String baseTime,
            Long marketCap,
            Long accumulatedTradeAmount,
            BigDecimal executionStrength,
            BigDecimal debtRatio,
            Long operatingProfit,
            BigDecimal operatingProfitGrowthRate,
            BigDecimal salesGrowthRate,
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
