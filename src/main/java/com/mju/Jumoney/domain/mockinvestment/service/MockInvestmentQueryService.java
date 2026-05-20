package com.mju.Jumoney.domain.mockinvestment.service;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentDashboardResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentSectorLeaderResponse;
import com.mju.Jumoney.domain.mockinvestment.exception.MockInvestmentErrorCode;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.sector.domain.Sector;
import com.mju.Jumoney.domain.sector.exception.SectorErrorCode;
import com.mju.Jumoney.domain.sector.repository.SectorRepository;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockInvestmentQueryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PROFIT_RATE_DIVIDE_SCALE = 6;
    private static final int PROFIT_RATE_DISPLAY_SCALE = 4;

    private final MockInvestmentAccountService mockInvestmentAccountService;
    private final PortfolioRepository portfolioRepository;
    private final SectorRepository sectorRepository;
    private final StockRepository stockRepository;
    private final StockCurrentPriceService stockCurrentPriceService;

    public MockInvestmentDashboardResponse getDashboard(Long userId) {
        Account account = mockInvestmentAccountService.getRequiredAccount(userId);
        List<Portfolio> portfolios = portfolioRepository.findByAccountId(account.getId());
        Map<String, StockCurrentPriceSnapshot> currentPrices = stockCurrentPriceService.getCurrentPrices(
                portfolios.stream()
                        .map(portfolio -> portfolio.getStock().getStockCode())
                        .toList()
        );

        BigDecimal totalEvaluationAmount = portfolios.stream()
                .map(portfolio -> calculateEvaluationAmount(portfolio, currentPrices))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAsset = account.getCashBalance().add(totalEvaluationAmount);
        BigDecimal totalProfitAmount = totalAsset.subtract(account.getSeedMoney());
        BigDecimal totalProfitRate = calculateProfitRate(totalProfitAmount, account.getSeedMoney());

        return new MockInvestmentDashboardResponse(
                account.getId(),
                account.getSeedMoney(),
                account.getCashBalance(),
                account.getTotalPurchaseAmount(),
                totalEvaluationAmount,
                totalAsset,
                totalProfitAmount,
                totalProfitRate
        );
    }

    public MockInvestmentSectorLeaderResponse getSectorLeader(Long sectorId) {
        Sector sector = findSectorById(sectorId);
        Stock stock = findMarketLeaderStock(sectorId);
        StockCurrentPriceSnapshot currentPrice = stockCurrentPriceService.getCurrentPrice(stock.getStockCode())
                .orElse(null);

        return new MockInvestmentSectorLeaderResponse(
                sector.getId(),
                sector.getSectorName().getDescription(),
                stock.getStockCode(),
                stock.getName(),
                currentPrice == null ? null : currentPrice.currentPrice(),
                currentPrice == null ? null : currentPrice.changeRate()
        );
    }

    // ========== 조회 메서드 ==========
    private Sector findSectorById(Long sectorId) {
        return sectorRepository.findById(sectorId)
                .orElseThrow(() -> new CustomException(SectorErrorCode.SECTOR_NOT_FOUND));
    }

    private Stock findMarketLeaderStock(Long sectorId) {
        return stockRepository.findFirstBySectorIdAndIsMarketLeaderTrue(sectorId)
                .orElseThrow(() -> new CustomException(MockInvestmentErrorCode.MARKET_LEADER_STOCK_NOT_FOUND));
    }

    // ========== 비즈니스 메서드 ==========
    private BigDecimal calculateEvaluationAmount(
            Portfolio portfolio,
            Map<String, StockCurrentPriceSnapshot> currentPrices
    ) {
        StockCurrentPriceSnapshot currentPrice = currentPrices.get(portfolio.getStock().getStockCode());
        if (currentPrice == null || currentPrice.currentPrice() == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.currentPrice().multiply(BigDecimal.valueOf(portfolio.getQuantity()));
    }

    private BigDecimal calculateProfitRate(BigDecimal totalProfitAmount, BigDecimal seedMoney) {
        if (seedMoney == null || seedMoney.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return totalProfitAmount.divide(seedMoney, PROFIT_RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(PROFIT_RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
