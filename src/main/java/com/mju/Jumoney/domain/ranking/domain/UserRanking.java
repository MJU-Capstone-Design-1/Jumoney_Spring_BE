package com.mju.Jumoney.domain.ranking.domain;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_rankings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_ranking_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_user_ranking_overall_rank", columnList = "overall_rank"),
                @Index(name = "idx_user_ranking_master_rank", columnList = "master_id, master_rank")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class UserRanking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_ranking_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id")
    private Master master;

    @Column(name = "total_asset", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAsset;

    @Column(name = "total_profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalProfitRate;

    @Column(name = "overall_rank", nullable = false)
    private int overallRank;

    @Column(name = "master_rank")
    private Integer masterRank;

    @Column(name = "ranking_date", nullable = false)
    private LocalDate rankingDate;

    public static UserRanking create(User user,
                                     Master master,
                                     BigDecimal totalAsset,
                                     BigDecimal totalProfitRate,
                                     int overallRank,
                                     Integer masterRank,
                                     LocalDate rankingDate) {
        return UserRanking.builder()
                .user(user)
                .master(master)
                .totalAsset(totalAsset)
                .totalProfitRate(totalProfitRate)
                .overallRank(overallRank)
                .masterRank(masterRank)
                .rankingDate(rankingDate)
                .build();
    }
}
