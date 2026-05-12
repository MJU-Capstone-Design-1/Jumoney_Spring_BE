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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final SurveyOptionRestrictionRepository surveyOptionRestrictionRepository;

    public HojumoneySurveyResponse getHojumoneySurvey() {
        List<HojumoneySurveyResponse.SurveyQuestionResponse> questions = surveyQuestionRepository
                .findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toQuestionResponse)
                .toList();

        return new HojumoneySurveyResponse(questions);
    }

    private HojumoneySurveyResponse.SurveyQuestionResponse toQuestionResponse(SurveyQuestion question) {
        List<HojumoneySurveyResponse.SurveyOptionResponse> options = surveyOptionRepository
                .findByQuestionIdOrderByDisplayOrderAsc(question.getId())
                .stream()
                .map(this::toOptionResponse)
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

    private HojumoneySurveyResponse.SurveyOptionResponse toOptionResponse(SurveyOption option) {
        List<Long> restrictedOptionIds = surveyOptionRestrictionRepository.findBySourceOptionId(option.getId())
                .stream()
                .map(restriction -> restriction.getRestrictedOption().getId())
                .toList();

        return new HojumoneySurveyResponse.SurveyOptionResponse(
                option.getId(),
                option.getContent(),
                option.getLogicCode().name(),
                option.getDisplayOrder(),
                restrictedOptionIds,
                option.getDescription()
        );
    }
}
