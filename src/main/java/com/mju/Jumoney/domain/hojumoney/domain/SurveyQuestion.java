package com.mju.Jumoney.domain.hojumoney.domain;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyQuestionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "survey_questions",
        indexes = {
                @Index(name = "idx_survey_question_type", columnList = "question_type", unique = true),
                @Index(name = "idx_survey_question_display_order", columnList = "display_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class SurveyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_question_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, unique = true, length = 30)
    private SurveyQuestionType questionType;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public static SurveyQuestion create(
            SurveyQuestionType questionType,
            String content,
            String description,
            int displayOrder
    ) {
        return SurveyQuestion.builder()
                .questionType(questionType)
                .content(content)
                .description(description)
                .displayOrder(displayOrder)
                .build();
    }

    public void updateContent(String content, String description, int displayOrder) {
        this.content = content;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
