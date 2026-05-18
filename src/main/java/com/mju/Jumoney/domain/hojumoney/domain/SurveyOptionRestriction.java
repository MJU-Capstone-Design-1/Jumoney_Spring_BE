package com.mju.Jumoney.domain.hojumoney.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "survey_option_restrictions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_survey_option_restriction_source_restricted",
                        columnNames = {"source_option_id", "restricted_option_id"}
                )
        },
        indexes = {
                @Index(name = "idx_survey_option_restriction_source", columnList = "source_option_id"),
                @Index(name = "idx_survey_option_restriction_restricted", columnList = "restricted_option_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class SurveyOptionRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restriction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_option_id", nullable = false)
    private SurveyOption sourceOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restricted_option_id", nullable = false)
    private SurveyOption restrictedOption;

    public static SurveyOptionRestriction create(SurveyOption sourceOption, SurveyOption restrictedOption) {
        return SurveyOptionRestriction.builder()
                .sourceOption(sourceOption)
                .restrictedOption(restrictedOption)
                .build();
    }
}
