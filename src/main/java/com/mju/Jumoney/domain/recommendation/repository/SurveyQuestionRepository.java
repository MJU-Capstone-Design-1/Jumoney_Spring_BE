package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.SurveyQuestion;
import com.mju.Jumoney.domain.recommendation.enums.SurveyQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    Optional<SurveyQuestion> findByQuestionType(SurveyQuestionType questionType);

    List<SurveyQuestion> findAllByOrderByDisplayOrderAsc();
}
