package com.mju.Jumoney.domain.home.service;

import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentChartResponse;
import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentSummaryResponse;
import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentChartResponse;
import com.mju.Jumoney.domain.mockinvestment.enums.MockInvestmentChartPeriod;
import com.mju.Jumoney.domain.mockinvestment.enums.OrderType;
import com.mju.Jumoney.domain.mockinvestment.repository.AccountRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.OrderRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentQueryService;
import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import com.mju.Jumoney.domain.stock.repository.StockCandleRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeMockInvestmentQueryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PROFIT_RATE_DIVIDE_SCALE = 6;
    private static final int PROFIT_RATE_DISPLAY_SCALE = 4;

    private final AccountRepository accountRepository;
    private final PortfolioRepository portfolioRepository;
    private final OrderRepository orderRepository;
    private final StockCurrentPriceService stockCurrentPriceService;
    private final StockCandleRepository stockCandleRepository;
    private final MockInvestmentQueryService mockInvestmentQueryService;

    public HomeMockInvestmentSummaryResponse getSummary(Long userId) {
        Optional<Account> account = accountRepository.findByUserId(userId);
        if (account.isEmpty()) {
            return new HomeMockInvestmentSummaryResponse(false, null, null, null, null);
        }

        List<Portfolio> portfolios = portfolioRepository.findByAccountId(account.get().getId());
        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPricesWithFallback(portfolios);
        EvaluationSummary evaluationSummary = evaluatePortfolios(portfolios, currentPrices, account.get().getTotalPurchaseAmount());
        Map<Long, LocalDateTime> firstBuyExecutedAtByStockId = getFirstBuyExecutedAtByStockId(account.get().getId());
        Portfolio topHolding = findTopHolding(portfolios, firstBuyExecutedAtByStockId).orElse(null);

        return new HomeMockInvestmentSummaryResponse(
                true,
                account.get().getTotalPurchaseAmount(),
                evaluationSummary.totalProfitAmount(),
                evaluationSummary.totalProfitRate(),
                topHolding == null ? null : toTopHolding(topHolding, currentPrices.get(topHolding.getStock().getStockCode()))
        );
    }

    public HomeMockInvestmentChartResponse getSummaryChart(Long userId) {
        Optional<Account> account = accountRepository.findByUserId(userId);
        if (account.isEmpty()) {
            return new HomeMockInvestmentChartResponse(false, null, null, null, null, null, List.of());
        }

        List<Portfolio> portfolios = portfolioRepository.findByAccountId(account.get().getId());
        Map<Long, LocalDateTime> firstBuyExecutedAtByStockId = getFirstBuyExecutedAtByStockId(account.get().getId());
        Optional<Portfolio> topHolding = findTopHolding(portfolios, firstBuyExecutedAtByStockId);
        if (topHolding.isEmpty()) {
            return new HomeMockInvestmentChartResponse(true, null, null, null, null, null, List.of());
        }

        Portfolio portfolio = topHolding.get();
        try {
            MockInvestmentChartResponse chartResponse = mockInvestmentQueryService.getChart(
                    portfolio.getStock().getStockCode(),
                    MockInvestmentChartPeriod.ONE_DAY,
                    null
            );
            return new HomeMockInvestmentChartResponse(
                    true,
                    portfolio.getStock().getId(),
                    portfolio.getStock().getStockCode(),
                    portfolio.getStock().getName(),
                    chartResponse.date(),
                    chartResponse.includesRealtime(),
                    chartResponse.candles().stream()
                            .map(this::toHomeChartCandle)
                            .toList()
            );
        } catch (RuntimeException e) {
            log.warn("[HomeMockInvestment] 대표 종목 차트 조회 실패. userId={}, stockCode={}",
                    userId, portfolio.getStock().getStockCode(), e);
            return new HomeMockInvestmentChartResponse(
                    true,
                    portfolio.getStock().getId(),
                    portfolio.getStock().getStockCode(),
                    portfolio.getStock().getName(),
                    null,
                    null,
                    List.of()
            );
        }
    }

    // ========== 조회 메서드 ==========
    private Map<String, StockCurrentPriceSnapshot> getCurrentPricesWithFallback(List<Portfolio> portfolios) {
        Map<String, StockCurrentPriceSnapshot> currentPrices = new LinkedHashMap<>(
                stockCurrentPriceService.getCurrentPrices(
                        portfolios.stream()
                                .map(portfolio -> portfolio.getStock().getStockCode())
                                .toList()
                )
        );

        for (Portfolio portfolio : portfolios) {
            String stockCode = portfolio.getStock().getStockCode();
            if (!currentPrices.containsKey(stockCode)) {
                resolveFallbackCurrentPrice(portfolio).ifPresent(snapshot -> currentPrices.put(stockCode, snapshot));
            }
        }
        return currentPrices;
    }

    private Optional<Portfolio> findTopHolding(List<Portfolio> portfolios, Map<Long, LocalDateTime> firstBuyExecutedAtByStockId) {
        if (portfolios.isEmpty()) {
            return Optional.empty();
        }

        return portfolios.stream()
                .max(Comparator.comparing(Portfolio::getTotalPurchaseAmount)
                        .thenComparing(portfolio -> firstBuyExecutedAtByStockId.get(portfolio.getStock().getId()),
                                Comparator.nullsFirst(Comparator.reverseOrder()))
                        .thenComparing(portfolio -> portfolio.getStock().getId(), Comparator.reverseOrder()));
    }

    private Map<Long, LocalDateTime> getFirstBuyExecutedAtByStockId(Long accountId) {
        Map<Long, LocalDateTime> firstBuyExecutedAtByStockId = new HashMap<>();
        for (Order order : orderRepository.findByAccountIdAndOrderTypeOrderByExecutedAtAsc(accountId, OrderType.BUY)) {
            if (order.getStock() != null) {
                firstBuyExecutedAtByStockId.putIfAbsent(order.getStock().getId(), order.getExecutedAt());
            }
        }
        return firstBuyExecutedAtByStockId;
    }

    private Optional<StockCurrentPriceSnapshot> resolveFallbackCurrentPrice(Portfolio portfolio) {
        return stockCandleRepository.findFirstByStockIdAndIntervalTypeOrderByCandleTimeDesc(
                        portfolio.getStock().getId(),
                        StockCandleIntervalType.DAY
                )
                .map(StockCandle::getClosePrice)
                .map(closePrice -> new StockCurrentPriceSnapshot(closePrice, null));
    }

    // ========== 계산 메서드 ==========
    private EvaluationSummary evaluatePortfolios(List<Portfolio> portfolios,
                                                 Map<String, StockCurrentPriceSnapshot> currentPrices,
                                                 BigDecimal totalPurchaseAmount) {
        BigDecimal totalEvaluationAmount = BigDecimal.ZERO;
        boolean hasUnknownPrice = false;

        for (Portfolio portfolio : portfolios) {
            BigDecimal evaluationAmount = calculateEvaluationAmount(portfolio, currentPrices);
            if (evaluationAmount == null) {
                hasUnknownPrice = true;
                continue;
            }
            totalEvaluationAmount = totalEvaluationAmount.add(evaluationAmount);
        }

        if (hasUnknownPrice) {
            return new EvaluationSummary(null, null);
        }

        BigDecimal totalProfitAmount = totalEvaluationAmount.subtract(totalPurchaseAmount);
        BigDecimal totalProfitRate = calculateProfitRate(totalProfitAmount, totalPurchaseAmount);
        return new EvaluationSummary(totalProfitAmount, totalProfitRate);
    }

    private BigDecimal calculateEvaluationAmount(Portfolio portfolio, Map<String, StockCurrentPriceSnapshot> currentPrices) {
        StockCurrentPriceSnapshot snapshot = currentPrices.get(portfolio.getStock().getStockCode());
        BigDecimal currentPrice = snapshot == null ? null : snapshot.currentPrice();
        if (currentPrice == null) {
            return null;
        }
        return currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
    }

    private BigDecimal calculateProfitRate(BigDecimal profitAmount, BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return profitAmount.divide(baseAmount, PROFIT_RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(PROFIT_RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    private HomeMockInvestmentSummaryResponse.TopHolding toTopHolding(Portfolio portfolio,
                                                                      StockCurrentPriceSnapshot currentPriceSnapshot) {
        BigDecimal currentPrice = currentPriceSnapshot == null ? null : currentPriceSnapshot.currentPrice();
        BigDecimal profitAmount = null;
        BigDecimal profitRate = null;
        if (currentPrice != null) {
            BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
            profitAmount = evaluationAmount.subtract(portfolio.getTotalPurchaseAmount());
            profitRate = calculateProfitRate(profitAmount, portfolio.getTotalPurchaseAmount());
        }

        return new HomeMockInvestmentSummaryResponse.TopHolding(
                portfolio.getStock().getId(),
                portfolio.getStock().getStockCode(),
                portfolio.getStock().getName(),
                portfolio.getTotalPurchaseAmount(),
                profitAmount,
                profitRate
        );
    }

    private HomeMockInvestmentChartResponse.Candle toHomeChartCandle(MockInvestmentChartResponse.Candle candle) {
        return new HomeMockInvestmentChartResponse.Candle(
                candle.candleTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice()
        );
    }

    private record EvaluationSummary(
            BigDecimal totalProfitAmount,
            BigDecimal totalProfitRate
    ) {
    }
}
