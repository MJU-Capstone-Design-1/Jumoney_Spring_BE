package com.mju.Jumoney.domain.verifiedoperation.service;

import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyRecommendationRequest;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyRecommendationResponse;
import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.hojumoney.enums.SurveyQuestionType;
import com.mju.Jumoney.domain.hojumoney.repository.SurveyOptionRepository;
import com.mju.Jumoney.domain.hojumoney.service.HojumoneyService;
import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.domain.MasterOption;
import com.mju.Jumoney.domain.master.repository.MasterOptionRepository;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceRequest;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceResponse;
import com.mju.Jumoney.domain.masterchoice.service.MasterChoiceService;
import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderRequest;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderResponse;
import com.mju.Jumoney.domain.mockinvestment.repository.AccountRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.OrderRepository;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentCommandService;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.enums.AuthProvider;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.domain.verifiedoperation.domain.VerifiedOperationTradeLot;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountConfig;
import com.mju.Jumoney.domain.verifiedoperation.enums.VerifiedOperationAccountType;
import com.mju.Jumoney.domain.verifiedoperation.repository.VerifiedOperationTradeLotRepository;
import com.mju.Jumoney.global.batch.MarketCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifiedOperationTradingService {

    private static final int TRADE_QUANTITY = 1;

    private final VerifiedOperationAccountConfigService configService;
    private final MarketCalendarService marketCalendarService;
    private final HojumoneyService hojumoneyService;
    private final MasterChoiceService masterChoiceService;
    private final MockInvestmentCommandService mockInvestmentCommandService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;
    private final VerifiedOperationTradeLotRepository tradeLotRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${verified-operation.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${verified-operation.scheduler.zone-id:Asia/Seoul}")
    private String zoneIdProperty;

    @Scheduled(
            cron = "${verified-operation.scheduler.cron:0 15 9 * * MON-FRI}",
            zone = "${verified-operation.scheduler.zone-id:Asia/Seoul}"
    )
    public void runScheduledTrading() {
        ZoneId zoneId = ZoneId.of(zoneIdProperty);
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime now = LocalDateTime.now(zoneId);
        execute(today, now, zoneId);
    }

    public void execute(LocalDate tradingDate, LocalDateTime now, ZoneId zoneId) {
        if (!schedulerEnabled) {
            log.info("[VerifiedOperationTrading] 스케줄 비활성화로 스킵");
            return;
        }
        if (!marketCalendarService.isOpenDay(tradingDate, zoneId)) {
            log.info("[VerifiedOperationTrading] 휴장일 모의 운용 스킵: date={}", tradingDate);
            return;
        }

        for (VerifiedOperationAccountConfig config : configService.getAccounts()) {
            try {
                executeAccount(config, now);
            } catch (Exception e) {
                log.warn("[VerifiedOperationTrading] 계정 운용 스킵: accountCode={}, reason={}",
                        config.accountCode(), e.getMessage(), e);
            }
        }
    }

    private void executeAccount(VerifiedOperationAccountConfig config, LocalDateTime now) {
        Long userId = findUserId(config);
        if (config.type() == VerifiedOperationAccountType.HOJUMONEY) {
            sellDueLots(config, userId, now);
        }
        buyRecommendedTopOne(config, userId, now);
    }

    private void sellDueLots(VerifiedOperationAccountConfig config, Long userId, LocalDateTime now) {
        LocalDateTime dueDateExclusiveEnd = now.toLocalDate().plusDays(1).atStartOfDay();
        List<VerifiedOperationTradeLot> dueLots = tradeLotRepository
                .findByAccountCodeAndRemainingQuantityGreaterThanAndSellDueAtBeforeOrderByBoughtAtAsc(
                        config.accountCode(),
                        0,
                        dueDateExclusiveEnd
                );
        for (VerifiedOperationTradeLot lot : dueLots) {
            try {
                MockInvestmentOrderResponse response = mockInvestmentCommandService.sell(
                        userId,
                        new MockInvestmentOrderRequest(lot.getStock().getStockCode(), lot.getRemainingQuantity())
                );
                closeLot(lot.getId(), response.orderId(), now);
                log.info("[VerifiedOperationTrading] 만기 lot 매도 완료: accountCode={}, stockCode={}, quantity={}",
                        config.accountCode(), response.stockCode(), response.quantity());
            } catch (Exception e) {
                log.info("[VerifiedOperationTrading] 만기 lot 매도 스킵: accountCode={}, lotId={}, reason={}",
                        config.accountCode(), lot.getId(), e.getMessage());
            }
        }
    }

    private void buyRecommendedTopOne(VerifiedOperationAccountConfig config, Long userId, LocalDateTime now) {
        Optional<RecommendedStock> recommendation = recommendTopOne(config);
        if (recommendation.isEmpty()) {
            log.info("[VerifiedOperationTrading] 추천 없음으로 매수 스킵: accountCode={}", config.accountCode());
            return;
        }

        RecommendedStock stock = recommendation.get();
        if (stock.currentPrice() == null) {
            log.info("[VerifiedOperationTrading] 현재가 없음으로 매수 스킵: accountCode={}, stockCode={}",
                    config.accountCode(), stock.stockCode());
            return;
        }

        Account account = findAccount(config);
        if (account.getCashBalance().compareTo(stock.currentPrice().multiply(BigDecimal.valueOf(TRADE_QUANTITY))) < 0) {
            log.info("[VerifiedOperationTrading] 현금 부족으로 매수 스킵: accountCode={}, stockCode={}, cash={}, price={}",
                    config.accountCode(), stock.stockCode(), account.getCashBalance(), stock.currentPrice());
            return;
        }

        MockInvestmentOrderResponse response = mockInvestmentCommandService.buy(
                userId,
                new MockInvestmentOrderRequest(stock.stockCode(), TRADE_QUANTITY)
        );
        saveBuyLot(config, response, now);
        log.info("[VerifiedOperationTrading] 추천 Top1 매수 완료: accountCode={}, stockCode={}, quantity={}",
                config.accountCode(), response.stockCode(), response.quantity());
    }

    private Optional<RecommendedStock> recommendTopOne(VerifiedOperationAccountConfig config) {
        if (config.type() == VerifiedOperationAccountType.HOJUMONEY) {
            HojumoneyRecommendationResponse response = hojumoneyService.recommend(new HojumoneyRecommendationRequest(
                    config.hojumoneyConditions().stream()
                            .map(logicCode -> surveyOptionRepository.findByLogicCode(logicCode)
                                    .orElseThrow(() -> new IllegalStateException("설문 선택지가 없습니다: " + logicCode))
                                    .getId())
                            .toList()
            ));
            return response.recommendations().stream()
                    .findFirst()
                    .map(item -> new RecommendedStock(item.stockCode(), item.currentPrice()));
        }

        Master master = masterRepository.findByMasterCode(config.masterCode())
                .orElseThrow(() -> new IllegalStateException("거장 데이터가 없습니다: " + config.masterCode()));
        List<Long> optionIds = config.masterConditions().stream()
                .map(logicCode -> masterOptionRepository.findByLogicCode(logicCode)
                        .orElseThrow(() -> new IllegalStateException("거장 조건이 없습니다: " + logicCode)))
                .map(MasterOption::getId)
                .toList();
        MasterChoiceResponse response = masterChoiceService.recommend(
                master.getId(),
                new MasterChoiceRequest(optionIds, null)
        );
        return response.recommendations().stream()
                .findFirst()
                .map(item -> new RecommendedStock(item.stockCode(), item.currentPrice()));
    }

    private Long findUserId(VerifiedOperationAccountConfig config) {
        return userRepository.findByProviderAndProviderIdIncludeDeleted(AuthProvider.LOCAL.name(), config.providerId())
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("모의 운용 사용자 계정이 없습니다: " + config.accountCode()));
    }

    private Account findAccount(VerifiedOperationAccountConfig config) {
        Long userId = findUserId(config);
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("모의 운용 투자 계좌가 없습니다: " + config.accountCode()));
    }

    private void saveBuyLot(
            VerifiedOperationAccountConfig config,
            MockInvestmentOrderResponse response,
            LocalDateTime boughtAt
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            Account account = findAccount(config);
            Stock stock = stockRepository.findByStockCode(response.stockCode())
                    .orElseThrow(() -> new IllegalStateException("종목이 없습니다: " + response.stockCode()));
            Order buyOrder = orderRepository.findById(response.orderId())
                    .orElseThrow(() -> new IllegalStateException("매수 주문이 없습니다: " + response.orderId()));
            tradeLotRepository.save(VerifiedOperationTradeLot.create(
                    config.accountCode(),
                    account,
                    stock,
                    buyOrder,
                    response.quantity(),
                    boughtAt,
                    resolveSellDueAt(config, boughtAt)
            ));
        });
    }

    private void closeLot(Long lotId, Long sellOrderId, LocalDateTime closedAt) {
        transactionTemplate.executeWithoutResult(status -> {
            VerifiedOperationTradeLot lot = tradeLotRepository.findById(lotId)
                    .orElseThrow(() -> new IllegalStateException("lot을 찾을 수 없습니다: " + lotId));
            Order sellOrder = orderRepository.findById(sellOrderId)
                    .orElseThrow(() -> new IllegalStateException("매도 주문이 없습니다: " + sellOrderId));
            lot.close(sellOrder, closedAt);
        });
    }

    private LocalDateTime resolveSellDueAt(VerifiedOperationAccountConfig config, LocalDateTime boughtAt) {
        if (config.type() != VerifiedOperationAccountType.HOJUMONEY) {
            return null;
        }
        SurveyLogicCode horizon = config.hojumoneyConditions().stream()
                .filter(logicCode -> logicCode.getQuestionType() == SurveyQuestionType.INVESTMENT_HORIZON)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("투자 기간 조건이 없습니다: " + config.accountCode()));
        return switch (horizon) {
            case ULTRA_SHORT -> boughtAt.toLocalDate().plusDays(1).atStartOfDay();
            case SHORT -> boughtAt.toLocalDate().plusDays(7).atStartOfDay();
            case MID -> boughtAt.toLocalDate().plusMonths(3).atStartOfDay();
            case LONG -> boughtAt.toLocalDate().plusYears(1).atStartOfDay();
            default -> throw new IllegalStateException("투자 기간 조건이 아닙니다: " + horizon);
        };
    }

    private record RecommendedStock(
            String stockCode,
            BigDecimal currentPrice
    ) {
    }
}
