package com.mju.Jumoney.domain.ranking.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import com.mju.Jumoney.domain.mockinvestment.repository.AccountRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.ranking.domain.UserRanking;
import com.mju.Jumoney.domain.ranking.repository.UserRankingRepository;
import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import com.mju.Jumoney.domain.stock.repository.StockCandleRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockInvestmentRankingUpdateService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PROFIT_RATE_DIVIDE_SCALE = 6;
    private static final int PROFIT_RATE_DISPLAY_SCALE = 4;

    private final AccountRepository accountRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserRankingRepository userRankingRepository;
    private final StockCurrentPriceService stockCurrentPriceService;
    private final StockCandleRepository stockCandleRepository;

    @Value("${mock-investment.ranking.scheduler.enabled:false}")
    private boolean rankingSchedulerEnabled;

    @Value("${mock-investment.ranking.scheduler.zone-id:Asia/Seoul}")
    private String rankingSchedulerZoneId;

    @Scheduled(
            cron = "${mock-investment.ranking.scheduler.cron:0 0 * * * *}",
            zone = "${mock-investment.ranking.scheduler.zone-id:Asia/Seoul}"
    )
    @Transactional
    public void refreshScheduledRankings() {
        if (!rankingSchedulerEnabled) {
            return;
        }
        refreshRankings();
    }

    @Transactional
    public void refreshRankings() {
        List<Account> accounts = accountRepository.findAllWithActiveUsers();
        if (accounts.isEmpty()) {
            userRankingRepository.deleteAllInBatch();
            return;
        }

        Map<Long, List<Portfolio>> portfoliosByAccountId = portfolioRepository.findByAccountIdIn(
                        accounts.stream()
                                .map(Account::getId)
                                .toList()
                ).stream()
                .collect(Collectors.groupingBy(portfolio -> portfolio.getAccount().getId()));

        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPricesWithFallback(
                portfoliosByAccountId.values().stream()
                        .flatMap(List::stream)
                        .toList()
        );

        List<RankingCandidate> candidates = accounts.stream()
                .map(account -> toRankingCandidate(account, portfoliosByAccountId.getOrDefault(account.getId(), List.of()), currentPrices))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RankingCandidate::totalAsset, Comparator.reverseOrder())
                        .thenComparing(RankingCandidate::totalProfitRate, Comparator.reverseOrder())
                        .thenComparing(candidate -> candidate.user().getId()))
                .toList();

        LocalDate rankingDate = LocalDate.now(ZoneId.of(rankingSchedulerZoneId));
        List<UserRanking> rankings = buildRankings(candidates, rankingDate);

        userRankingRepository.deleteAllInBatch();
        userRankingRepository.saveAll(rankings);

        log.info("[MockInvestmentRankingUpdateService] 모의투자 랭킹 스냅샷 갱신 완료 - accounts={}, rankings={}",
                accounts.size(), rankings.size());
    }

    private List<UserRanking> buildRankings(List<RankingCandidate> candidates, LocalDate rankingDate) {
        Map<Long, Integer> nextMasterRanks = new HashMap<>();
        List<UserRanking> rankings = new ArrayList<>();

        for (int index = 0; index < candidates.size(); index++) {
            RankingCandidate candidate = candidates.get(index);
            Master master = candidate.master();
            Integer masterRank = null;
            if (master != null) {
                masterRank = nextMasterRanks.getOrDefault(master.getId(), 0) + 1;
                nextMasterRanks.put(master.getId(), masterRank);
            }

            rankings.add(UserRanking.create(
                    candidate.user(),
                    master,
                    candidate.totalAsset(),
                    candidate.totalProfitRate(),
                    index + 1,
                    masterRank,
                    rankingDate
            ));
        }

        return rankings;
    }

    private RankingCandidate toRankingCandidate(Account account,
                                                List<Portfolio> portfolios,
                                                Map<String, StockCurrentPriceSnapshot> currentPrices) {
        BigDecimal totalEvaluationAmount = BigDecimal.ZERO;
        for (Portfolio portfolio : portfolios) {
            BigDecimal evaluationAmount = calculateEvaluationAmount(portfolio, currentPrices);
            if (evaluationAmount == null) {
                return null;
            }
            totalEvaluationAmount = totalEvaluationAmount.add(evaluationAmount);
        }

        BigDecimal totalAsset = account.getCashBalance().add(totalEvaluationAmount);
        BigDecimal totalProfitAmount = totalAsset.subtract(account.getSeedMoney());
        BigDecimal totalProfitRate = calculateProfitRate(totalProfitAmount, account.getSeedMoney());
        return new RankingCandidate(account.getUser(), account.getUser().getSelectedMaster(), totalAsset, totalProfitRate);
    }

    private BigDecimal calculateEvaluationAmount(Portfolio portfolio, Map<String, StockCurrentPriceSnapshot> currentPrices) {
        StockCurrentPriceSnapshot snapshot = currentPrices.get(portfolio.getStock().getStockCode());
        BigDecimal currentPrice = snapshot == null ? null : snapshot.currentPrice();
        if (currentPrice == null) {
            return null;
        }
        return currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
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

    private BigDecimal calculateProfitRate(BigDecimal profitAmount, BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return profitAmount.divide(baseAmount, PROFIT_RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(PROFIT_RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    private record RankingCandidate(
            User user,
            Master master,
            BigDecimal totalAsset,
            BigDecimal totalProfitRate
    ) {
    }
}
