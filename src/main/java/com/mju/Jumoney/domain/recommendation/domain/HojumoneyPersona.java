package com.mju.Jumoney.domain.recommendation.domain;

import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "hojumoney_personas",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hojumoney_persona_combination",
                        columnNames = {"investment_purpose", "risk_profile", "investment_horizon"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_hojumoney_persona_combination",
                        columnList = "investment_purpose,risk_profile,investment_horizon"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class HojumoneyPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hojumoney_persona_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_purpose", nullable = false, length = 40)
    private SurveyLogicCode investmentPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", nullable = false, length = 40)
    private SurveyLogicCode riskProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_horizon", nullable = false, length = 40)
    private SurveyLogicCode investmentHorizon;

    @Column(name = "persona_name", nullable = false, length = 100)
    private String personaName;

    @Column(name = "persona_description", nullable = false, columnDefinition = "text")
    private String personaDescription;

    public static HojumoneyPersona create(
            SurveyLogicCode investmentPurpose,
            SurveyLogicCode riskProfile,
            SurveyLogicCode investmentHorizon,
            String personaName,
            String personaDescription
    ) {
        return HojumoneyPersona.builder()
                .investmentPurpose(investmentPurpose)
                .riskProfile(riskProfile)
                .investmentHorizon(investmentHorizon)
                .personaName(personaName)
                .personaDescription(personaDescription)
                .build();
    }

    public void updateContent(String personaName, String personaDescription) {
        this.personaName = personaName;
        this.personaDescription = personaDescription;
    }
}
