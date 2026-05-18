package com.mju.Jumoney.domain.hojumoney.domain;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.recommendation.domain.Recommendation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "hojumoney_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class HojumoneyRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hojumoney_recommendation_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false, unique = true)
    private Recommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_purpose", nullable = false, length = 40)
    private SurveyLogicCode investmentPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", nullable = false, length = 40)
    private SurveyLogicCode riskProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_horizon", nullable = false, length = 40)
    private SurveyLogicCode investmentHorizon;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> selectedOptionIds;

    @Column(name = "persona_name", nullable = false, length = 100)
    private String personaName;

    @Column(name = "persona_description", nullable = false, columnDefinition = "text")
    private String personaDescription;

    public static HojumoneyRecommendation create(
            Recommendation recommendation,
            SurveyLogicCode investmentPurpose,
            SurveyLogicCode riskProfile,
            SurveyLogicCode investmentHorizon,
            List<Long> selectedOptionIds,
            String personaName,
            String personaDescription
    ) {
        return HojumoneyRecommendation.builder()
                .recommendation(recommendation)
                .investmentPurpose(investmentPurpose)
                .riskProfile(riskProfile)
                .investmentHorizon(investmentHorizon)
                .selectedOptionIds(List.copyOf(selectedOptionIds))
                .personaName(personaName)
                .personaDescription(personaDescription)
                .build();
    }
}
