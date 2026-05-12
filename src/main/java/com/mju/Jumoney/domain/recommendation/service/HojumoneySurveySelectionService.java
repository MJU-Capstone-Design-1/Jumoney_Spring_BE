package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.recommendation.domain.SurveyOption;
import com.mju.Jumoney.domain.recommendation.domain.SurveyOptionRestriction;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneySurveySelection;
import com.mju.Jumoney.domain.recommendation.enums.SurveyQuestionType;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.recommendation.repository.SurveyOptionRepository;
import com.mju.Jumoney.domain.recommendation.repository.SurveyOptionRestrictionRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HojumoneySurveySelectionService {

    private static final int REQUIRED_SELECTION_COUNT = 3;

    private final SurveyOptionRepository surveyOptionRepository;
    private final SurveyOptionRestrictionRepository surveyOptionRestrictionRepository;

    public HojumoneySurveySelection validateAndClassify(List<Long> selectedOptionIds) {
        validateSelectionCount(selectedOptionIds);

        List<Long> distinctOptionIds = selectedOptionIds.stream()
                .distinct()
                .toList();
        if (distinctOptionIds.size() != REQUIRED_SELECTION_COUNT) {
            throw new CustomException(RecommendationErrorCode.INVALID_SURVEY_SELECTION_COUNT);
        }

        List<SurveyOption> selectedOptions = surveyOptionRepository.findByIdIn(distinctOptionIds);
        if (selectedOptions.size() != REQUIRED_SELECTION_COUNT) {
            throw new CustomException(RecommendationErrorCode.SURVEY_OPTION_NOT_FOUND);
        }

        Map<Long, SurveyOption> selectedOptionMap = selectedOptions.stream()
                .collect(Collectors.toMap(SurveyOption::getId, Function.identity()));
        List<SurveyOption> orderedSelectedOptions = distinctOptionIds.stream()
                .map(selectedOptionMap::get)
                .toList();

        validateRestrictedOptions(orderedSelectedOptions);

        EnumMap<SurveyQuestionType, SurveyOption> optionsByQuestionType = new EnumMap<>(SurveyQuestionType.class);
        for (SurveyOption option : orderedSelectedOptions) {
            SurveyQuestionType questionType = option.getQuestion().getQuestionType();
            if (option.getLogicCode().getQuestionType() != questionType) {
                throw new CustomException(RecommendationErrorCode.INVALID_SURVEY_LOGIC_CODE);
            }
            if (optionsByQuestionType.put(questionType, option) != null) {
                throw new CustomException(RecommendationErrorCode.DUPLICATE_SURVEY_QUESTION_SELECTION);
            }
        }

        SurveyOption investmentPurposeOption = requiredOption(optionsByQuestionType, SurveyQuestionType.INVESTMENT_PURPOSE);
        SurveyOption riskProfileOption = requiredOption(optionsByQuestionType, SurveyQuestionType.RISK_PROFILE);
        SurveyOption investmentHorizonOption = requiredOption(optionsByQuestionType, SurveyQuestionType.INVESTMENT_HORIZON);

        return new HojumoneySurveySelection(
                investmentPurposeOption,
                investmentPurposeOption.getLogicCode(),
                riskProfileOption,
                riskProfileOption.getLogicCode(),
                investmentHorizonOption,
                investmentHorizonOption.getLogicCode(),
                orderedSelectedOptions
        );
    }

    private void validateSelectionCount(List<Long> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.size() != REQUIRED_SELECTION_COUNT) {
            throw new CustomException(RecommendationErrorCode.INVALID_SURVEY_SELECTION_COUNT);
        }
    }

    private void validateRestrictedOptions(List<SurveyOption> selectedOptions) {
        Set<Long> selectedOptionIds = selectedOptions.stream()
                .map(SurveyOption::getId)
                .collect(Collectors.toCollection(HashSet::new));

        boolean hasRestrictedSelection = surveyOptionRestrictionRepository.findBySourceOptionIdIn(selectedOptionIds).stream()
                .map(SurveyOptionRestriction::getRestrictedOption)
                .map(SurveyOption::getId)
                .anyMatch(selectedOptionIds::contains);
        if (hasRestrictedSelection) {
            throw new CustomException(RecommendationErrorCode.RESTRICTED_SURVEY_OPTION_SELECTION);
        }
    }

    private SurveyOption requiredOption(
            Map<SurveyQuestionType, SurveyOption> optionsByQuestionType,
            SurveyQuestionType questionType
    ) {
        SurveyOption option = optionsByQuestionType.get(questionType);
        if (option == null) {
            throw new CustomException(RecommendationErrorCode.MISSING_SURVEY_QUESTION_SELECTION);
        }
        return option;
    }
}
