package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.recommendation.dto.HojumoneyIndicatorCandidate;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HojumoneyIndicatorFilterService {

    private static final int CAPITAL_PROTECTION_MARKET_CAP_LIMIT = 50;
    private static final BigDecimal CAPITAL_PROTECTION_MAX_DEBT_RATIO = BigDecimal.valueOf(100);
    private static final BigDecimal DIVIDEND_MIN_YIELD = BigDecimal.valueOf(4);
    private static final BigDecimal DIVIDEND_MAX_YIELD = BigDecimal.valueOf(8);
    private static final BigDecimal DIVIDEND_MIN_PAYOUT_RATIO = BigDecimal.valueOf(25);
    private static final BigDecimal DIVIDEND_MAX_PAYOUT_RATIO = BigDecimal.valueOf(80);
    private static final BigDecimal STEADY_GROWTH_MIN_ROE = BigDecimal.valueOf(8);
    private static final BigDecimal STEADY_GROWTH_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(5);
    private static final BigDecimal CAPITAL_GAIN_MAX_PER = BigDecimal.valueOf(10);
    private static final BigDecimal CAPITAL_GAIN_MAX_PBR = BigDecimal.ONE;

    private final StockIndicatorRepository stockIndicatorRepository;

    public List<HojumoneyIndicatorCandidate> findCandidates(SurveyLogicCode investmentPurpose) {
        String baseTime = stockIndicatorRepository.findLatestBaseTime()
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.STOCK_INDICATOR_BASE_TIME_NOT_FOUND));

        List<StockIndicator> indicators = switch (investmentPurpose) {
            case CAPITAL_PROTECTION -> findCapitalProtectionCandidates(baseTime);
            case DIVIDEND_INCOME -> stockIndicatorRepository.findDividendIncomeCandidates(
                    baseTime,
                    DIVIDEND_MIN_YIELD,
                    DIVIDEND_MAX_YIELD,
                    DIVIDEND_MIN_PAYOUT_RATIO,
                    DIVIDEND_MAX_PAYOUT_RATIO
            );
            case STEADY_GROWTH -> stockIndicatorRepository.findSteadyGrowthCandidates(
                    baseTime,
                    STEADY_GROWTH_MIN_ROE,
                    STEADY_GROWTH_MIN_EPS_GROWTH_RATE
            );
            case CAPITAL_GAIN -> stockIndicatorRepository.findCapitalGainCandidates(
                    baseTime,
                    CAPITAL_GAIN_MAX_PER,
                    CAPITAL_GAIN_MAX_PBR
            );
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
        };

        return indicators.stream()
                .map(HojumoneyIndicatorCandidate::from)
                .toList();
    }

    private List<StockIndicator> findCapitalProtectionCandidates(String baseTime) {
        List<Long> topMarketCapStockIds = stockIndicatorRepository.findTopMarketCapStockIds(
                baseTime,
                PageRequest.of(0, CAPITAL_PROTECTION_MARKET_CAP_LIMIT)
        );
        if (topMarketCapStockIds.isEmpty()) {
            return List.of();
        }

        return stockIndicatorRepository.findCapitalProtectionCandidatesInTopMarketCapStocks(
                baseTime,
                topMarketCapStockIds,
                CAPITAL_PROTECTION_MAX_DEBT_RATIO
        );
    }
}
