package com.mju.Jumoney.domain.masterchoice.service;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceCandidate;
import com.mju.Jumoney.domain.masterchoice.exception.MasterChoiceErrorCode;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterChoiceFilterService {

    private final StockIndicatorRepository stockIndicatorRepository;

    public List<MasterChoiceCandidate> findCandidates(
            MasterCode masterCode,
            Collection<MasterOptionLogicCode> selectedLogicCodes,
            Collection<SectorType> selectedSectorTypes
    ) {
        Set<MasterOptionLogicCode> logicCodes = normalizeLogicCodes(masterCode, selectedLogicCodes);
        Set<SectorType> sectorTypes = selectedSectorTypes == null || selectedSectorTypes.isEmpty()
                ? Set.of()
                : EnumSet.copyOf(selectedSectorTypes);

        String baseTime = stockIndicatorRepository.findLatestBaseTime()
                .orElseThrow(() -> new CustomException(MasterChoiceErrorCode.STOCK_INDICATOR_BASE_TIME_NOT_FOUND));

        List<StockIndicator> indicators = stockIndicatorRepository.findByBaseTimeWithStockAndSector(baseTime);
        List<MasterChoiceCandidate> candidates = new ArrayList<>();
        for (StockIndicator indicator : indicators) {
            MasterChoiceRuleEvaluator.Indicator ruleIndicator = MasterChoiceRuleEvaluator.fromStockIndicator(indicator);
            MasterChoiceCandidate candidate = evaluate(indicator, ruleIndicator, logicCodes, sectorTypes);
            if (candidate.matchedConditionCount() == logicCodes.size()) {
                candidate.setSortMetricValue(MasterChoiceRuleEvaluator.sortMetricValue(masterCode, ruleIndicator));
                if (masterCode == MasterCode.PETER_LYNCH) {
                    candidate.setFallbackSortMetricValue(indicator.getSalesGrowthRate());
                }
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private Set<MasterOptionLogicCode> normalizeLogicCodes(
            MasterCode masterCode,
            Collection<MasterOptionLogicCode> selectedLogicCodes
    ) {
        Set<MasterOptionLogicCode> logicCodes = selectedLogicCodes == null || selectedLogicCodes.isEmpty()
                ? defaultLogicCodes(masterCode)
                : EnumSet.copyOf(selectedLogicCodes);

        boolean hasInvalidCode = logicCodes.stream()
                .anyMatch(logicCode -> logicCode.getMasterCode() != masterCode);
        if (hasInvalidCode) {
            throw new CustomException(MasterChoiceErrorCode.INVALID_MASTER_OPTION_SELECTION);
        }
        return logicCodes;
    }

    private Set<MasterOptionLogicCode> defaultLogicCodes(MasterCode masterCode) {
        EnumSet<MasterOptionLogicCode> logicCodes = EnumSet.noneOf(MasterOptionLogicCode.class);
        Arrays.stream(MasterOptionLogicCode.values())
                .filter(logicCode -> logicCode.getMasterCode() == masterCode)
                .forEach(logicCodes::add);
        return logicCodes;
    }

    private MasterChoiceCandidate evaluate(
            StockIndicator indicator,
            MasterChoiceRuleEvaluator.Indicator ruleIndicator,
            Set<MasterOptionLogicCode> logicCodes,
            Set<SectorType> selectedSectorTypes
    ) {
        MasterChoiceCandidate candidate = new MasterChoiceCandidate(indicator);
        for (MasterOptionLogicCode logicCode : logicCodes) {
            if (MasterChoiceRuleEvaluator.matches(ruleIndicator, logicCode, selectedSectorTypes)) {
                candidate.addMatchedOption(logicCode);
            }
        }
        return candidate;
    }
}
