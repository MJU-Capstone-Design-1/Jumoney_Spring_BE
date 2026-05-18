package com.mju.Jumoney.domain.hojumoney.repository;

import com.mju.Jumoney.domain.hojumoney.domain.SurveyOption;
import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {

    Optional<SurveyOption> findByLogicCode(SurveyLogicCode logicCode);

    List<SurveyOption> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);

    List<SurveyOption> findByQuestionIdInOrderByQuestionDisplayOrderAscDisplayOrderAsc(Collection<Long> questionIds);

    List<SurveyOption> findByIdIn(Collection<Long> ids);
}
