package com.mju.Jumoney.domain.home.service;

import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentRankingsResponse;
import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentSummaryResponse.TopHolding;
import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import com.mju.Jumoney.domain.mockinvestment.enums.OrderType;
import com.mju.Jumoney.domain.mockinvestment.repository.AccountRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.OrderRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.ranking.domain.UserRanking;
import com.mju.Jumoney.domain.ranking.repository.UserRankingRepository;
import com.mju.Jumoney.domain.ranking.service.MockInvestmentRankingUpdateService;
import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import com.mju.Jumoney.domain.stock.repository.StockCandleRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeMockInvestmentRankingQueryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PROFIT_RATE_DIVIDE_SCALE = 6;
    private static final int PROFIT_RATE_DISPLAY_SCALE = 4;
    private static final int SECTION_LIMIT = 5;
    private static final int REPRESENTATIVE_STOCK_LIMIT = 3;

    private final UserRankingRepository userRankingRepository;
    private final MasterRepository masterRepository;
    private final AccountRepository accountRepository;
    private final PortfolioRepository portfolioRepository;
    private final OrderRepository orderRepository;
    private final StockCurrentPriceService stockCurrentPriceService;
    private final StockCandleRepository stockCandleRepository;
    private final MockInvestmentRankingUpdateService mockInvestmentRankingUpdateService;

    @Transactional
    public HomeMockInvestmentRankingsResponse getRankings() {
        List<UserRanking> overallRankings = loadOverallRankingsEnsuringSnapshot();
        List<Master> masters = masterRepository.findAllByOrderByDisplayOrderAsc();

        Set<Long> userIds = new LinkedHashSet<>();
        overallRankings.stream()
                .map(userRanking -> userRanking.getUser().getId())
                .forEach(userIds::add);

        Map<Long, List<UserRanking>> masterRankingsByMasterId = new LinkedHashMap<>();
        for (Master master : masters) {
            List<UserRanking> masterRankings = userRankingRepository.findTopByMasterIdWithUserAndMaster(master.getId(), SECTION_LIMIT);
            masterRankingsByMasterId.put(master.getId(), masterRankings);
            masterRankings.stream()
                    .map(userRanking -> userRanking.getUser().getId())
                    .forEach(userIds::add);
        }

        RankingContext context = buildContext(userIds);

        return new HomeMockInvestmentRankingsResponse(
                toOverallSection(overallRankings, context),
                masters.stream()
                        .map(master -> toMasterSection(master, masterRankingsByMasterId.getOrDefault(master.getId(), List.of()), context))
                        .toList()
        );
    }

    private List<UserRanking> loadOverallRankingsEnsuringSnapshot() {
        List<UserRanking> overallRankings = userRankingRepository.findTopOverallWithUserAndMaster(SECTION_LIMIT);
        if (!overallRankings.isEmpty()) {
            return overallRankings;
        }

        mockInvestmentRankingUpdateService.refreshRankings();
        return userRankingRepository.findTopOverallWithUserAndMaster(SECTION_LIMIT);
    }

    private RankingContext buildContext(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return new RankingContext(Map.of(), Map.of(), Map.of(), Map.of());
        }

        Map<Long, Account> accountsByUserId = accountRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(account -> account.getUser().getId(), Function.identity()));
        List<Long> accountIds = accountsByUserId.values().stream()
                .map(Account::getId)
                .toList();

        Map<Long, List<Portfolio>> portfoliosByAccountId = portfolioRepository.findByAccountIdIn(accountIds).stream()
                .collect(Collectors.groupingBy(portfolio -> portfolio.getAccount().getId()));
        Map<Long, Map<Long, LocalDateTime>> firstBuyExecutedAtByAccountId = getFirstBuyExecutedAtByAccountId(accountIds);
        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPricesWithFallback(
                portfoliosByAccountId.values().stream()
                        .flatMap(List::stream)
                        .toList()
        );

        return new RankingContext(accountsByUserId, portfoliosByAccountId, firstBuyExecutedAtByAccountId, currentPrices);
    }

    private Map<Long, Map<Long, LocalDateTime>> getFirstBuyExecutedAtByAccountId(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<Long, LocalDateTime>> firstBuyExecutedAtByAccountId = new HashMap<>();
        for (Order order : orderRepository.findByAccountIdInAndOrderTypeOrderByExecutedAtAsc(accountIds, OrderType.BUY)) {
            if (order.getStock() == null) {
                continue;
            }
            firstBuyExecutedAtByAccountId
                    .computeIfAbsent(order.getAccount().getId(), ignored -> new HashMap<>())
                    .putIfAbsent(order.getStock().getId(), order.getExecutedAt());
        }
        return firstBuyExecutedAtByAccountId;
    }

    private HomeMockInvestmentRankingsResponse.RankingSection toOverallSection(List<UserRanking> rankings, RankingContext context) {
        return new HomeMockInvestmentRankingsResponse.RankingSection(
                "OVERALL",
                null,
                null,
                null,
                rankings.isEmpty() ? null : rankings.get(0).getRankingDate(),
                rankings.stream()
                        .map(ranking -> toRankingUser(ranking, ranking.getOverallRank(), context))
                        .toList()
        );
    }

    private HomeMockInvestmentRankingsResponse.RankingSection toMasterSection(Master master,
                                                                              List<UserRanking> rankings,
                                                                              RankingContext context) {
        return new HomeMockInvestmentRankingsResponse.RankingSection(
                "MASTER",
                master.getId(),
                master.getMasterCode().name(),
                master.getMasterName(),
                rankings.isEmpty() ? null : rankings.get(0).getRankingDate(),
                rankings.stream()
                        .map(ranking -> toRankingUser(ranking, ranking.getMasterRank(), context))
                        .toList()
        );
    }

    private HomeMockInvestmentRankingsResponse.RankingUser toRankingUser(UserRanking ranking,
                                                                         Integer rank,
                                                                         RankingContext context) {
        User user = ranking.getUser();
        String nickname = user.getServiceNickname() != null ? user.getServiceNickname() : user.getNickname();
        Account account = context.accountsByUserId().get(user.getId());
        List<Portfolio> portfolios = account == null
                ? List.of()
                : context.portfoliosByAccountId().getOrDefault(account.getId(), List.of());
        Map<Long, LocalDateTime> firstBuyExecutedAtByStockId = account == null
                ? Map.of()
                : context.firstBuyExecutedAtByAccountId().getOrDefault(account.getId(), Map.of());

        List<TopHolding> representativeStocks = portfolios.stream()
                .sorted(Comparator.comparing(Portfolio::getTotalPurchaseAmount, Comparator.reverseOrder())
                        .thenComparing(portfolio -> firstBuyExecutedAtByStockId.get(portfolio.getStock().getId()),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(portfolio -> portfolio.getStock().getId()))
                .limit(REPRESENTATIVE_STOCK_LIMIT)
                .map(portfolio -> toTopHolding(portfolio, context.currentPrices().get(portfolio.getStock().getStockCode())))
                .toList();

        return new HomeMockInvestmentRankingsResponse.RankingUser(
                rank == null ? 0 : rank,
                user.getId(),
                ranking.getMaster() == null ? null : ranking.getMaster().getId(),
                nickname,
                ranking.getTotalAsset(),
                ranking.getTotalProfitRate(),
                representativeStocks
        );
    }

    private Map<String, StockCurrentPriceSnapshot> getCurrentPricesWithFallback(List<Portfolio> portfolios) {
        Map<String, StockCurrentPriceSnapshot> currentPrices = new LinkedHashMap<>(
                stockCurrentPriceService.getCurrentPrices(
                        portfolios.stream()
                                .map(portfolio -> portfolio.getStock().getStockCode())
                                .distinct()
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

    private Optional<StockCurrentPriceSnapshot> resolveFallbackCurrentPrice(Portfolio portfolio) {
        return stockCandleRepository.findFirstByStockIdAndIntervalTypeOrderByCandleTimeDesc(
                        portfolio.getStock().getId(),
                        StockCandleIntervalType.DAY
                )
                .map(StockCandle::getClosePrice)
                .map(closePrice -> new StockCurrentPriceSnapshot(closePrice, null));
    }

    private TopHolding toTopHolding(Portfolio portfolio, StockCurrentPriceSnapshot currentPriceSnapshot) {
        BigDecimal currentPrice = currentPriceSnapshot == null ? null : currentPriceSnapshot.currentPrice();
        BigDecimal profitAmount = null;
        BigDecimal profitRate = null;
        if (currentPrice != null) {
            BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
            profitAmount = evaluationAmount.subtract(portfolio.getTotalPurchaseAmount());
            profitRate = calculateProfitRate(profitAmount, portfolio.getTotalPurchaseAmount());
        }

        return new TopHolding(
                portfolio.getStock().getId(),
                portfolio.getStock().getStockCode(),
                portfolio.getStock().getName(),
                portfolio.getTotalPurchaseAmount(),
                profitAmount,
                profitRate
        );
    }

    private BigDecimal calculateProfitRate(BigDecimal profitAmount, BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return profitAmount.divide(baseAmount, PROFIT_RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(PROFIT_RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    private record RankingContext(
            Map<Long, Account> accountsByUserId,
            Map<Long, List<Portfolio>> portfoliosByAccountId,
            Map<Long, Map<Long, LocalDateTime>> firstBuyExecutedAtByAccountId,
            Map<String, StockCurrentPriceSnapshot> currentPrices
    ) {
    }
}
