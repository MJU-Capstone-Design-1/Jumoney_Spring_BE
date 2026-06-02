package com.mju.Jumoney.domain.masterchoice.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.domain.MasterOption;
import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.master.exception.MasterErrorCode;
import com.mju.Jumoney.domain.master.repository.MasterOptionRepository;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceCandidate;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceRequest;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceResponse;
import com.mju.Jumoney.domain.masterchoice.exception.MasterChoiceErrorCode;
import com.mju.Jumoney.domain.sector.service.GoodSectorService;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterChoiceService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;

    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;
    private final MasterChoiceFilterService masterChoiceFilterService;
    private final StockCurrentPriceService stockCurrentPriceService;
    private final GoodSectorService goodSectorService;

    public MasterChoiceResponse recommend(Long masterId, MasterChoiceRequest request) {
        Master master = findMasterById(masterId);
        List<MasterOption> selectedOptions = resolveSelectedOptions(master, request.selectedOptionIds());
        validateSectorSelection(selectedOptions, request.sectorTypes());

        List<MasterOptionLogicCode> logicCodes = selectedOptions.stream()
                .map(MasterOption::getLogicCode)
                .toList();
        List<Long> selectedOptionIds = selectedOptions.stream()
                .map(MasterOption::getId)
                .toList();

        Set<String> goodSectorNames = goodSectorService.getTodayGoodSectorNames();
        List<MasterChoiceCandidate> eligibleCandidates = masterChoiceFilterService.findCandidates(
                        master.getMasterCode(),
                        logicCodes,
                        request.sectorTypes()
                )
                .stream()
                .filter(candidate -> hasSortableMetric(master.getMasterCode(), candidate))
                .sorted(candidateComparator(master.getMasterCode(), goodSectorNames))
                .toList();

        List<MasterChoiceCandidate> topCandidates = eligibleCandidates.stream()
                .limit(DEFAULT_RECOMMENDATION_LIMIT)
                .toList();
        Map<String, StockCurrentPriceSnapshot> currentPrices = stockCurrentPriceService.getCurrentPrices(
                topCandidates.stream()
                        .map(candidate -> candidate.getStock().getStockCode())
                        .toList()
        );

        List<MasterChoiceResponse.RecommendedStockResponse> recommendations = topCandidates.stream()
                .map(candidate -> toRecommendedStockResponse(
                        candidate,
                        sortMetricKey(master.getMasterCode(), candidate),
                        sortMetricValue(master.getMasterCode(), candidate),
                        currentPrices.get(candidate.getStock().getStockCode()),
                        goodSectorNames
                ))
                .toList();

        return new MasterChoiceResponse(
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
            throw new CustomException(MasterChoiceErrorCode.INVALID_MASTER_OPTION_SELECTION);
        }

        boolean hasInvalidOption = options.stream()
                .anyMatch(option -> !option.getMaster().getId().equals(master.getId()));
        if (hasInvalidOption) {
            throw new CustomException(MasterChoiceErrorCode.INVALID_MASTER_OPTION_SELECTION);
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
            throw new CustomException(MasterChoiceErrorCode.MISSING_MASTER_SECTOR_SELECTION);
        }
        if (!needsSectorSelection && sectorTypes != null && !sectorTypes.isEmpty()) {
            throw new CustomException(MasterChoiceErrorCode.UNSUPPORTED_MASTER_SECTOR_SELECTION);
        }
    }

    private boolean requiresSectorSelection(MasterOptionLogicCode logicCode) {
        return logicCode == MasterOptionLogicCode.LYNCH_SECTOR
                || logicCode == MasterOptionLogicCode.DALIO_ALL_WEATHER;
    }

    private Comparator<MasterChoiceCandidate> candidateComparator(
            MasterCode masterCode,
            Set<String> goodSectorNames
    ) {
        Comparator<MasterChoiceCandidate> comparator = Comparator
                .comparing((MasterChoiceCandidate candidate) -> goodSectorService.hasGoodSectorMatch(candidate.getStock(), goodSectorNames), Comparator.reverseOrder());

        if (masterCode == MasterCode.PETER_LYNCH) {
            return comparator
                    .thenComparing(candidate -> candidate.getSortMetricValue() != null, Comparator.reverseOrder())
                    .thenComparing(MasterChoiceCandidate::getSortMetricValue, Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(MasterChoiceCandidate::getFallbackSortMetricValue, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(candidate -> candidate.getStock().getStockCode());
        }

        Comparator<MasterChoiceCandidate> sortMetricComparator = Comparator.comparing(
                MasterChoiceCandidate::getSortMetricValue,
                Comparator.nullsLast(BigDecimal::compareTo)
        );

        return comparator
                .thenComparing(sortMetricComparator.reversed())
                .thenComparing(candidate -> candidate.getStock().getStockCode());
    }

    private boolean hasSortableMetric(MasterCode masterCode, MasterChoiceCandidate candidate) {
        if (masterCode == MasterCode.PETER_LYNCH) {
            return true;
        }
        return candidate.getSortMetricValue() != null;
    }

    private String sortMetricKey(MasterCode masterCode, MasterChoiceCandidate candidate) {
        return switch (masterCode) {
            case WARREN_BUFFETT -> "ROE";
            case PETER_LYNCH -> candidate.getSortMetricValue() != null ? "PEG" : "SALES_GROWTH_RATE";
            case RAY_DALIO -> "MARKET_CAP";
            case WILLIAM_ONEIL -> "HIGH_52_WEEK_RATE";
        };
    }

    private BigDecimal sortMetricValue(MasterCode masterCode, MasterChoiceCandidate candidate) {
        if (masterCode == MasterCode.PETER_LYNCH && candidate.getSortMetricValue() == null) {
            return candidate.getFallbackSortMetricValue();
        }
        return candidate.getSortMetricValue();
    }

    private MasterChoiceResponse.RecommendedStockResponse toRecommendedStockResponse(
            MasterChoiceCandidate candidate,
            String sortMetricKey,
            BigDecimal sortMetricValue,
            StockCurrentPriceSnapshot currentPrice,
            Set<String> goodSectorNames
    ) {
        Stock stock = candidate.getStock();
        return new MasterChoiceResponse.RecommendedStockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                0,
                List.copyOf(candidate.getMatchedOptions()),
                goodSectorService.goodSectorTags(stock, goodSectorNames),
                candidate.matchedConditionCount(),
                sortMetricKey,
                sortMetricValue,
                currentPrice == null ? null : currentPrice.currentPrice(),
                currentPrice == null ? null : currentPrice.changeRate()
        );
    }

    private List<MasterChoiceResponse.RecommendedStockResponse> rank(
            List<MasterChoiceResponse.RecommendedStockResponse> recommendations
    ) {
        List<MasterChoiceResponse.RecommendedStockResponse> ranked = new ArrayList<>();
        for (int i = 0; i < recommendations.size(); i++) {
            MasterChoiceResponse.RecommendedStockResponse item = recommendations.get(i);
            ranked.add(new MasterChoiceResponse.RecommendedStockResponse(
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

    private Master findMasterById(Long masterId) {
        return masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(MasterErrorCode.MASTER_NOT_FOUND));
    }
}
