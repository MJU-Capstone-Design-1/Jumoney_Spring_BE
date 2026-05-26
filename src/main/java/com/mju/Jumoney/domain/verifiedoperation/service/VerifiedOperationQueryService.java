package com.mju.Jumoney.domain.verifiedoperation.service;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderHistoryItemResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentPortfolioItemResponse;
import com.mju.Jumoney.domain.mockinvestment.enums.OrderType;
import com.mju.Jumoney.domain.mockinvestment.repository.AccountRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.OrderRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.enums.AuthProvider;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountConfig;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountDetailResponse;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountSummaryResponse;
import com.mju.Jumoney.domain.verifiedoperation.enums.VerifiedOperationAccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerifiedOperationQueryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String OPERATION_DESCRIPTION =
            "2026년 5월 26일부터 매일 추천 종목 1종목을 1주 매수하는 모의 운용 계정이에요. 추천 로직의 신뢰성을 확인해볼 수 있어요.";
    private static final int PROFIT_RATE_DIVIDE_SCALE = 6;
    private static final int PROFIT_RATE_DISPLAY_SCALE = 4;
    private static final int RECENT_ORDER_LIMIT = 20;

    private final VerifiedOperationAccountConfigService configService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PortfolioRepository portfolioRepository;
    private final OrderRepository orderRepository;
    private final StockCurrentPriceService stockCurrentPriceService;

    public VerifiedOperationAccountSummaryResponse getHojumoneyAccounts() {
        List<VerifiedOperationAccountSummaryResponse.AccountSummary> accounts = configService.getAccounts().stream()
                .filter(config -> config.type() == VerifiedOperationAccountType.HOJUMONEY)
                .map(this::toSummary)
                .toList();
        return new VerifiedOperationAccountSummaryResponse(OPERATION_DESCRIPTION, accounts);
    }

    public VerifiedOperationAccountDetailResponse getMasterChoiceAccount(MasterCode masterCode) {
        VerifiedOperationAccountConfig config = configService.getAccounts().stream()
                .filter(account -> account.type() == VerifiedOperationAccountType.MASTER_CHOICE)
                .filter(account -> account.masterCode() == masterCode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown master operation account: " + masterCode));
        return toDetail(config);
    }

    private VerifiedOperationAccountDetailResponse toDetail(VerifiedOperationAccountConfig config) {
        Account account = findAccount(config);
        List<Portfolio> portfolios = portfolioRepository.findByAccountIdOrderByUpdatedAtDesc(account.getId());
        Map<String, StockCurrentPriceSnapshot> currentPrices = getCurrentPrices(portfolios);
        AccountEvaluation evaluation = evaluate(account, portfolios, currentPrices);
        List<MockInvestmentPortfolioItemResponse> holdings = portfolios.stream()
                .map(portfolio -> toPortfolioItem(portfolio, currentPrices.get(portfolio.getStock().getStockCode())))
                .toList();
        List<MockInvestmentOrderHistoryItemResponse> recentOrders = orderRepository.findByAccountIdOrderByExecutedAtDesc(
                        account.getId(),
                        PageRequest.of(0, RECENT_ORDER_LIMIT)
                )
                .stream()
                .map(this::toOrderHistoryItem)
                .toList();

        return new VerifiedOperationAccountDetailResponse(
                OPERATION_DESCRIPTION,
                config.accountCode(),
                config.accountName(),
                config.type(),
                conditions(config),
                account.getTotalPurchaseAmount(),
                evaluation.totalEvaluationAmount(),
                evaluation.totalProfitAmount(),
                evaluation.totalProfitRate(),
                portfolios.size(),
                lastTradedAt(account),
                holdings,
                recentOrders
        );
    }

    private VerifiedOperationAccountSummaryResponse.AccountSummary toSummary(VerifiedOperationAccountConfig config) {
        Account account = findAccount(config);
        List<Portfolio> portfolios = portfolioRepository.findByAccountId(account.getId());
        AccountEvaluation evaluation = evaluate(account, portfolios, getCurrentPrices(portfolios));
        return new VerifiedOperationAccountSummaryResponse.AccountSummary(
                config.accountCode(),
                config.accountName(),
                config.type(),
                conditions(config),
                account.getTotalPurchaseAmount(),
                evaluation.totalEvaluationAmount(),
                evaluation.totalProfitAmount(),
                evaluation.totalProfitRate(),
                portfolios.size(),
                lastTradedAt(account)
        );
    }

    private Account findAccount(VerifiedOperationAccountConfig config) {
        User user = userRepository.findByProviderAndProviderIdIncludeDeleted(AuthProvider.LOCAL.name(), config.providerId())
                .orElseThrow(() -> new IllegalStateException("모의 운용 사용자 계정이 없습니다: " + config.accountCode()));
        return accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("모의 운용 투자 계좌가 없습니다: " + config.accountCode()));
    }

    private List<VerifiedOperationAccountSummaryResponse.ConditionResponse> conditions(VerifiedOperationAccountConfig config) {
        if (config.type() == VerifiedOperationAccountType.HOJUMONEY) {
            return config.hojumoneyConditions().stream()
                    .map(this::toCondition)
                    .toList();
        }
        return config.masterConditions().stream()
                .map(this::toCondition)
                .toList();
    }

    private VerifiedOperationAccountSummaryResponse.ConditionResponse toCondition(SurveyLogicCode logicCode) {
        return new VerifiedOperationAccountSummaryResponse.ConditionResponse(logicCode.name(), logicCode.getLabel());
    }

    private VerifiedOperationAccountSummaryResponse.ConditionResponse toCondition(MasterOptionLogicCode logicCode) {
        return new VerifiedOperationAccountSummaryResponse.ConditionResponse(logicCode.name(), logicCode.getLabel());
    }

    private Map<String, StockCurrentPriceSnapshot> getCurrentPrices(List<Portfolio> portfolios) {
        List<String> stockCodes = portfolios.stream()
                .map(portfolio -> portfolio.getStock().getStockCode())
                .toList();
        if (stockCodes.isEmpty()) {
            return Map.of();
        }
        return stockCurrentPriceService.getCurrentPrices(stockCodes);
    }

    private AccountEvaluation evaluate(
            Account account,
            List<Portfolio> portfolios,
            Map<String, StockCurrentPriceSnapshot> currentPrices
    ) {
        BigDecimal totalEvaluationAmount = portfolios.stream()
                .map(portfolio -> evaluationAmount(portfolio, currentPrices.get(portfolio.getStock().getStockCode())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfitAmount = totalEvaluationAmount.subtract(account.getTotalPurchaseAmount());
        BigDecimal totalProfitRate = calculateProfitRate(totalProfitAmount, account.getTotalPurchaseAmount());
        return new AccountEvaluation(totalEvaluationAmount, totalProfitAmount, totalProfitRate);
    }

    private BigDecimal evaluationAmount(Portfolio portfolio, StockCurrentPriceSnapshot currentPriceSnapshot) {
        BigDecimal currentPrice = currentPriceSnapshot == null ? null : currentPriceSnapshot.currentPrice();
        return currentPrice == null
                ? BigDecimal.ZERO
                : currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
    }

    private MockInvestmentPortfolioItemResponse toPortfolioItem(
            Portfolio portfolio,
            StockCurrentPriceSnapshot currentPriceSnapshot
    ) {
        BigDecimal currentPrice = currentPriceSnapshot == null ? null : currentPriceSnapshot.currentPrice();
        BigDecimal changeRate = currentPriceSnapshot == null ? null : currentPriceSnapshot.changeRate();
        BigDecimal evaluationAmount = currentPrice == null
                ? BigDecimal.ZERO
                : currentPrice.multiply(BigDecimal.valueOf(portfolio.getQuantity()));
        BigDecimal profitAmount = currentPrice == null
                ? BigDecimal.ZERO
                : evaluationAmount.subtract(portfolio.getTotalPurchaseAmount());
        BigDecimal profitRate = currentPrice == null
                ? BigDecimal.ZERO
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

    private MockInvestmentOrderHistoryItemResponse toOrderHistoryItem(Order order) {
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

    private java.time.LocalDateTime lastTradedAt(Account account) {
        return orderRepository.findFirstByAccountIdAndOrderTypeInOrderByExecutedAtDesc(
                        account.getId(),
                        List.of(OrderType.BUY, OrderType.SELL)
                )
                .map(Order::getExecutedAt)
                .orElse(null);
    }

    private BigDecimal calculateProfitRate(BigDecimal profitAmount, BigDecimal baseAmount) {
        if (baseAmount == null || baseAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return profitAmount.divide(baseAmount, PROFIT_RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(PROFIT_RATE_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    private record AccountEvaluation(
            BigDecimal totalEvaluationAmount,
            BigDecimal totalProfitAmount,
            BigDecimal totalProfitRate
    ) {
    }
}
