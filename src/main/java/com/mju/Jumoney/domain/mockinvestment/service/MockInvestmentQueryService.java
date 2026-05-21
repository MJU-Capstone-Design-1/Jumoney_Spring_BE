package com.mju.Jumoney.domain.mockinvestment.service;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import com.mju.Jumoney.domain.mockinvestment.dto.*;
import com.mju.Jumoney.domain.mockinvestment.enums.MockInvestmentStockSearchSortType;
import com.mju.Jumoney.domain.mockinvestment.exception.MockInvestmentErrorCode;
import com.mju.Jumoney.domain.mockinvestment.repository.OrderRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.sector.domain.Sector;
import com.mju.Jumoney.domain.sector.exception.SectorErrorCode;
import com.mju.Jumoney.domain.sector.repository.SectorRepository;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.exception.StockErrorCode;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockInvestmentQueryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int PROFIT_RATE_DIVIDE_SCALE = 6;
    private static final int PROFIT_RATE_DISPLAY_SCALE = 4;

    private final MockInvestmentAccountService mockInvestmentAccountService;
    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final SectorRepository sectorRepository;
    private final StockRepository stockRepository;
    private final StockIndicatorRepository stockIndicatorRepository;
    private final StockCurrentPriceService stockCurrentPriceService;

    public MockInvestmentDashboardResponse getDashboard(Long userId) {
        Account account = mockInvestmentAccountService.getRequiredAccount(userId);
        List<Portfolio> portfolios = portfolioRepository.findByAccountId(account.getId());
        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPricesByPortfolios(portfolios);

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

    public MockInvestmentPortfolioListResponse getPortfolios(Long userId) {
        Account account = mockInvestmentAccountService.getRequiredAccount(userId);
        List<Portfolio> portfolios = portfolioRepository.findByAccountIdOrderByUpdatedAtDesc(account.getId());
        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPricesByPortfolios(portfolios);

        List<MockInvestmentPortfolioItemResponse> portfolioItems = portfolios.stream()
                .map(portfolio -> toPortfolioItemResponse(portfolio, currentPrices.get(portfolio.getStock().getStockCode())))
                .toList();

        return new MockInvestmentPortfolioListResponse(portfolioItems);
    }

    public MockInvestmentSectorStocksResponse getSectorStocks(Long sectorId) {
        Sector sector = findSectorById(sectorId);
        List<Stock> stocks = stockRepository.findBySectorIdOrderByNameAsc(sectorId);
        Map<Long, Long> marketCaps = getLatestMarketCaps(stocks);
        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPricesByStockCodes(
                stocks.stream()
                        .map(Stock::getStockCode)
                        .toList()
        );

        List<MockInvestmentSectorStockItemResponse> stockItems = stocks.stream()
                .sorted(buildSectorStocksComparator(marketCaps))
                .map(stock -> toSectorStockItemResponse(stock, currentPrices.get(stock.getStockCode())))
                .toList();

        return new MockInvestmentSectorStocksResponse(
                sector.getId(),
                sector.getSectorName().getDescription(),
                stockItems
        );
    }

    public MockInvestmentStockDetailResponse getStockDetail(String stockCode) {
        Stock stock = stockRepository.findWithSectorByStockCode(stockCode)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));

        StockCurrentPriceSnapshot currentPrice = stockCurrentPriceService.getCurrentPrice(stock.getStockCode())
                .orElse(null);
        StockIndicator indicator = getLatestIndicator(stock.getId()).orElse(null);

        return new MockInvestmentStockDetailResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                stock.getSector().getSectorName().name(),
                stock.isMarketLeader(),
                buildStockTags(stock),
                new MockInvestmentStockDetailResponse.PriceInfo(
                        currentPrice == null ? null : currentPrice.currentPrice(),
                        currentPrice == null ? null : currentPrice.changeRate(),
                        indicator == null ? null : indicator.getMarketCap(),
                        indicator == null ? null : indicator.getAccumulatedTradeAmount()
                ),
                new MockInvestmentStockDetailResponse.InvestmentMetrics(
                        indicator == null ? null : indicator.getPbr(),
                        indicator == null ? null : indicator.getPer(),
                        indicator == null ? null : indicator.getRoe(),
                        indicator == null ? null : indicator.getDividendYield(),
                        indicator == null ? null : indicator.getPayoutRatio(),
                        indicator == null ? null : indicator.getExecutionStrength(),
                        indicator == null ? null : indicator.getInstNetBuy20Days()
                ),
                new MockInvestmentStockDetailResponse.FinancialMetrics(
                        indicator == null ? null : indicator.getCurrentSales(),
                        indicator == null ? null : indicator.getOperatingProfit(),
                        indicator == null ? null : indicator.getDebtRatio()
                ),
                stock.getDescription() == null ? List.of() : stock.getDescription()
        );
    }

    public MockInvestmentStockSearchResponse searchStocks(String keyword, MockInvestmentStockSearchSortType sort) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return new MockInvestmentStockSearchResponse(normalizedKeyword, List.of());
        }

        List<Stock> stocks = stockRepository.findByNameContainingIgnoreCaseOrderByNameAsc(normalizedKeyword);
        Map<Long, StockIndicator> indicators = getLatestIndicatorsByStockId(stocks);
        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPricesByStockCodes(
                stocks.stream()
                        .map(Stock::getStockCode)
                        .toList()
        );

        List<MockInvestmentSectorStockItemResponse> stockItems = stocks.stream()
                .sorted(buildStockSearchComparator(sort, currentPrices, indicators))
                .map(stock -> toSectorStockItemResponse(stock, currentPrices.get(stock.getStockCode())))
                .toList();

        return new MockInvestmentStockSearchResponse(normalizedKeyword, stockItems);
    }

    public MockInvestmentOrderHistoryResponse getOrderHistory(Long userId) {
        Account account = mockInvestmentAccountService.getRequiredAccount(userId);
        List<MockInvestmentOrderHistoryItemResponse> orderItems = orderRepository.findByAccountIdOrderByExecutedAtDesc(account.getId()).stream()
                .map(this::toOrderHistoryItemResponse)
                .toList();

        return new MockInvestmentOrderHistoryResponse(orderItems);
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

    private Map<String, StockCurrentPriceSnapshot> getCurrentPricesByPortfolios(List<Portfolio> portfolios) {
        return getCurrentPricesByStockCodes(
                portfolios.stream()
                        .map(portfolio -> portfolio.getStock().getStockCode())
                        .toList()
        );
    }

    private Map<String, StockCurrentPriceSnapshot> getCurrentPricesByStockCodes(List<String> stockCodes) {
        if (stockCodes.isEmpty()) {
            return Map.of();
        }
        return stockCurrentPriceService.getCurrentPrices(stockCodes);
    }

    private Map<Long, Long> getLatestMarketCaps(List<Stock> stocks) {
        return getLatestIndicatorsByStockId(stocks).values().stream()
                .collect(Collectors.toMap(
                        indicator -> indicator.getStock().getId(),
                        StockIndicator::getMarketCap
                ));
    }

    private Optional<StockIndicator> getLatestIndicator(Long stockId) {
        return stockIndicatorRepository.findLatestBaseTime()
                .flatMap(baseTime -> stockIndicatorRepository.findByStockIdAndBaseTime(stockId, baseTime));
    }

    private Map<Long, StockIndicator> getLatestIndicatorsByStockId(List<Stock> stocks) {
        if (stocks.isEmpty()) {
            return Map.of();
        }

        Optional<String> latestBaseTime = stockIndicatorRepository.findLatestBaseTime();
        if (latestBaseTime.isEmpty()) {
            return Map.of();
        }

        List<Long> stockIds = stocks.stream()
                .map(Stock::getId)
                .toList();

        return stockIndicatorRepository.findByBaseTimeAndStockIdsWithStock(latestBaseTime.get(), stockIds).stream()
                .collect(Collectors.toMap(
                        indicator -> indicator.getStock().getId(),
                        indicator -> indicator
                ));
    }

    // ========== 비즈니스 메서드 ==========
    private Comparator<Stock> buildSectorStocksComparator(Map<Long, Long> marketCaps) {
        return Comparator
                .comparing(Stock::isMarketLeader, Comparator.reverseOrder())
                .thenComparing(
                        stock -> marketCaps.getOrDefault(stock.getId(), 0L),
                        Comparator.reverseOrder()
                )
                .thenComparing(Stock::getName);
    }

    private Comparator<Stock> buildStockSearchComparator(
            MockInvestmentStockSearchSortType sort,
            Map<String, StockCurrentPriceSnapshot> currentPrices,
            Map<Long, StockIndicator> indicators
    ) {
        MockInvestmentStockSearchSortType effectiveSort = sort == null
                ? MockInvestmentStockSearchSortType.NAME_ASC
                : sort;

        return switch (effectiveSort) {
            case PRICE_DESC -> priceComparator(currentPrices, true);
            case PRICE_ASC -> priceComparator(currentPrices, false);
            case MARKET_CAP_DESC -> indicatorLongComparator(indicators, StockIndicator::getMarketCap);
            case TRADE_AMOUNT_DESC -> indicatorLongComparator(indicators, StockIndicator::getAccumulatedTradeAmount);
            case NAME_ASC -> nameComparator();
        };
    }

    private Comparator<Stock> nameComparator() {
        return Comparator.comparing(Stock::getName)
                .thenComparing(Stock::getStockCode);
    }

    private Comparator<Stock> priceComparator(
            Map<String, StockCurrentPriceSnapshot> currentPrices,
            boolean descending
    ) {
        Comparator<BigDecimal> priceComparator = descending
                ? Comparator.reverseOrder()
                : Comparator.naturalOrder();

        return Comparator
                .comparing((Stock stock) -> currentPriceOf(stock, currentPrices) == null)
                .thenComparing(
                        stock -> currentPriceOf(stock, currentPrices),
                        Comparator.nullsLast(priceComparator)
                )
                .thenComparing(Stock::getName)
                .thenComparing(Stock::getStockCode);
    }

    private Comparator<Stock> indicatorLongComparator(
            Map<Long, StockIndicator> indicators,
            java.util.function.Function<StockIndicator, Long> valueExtractor
    ) {
        return Comparator
                .comparing((Stock stock) -> indicatorValueOf(stock, indicators, valueExtractor) == null)
                .thenComparing(
                        stock -> indicatorValueOf(stock, indicators, valueExtractor),
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(Stock::getName)
                .thenComparing(Stock::getStockCode);
    }

    private BigDecimal currentPriceOf(
            Stock stock,
            Map<String, StockCurrentPriceSnapshot> currentPrices
    ) {
        StockCurrentPriceSnapshot snapshot = currentPrices.get(stock.getStockCode());
        return snapshot == null ? null : snapshot.currentPrice();
    }

    private Long indicatorValueOf(
            Stock stock,
            Map<Long, StockIndicator> indicators,
            java.util.function.Function<StockIndicator, Long> valueExtractor
    ) {
        StockIndicator indicator = indicators.get(stock.getId());
        return indicator == null ? null : valueExtractor.apply(indicator);
    }

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

    private MockInvestmentPortfolioItemResponse toPortfolioItemResponse(
            Portfolio portfolio,
            StockCurrentPriceSnapshot currentPriceSnapshot
    ) {
        BigDecimal currentPrice = currentPriceSnapshot == null ? null : currentPriceSnapshot.currentPrice();
        BigDecimal changeRate = currentPriceSnapshot == null ? null : currentPriceSnapshot.changeRate();
        BigDecimal evaluationAmount = currentPrice == null
                ? ZERO
                : currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
        BigDecimal profitAmount = currentPrice == null
                ? ZERO
                : evaluationAmount.subtract(portfolio.getTotalPurchaseAmount());
        BigDecimal profitRate = currentPrice == null
                ? ZERO
                : calculateProfitRate(profitAmount, portfolio.getTotalPurchaseAmount());

        return new MockInvestmentPortfolioItemResponse(
                portfolio.getStock().getId(),
                portfolio.getStock().getStockCode(),
                portfolio.getStock().getName(),
                portfolio.getStock().getSector().getSectorName().getDescription(),
                portfolio.getQuantity(),
                portfolio.getAveragePurchasePrice(),
                currentPrice,
                evaluationAmount,
                profitAmount,
                profitRate,
                changeRate
        );
    }

    private MockInvestmentSectorStockItemResponse toSectorStockItemResponse(
            Stock stock,
            StockCurrentPriceSnapshot currentPriceSnapshot
    ) {
        return new MockInvestmentSectorStockItemResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                currentPriceSnapshot == null ? null : currentPriceSnapshot.currentPrice(),
                currentPriceSnapshot == null ? null : currentPriceSnapshot.changeRate(),
                stock.isMarketLeader(),
                buildStockTags(stock)
        );
    }

    private List<String> buildStockTags(Stock stock) {
        List<String> tags = new ArrayList<>();
        tags.add(stock.getSector().getSectorName().name());
        if (stock.isMarketLeader()) {
            tags.add("MARKET_LEADER");
        }
        return tags;
    }

    private MockInvestmentOrderHistoryItemResponse toOrderHistoryItemResponse(Order order) {
        return new MockInvestmentOrderHistoryItemResponse(
                order.getId(),
                order.getOrderType().name(),
                order.getStock() == null ? null : order.getStock().getStockCode(),
                order.getStock() == null ? null : order.getStock().getName(),
                order.getExecutionPrice(),
                order.getQuantity(),
                order.getTotalExecutionAmount(),
                order.getExecutedAt()
        );
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
