package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.recommendation.domain.SurveyOption;
import com.mju.Jumoney.domain.recommendation.domain.SurveyQuestion;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneySurveyResponse;
import com.mju.Jumoney.domain.recommendation.repository.SurveyOptionRepository;
import com.mju.Jumoney.domain.recommendation.repository.SurveyOptionRestrictionRepository;
import com.mju.Jumoney.domain.recommendation.repository.SurveyQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final SurveyOptionRestrictionRepository surveyOptionRestrictionRepository;

    public HojumoneySurveyResponse getHojumoneySurvey() {
        List<SurveyQuestion> surveyQuestions = surveyQuestionRepository.findAllByOrderByDisplayOrderAsc();
        List<Long> questionIds = surveyQuestions.stream()
                .map(SurveyQuestion::getId)
                .toList();

        Map<Long, List<SurveyOption>> optionsByQuestionId = surveyOptionRepository
                .findByQuestionIdInOrderByQuestionDisplayOrderAscDisplayOrderAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));

        List<Long> optionIds = optionsByQuestionId.values().stream()
                .flatMap(List::stream)
                .map(SurveyOption::getId)
                .toList();
        Map<Long, List<Long>> restrictedOptionIdsBySourceOptionId = surveyOptionRestrictionRepository
                .findBySourceOptionIdIn(optionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        restriction -> restriction.getSourceOption().getId(),
                        Collectors.mapping(restriction -> restriction.getRestrictedOption().getId(), Collectors.toList())
                ));

        List<HojumoneySurveyResponse.SurveyQuestionResponse> questions = surveyQuestions.stream()
                .map(question -> toQuestionResponse(question, optionsByQuestionId, restrictedOptionIdsBySourceOptionId))
                .toList();

        return new HojumoneySurveyResponse(questions);
    }

    private HojumoneySurveyResponse.SurveyQuestionResponse toQuestionResponse(
            SurveyQuestion question,
            Map<Long, List<SurveyOption>> optionsByQuestionId,
            Map<Long, List<Long>> restrictedOptionIdsBySourceOptionId
    ) {
        List<HojumoneySurveyResponse.SurveyOptionResponse> options = optionsByQuestionId
                .getOrDefault(question.getId(), List.of())
                .stream()
                .map(option -> toOptionResponse(option, restrictedOptionIdsBySourceOptionId))
                .toList();

        return new HojumoneySurveyResponse.SurveyQuestionResponse(
                question.getId(),
                question.getQuestionType(),
                question.getContent(),
                question.getDescription(),
                question.getDisplayOrder(),
                options
        );
    }

    private HojumoneySurveyResponse.SurveyOptionResponse toOptionResponse(
            SurveyOption option,
            Map<Long, List<Long>> restrictedOptionIdsBySourceOptionId
    ) {
        return new HojumoneySurveyResponse.SurveyOptionResponse(
                option.getId(),
                option.getLogicCode(),
                option.getContent(),
                option.getDisplayOrder(),
                restrictedOptionIdsBySourceOptionId.getOrDefault(option.getId(), List.of()),
                option.getDescription()
        );
    }
}
