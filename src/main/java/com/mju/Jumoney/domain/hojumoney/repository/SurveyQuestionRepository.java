package com.mju.Jumoney.domain.hojumoney.repository;

import com.mju.Jumoney.domain.hojumoney.domain.SurveyQuestion;
import com.mju.Jumoney.domain.hojumoney.enums.SurveyQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    Optional<SurveyQuestion> findByQuestionType(SurveyQuestionType questionType);

    List<SurveyQuestion> findAllByOrderByDisplayOrderAsc();
}
