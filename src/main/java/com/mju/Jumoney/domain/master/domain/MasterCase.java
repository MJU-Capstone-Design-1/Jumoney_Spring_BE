package com.mju.Jumoney.domain.master.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "master_cases",
        indexes = {
                @Index(name = "idx_master_case_master_id", columnList = "master_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_case_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    @Column(nullable = false, length = 50)
    private String sector;

    @Column(name = "investment_period", nullable = false, length = 50)
    private String investmentPeriod;

    @Column(name = "investment_result", nullable = false, length = 100)
    private String investmentResult;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    public static MasterCase create(
            Master master,
            String stockName,
            String sector,
            String investmentPeriod,
            String investmentResult,
            String title,
            String description
    ) {
        return MasterCase.builder()
                .master(master)
                .stockName(stockName)
                .sector(sector)
                .investmentPeriod(investmentPeriod)
                .investmentResult(investmentResult)
                .title(title)
                .description(description)
                .build();
    }
}
