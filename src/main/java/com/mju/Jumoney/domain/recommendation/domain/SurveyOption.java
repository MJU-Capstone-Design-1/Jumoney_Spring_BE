package com.mju.Jumoney.domain.recommendation.domain;

import com.mju.Jumoney.domain.recommendation.dto.SurveyOptionIndicatorDescription;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(
        name = "survey_options",
        indexes = {
                @Index(name = "idx_survey_option_question_id", columnList = "survey_question_id"),
                @Index(name = "idx_survey_option_logic_code", columnList = "logic_code", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class SurveyOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_question_id", nullable = false)
    private SurveyQuestion question;

    @Column(nullable = false, length = 255)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "logic_code", nullable = false, unique = true, length = 40)
    private SurveyLogicCode logicCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<SurveyOptionIndicatorDescription> description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public static SurveyOption create(
            SurveyQuestion question,
            String content,
            SurveyLogicCode logicCode,
            List<SurveyOptionIndicatorDescription> description,
            int displayOrder
    ) {
        return SurveyOption.builder()
                .question(question)
                .content(content)
                .logicCode(logicCode)
                .description(description)
                .displayOrder(displayOrder)
                .build();
    }
}
