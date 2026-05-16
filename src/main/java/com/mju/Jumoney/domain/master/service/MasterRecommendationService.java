package com.mju.Jumoney.domain.master.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.domain.MasterOption;
import com.mju.Jumoney.domain.master.dto.MasterRecommendationCandidate;
import com.mju.Jumoney.domain.master.dto.MasterRecommendationRequest;
import com.mju.Jumoney.domain.master.dto.MasterRecommendationResponse;
import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.master.repository.MasterOptionRepository;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.sector.service.GoodSectorService;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterRecommendationService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;

    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;
    private final MasterRecommendationFilterService filterService;
    private final StockCurrentPriceService stockCurrentPriceService;
    private final GoodSectorService goodSectorService;

    public MasterRecommendationResponse recommend(Long masterId, MasterRecommendationRequest request) {
        Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.MASTER_NOT_FOUND));
        List<MasterOption> selectedOptions = resolveSelectedOptions(master, request.selectedOptionIds());
        validateSectorSelection(selectedOptions, request.sectorTypes());

        List<MasterOptionLogicCode> logicCodes = selectedOptions.stream()
                .map(MasterOption::getLogicCode)
                .toList();
        List<Long> selectedOptionIds = selectedOptions.stream()
                .map(MasterOption::getId)
                .toList();

        Set<String> goodSectorNames = goodSectorService.getTodayGoodSectorNames();
        List<MasterRecommendationCandidate> eligibleCandidates = filterService.findCandidates(
                        master.getMasterCode(),
                        logicCodes,
                        request.sectorTypes()
                )
                .stream()
                .filter(candidate -> candidate.getSortMetricValue() != null)
                .sorted(candidateComparator(master.getMasterCode(), goodSectorNames))
                .toList();

        List<MasterRecommendationCandidate> topCandidates = eligibleCandidates.stream()
                .limit(DEFAULT_RECOMMENDATION_LIMIT)
                .toList();
        Map<String, StockCurrentPriceSnapshot> currentPrices = stockCurrentPriceService.getCurrentPrices(
                topCandidates.stream()
                        .map(candidate -> candidate.getStock().getStockCode())
                        .toList()
        );

        List<MasterRecommendationResponse.RecommendedStockResponse> recommendations = topCandidates.stream()
                .map(candidate -> toRecommendedStockResponse(
                        candidate,
                        sortMetricKey(master.getMasterCode()),
                        currentPrices.get(candidate.getStock().getStockCode()),
                        goodSectorNames
                ))
                .toList();

        return new MasterRecommendationResponse(
                master.getId(),
                master.getMasterCode(),
                master.getMasterName(),
                selectedOptionIds,
                recommendations.size(),
                rank(recommendations)
        );
    }

    private List<MasterOption> resolveSelectedOptions(Master master, List<Long> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return masterOptionRepository.findByMasterIdOrderByDisplayOrderAsc(master.getId());
        }

        List<Long> distinctIds = selectedOptionIds.stream()
                .distinct()
                .toList();
        List<MasterOption> options = masterOptionRepository.findByIdIn(distinctIds);
        if (options.size() != distinctIds.size()) {
            throw new CustomException(RecommendationErrorCode.INVALID_MASTER_OPTION_SELECTION);
        }

        boolean hasInvalidOption = options.stream()
                .anyMatch(option -> !option.getMaster().getId().equals(master.getId()));
        if (hasInvalidOption) {
            throw new CustomException(RecommendationErrorCode.INVALID_MASTER_OPTION_SELECTION);
        }

        Map<Long, MasterOption> optionById = options.stream()
                .collect(Collectors.toMap(MasterOption::getId, option -> option));
        return distinctIds.stream()
                .map(optionById::get)
                .toList();
    }

    private void validateSectorSelection(List<MasterOption> selectedOptions, List<?> sectorTypes) {
        boolean needsSectorSelection = selectedOptions.stream()
                .map(MasterOption::getLogicCode)
                .anyMatch(this::requiresSectorSelection);
        if (needsSectorSelection && (sectorTypes == null || sectorTypes.isEmpty())) {
            throw new CustomException(RecommendationErrorCode.MISSING_MASTER_SECTOR_SELECTION);
        }
        if (!needsSectorSelection && sectorTypes != null && !sectorTypes.isEmpty()) {
            throw new CustomException(RecommendationErrorCode.UNSUPPORTED_MASTER_SECTOR_SELECTION);
        }
    }

    private boolean requiresSectorSelection(MasterOptionLogicCode logicCode) {
        return logicCode == MasterOptionLogicCode.LYNCH_SECTOR
                || logicCode == MasterOptionLogicCode.DALIO_ALL_WEATHER;
    }

    private Comparator<MasterRecommendationCandidate> candidateComparator(
            MasterCode masterCode,
            Set<String> goodSectorNames
    ) {
        Comparator<MasterRecommendationCandidate> comparator = Comparator
                .comparing((MasterRecommendationCandidate candidate) -> goodSectorService.hasGoodSectorMatch(candidate.getStock(), goodSectorNames), Comparator.reverseOrder());

        Comparator<MasterRecommendationCandidate> sortMetricComparator = Comparator.comparing(
                MasterRecommendationCandidate::getSortMetricValue,
                Comparator.nullsLast(BigDecimal::compareTo)
        );

        if (masterCode == MasterCode.PETER_LYNCH) {
            return comparator
                    .thenComparing(sortMetricComparator)
                    .thenComparing(candidate -> candidate.getStock().getStockCode());
        }

        return comparator
                .thenComparing(sortMetricComparator.reversed())
                .thenComparing(candidate -> candidate.getStock().getStockCode());
    }

    private String sortMetricKey(MasterCode masterCode) {
        return switch (masterCode) {
            case WARREN_BUFFETT -> "ROE";
            case PETER_LYNCH -> "PEG";
            case RAY_DALIO -> "MARKET_CAP";
            case WILLIAM_ONEIL -> "HIGH_52_WEEK_RATE";
        };
    }

    private MasterRecommendationResponse.RecommendedStockResponse toRecommendedStockResponse(
            MasterRecommendationCandidate candidate,
            String sortMetricKey,
            StockCurrentPriceSnapshot currentPrice,
            Set<String> goodSectorNames
    ) {
        Stock stock = candidate.getStock();
        return new MasterRecommendationResponse.RecommendedStockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                0,
                List.copyOf(candidate.getMatchedOptions()),
                goodSectorService.goodSectorTags(stock, goodSectorNames),
                candidate.matchedConditionCount(),
                sortMetricKey,
                candidate.getSortMetricValue(),
                currentPrice == null ? null : currentPrice.currentPrice(),
                currentPrice == null ? null : currentPrice.changeRate()
        );
    }

    private List<MasterRecommendationResponse.RecommendedStockResponse> rank(
            List<MasterRecommendationResponse.RecommendedStockResponse> recommendations
    ) {
        List<MasterRecommendationResponse.RecommendedStockResponse> ranked = new ArrayList<>();
        for (int i = 0; i < recommendations.size(); i++) {
            MasterRecommendationResponse.RecommendedStockResponse item = recommendations.get(i);
            ranked.add(new MasterRecommendationResponse.RecommendedStockResponse(
                    item.stockId(),
                    item.stockCode(),
                    item.stockName(),
                    i + 1,
                    item.tags(),
                    item.goodSectorTags(),
                    item.matchedConditionCount(),
                    item.sortMetricKey(),
                    item.sortMetricValue(),
                    item.currentPrice(),
                    item.changeRate()
            ));
        }
        return ranked;
    }

}
