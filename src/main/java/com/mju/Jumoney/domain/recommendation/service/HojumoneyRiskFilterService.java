package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRiskCandidate;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import com.mju.Jumoney.domain.stock.repository.HtsStockRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HojumoneyRiskFilterService {

    private final HtsStockRepository htsStockRepository;

    public List<HojumoneyRiskCandidate> findCandidates(SurveyLogicCode riskProfile) {
        HtsSearchType searchType = toHtsSearchType(riskProfile);
        LocalDate baseDate = htsStockRepository.findLatestBaseDate()
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.HTS_STOCK_BASE_DATE_NOT_FOUND));

        return htsStockRepository.findBySearchTypeAndBaseDateWithStock(searchType, baseDate)
                .stream()
                .map(HojumoneyRiskCandidate::from)
                .toList();
    }

    private HtsSearchType toHtsSearchType(SurveyLogicCode riskProfile) {
        return switch (riskProfile) {
            case STABILITY -> HtsSearchType.STABILITY;
            case SAFE_PURSUIT -> HtsSearchType.SAFE_PURSUIT;
            case PROFIT_PURSUIT -> HtsSearchType.PROFIT_PURSUIT;
            case AGGRESSIVE -> HtsSearchType.AGGRESSIVE;
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
        };
    }
}
