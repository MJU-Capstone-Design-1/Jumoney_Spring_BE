package com.mju.Jumoney.domain.recommendation.domain;

import com.mju.Jumoney.domain.recommendation.enums.RecommendationType;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "recommendations",
        indexes = {
                @Index(name = "idx_recommendation_user_type_created_at", columnList = "user_id,recommendation_type,created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 30)
    private RecommendationType recommendationType;

    public static Recommendation create(User user, RecommendationType recommendationType) {
        return Recommendation.builder()
                .user(user)
                .recommendationType(recommendationType)
                .build();
    }
}
