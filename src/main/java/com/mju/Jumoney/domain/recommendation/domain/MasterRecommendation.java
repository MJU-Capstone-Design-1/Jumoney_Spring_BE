package com.mju.Jumoney.domain.recommendation.domain;

import com.mju.Jumoney.domain.master.domain.Master;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "master_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_recommendation_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false, unique = true)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> selectedOptionIds;

    public static MasterRecommendation create(
            Recommendation recommendation,
            Master master,
            List<Long> selectedOptionIds
    ) {
        return MasterRecommendation.builder()
                .recommendation(recommendation)
                .master(master)
                .selectedOptionIds(List.copyOf(selectedOptionIds))
                .build();
    }
}
