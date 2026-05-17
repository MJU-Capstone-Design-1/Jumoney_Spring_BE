package com.mju.Jumoney.domain.master.domain;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "masters",
        indexes = {
                @Index(name = "idx_master_name", columnList = "master_name", unique = true),
                @Index(name = "idx_master_code", columnList = "master_code", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Master {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_id")
    private Long id;

    @Column(name = "master_name", nullable = false, unique = true, length = 50)
    private String masterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "master_code", nullable = false, unique = true, length = 30)
    private MasterCode masterCode;

    @Column(length = 200)
    private String quote;

    @Column(name = "image_file_name", length = 200)
    private String imageFileName;

    @Column(name = "return_rate", length = 100)
    private String returnRate;

    @Column(name = "portfolio_base_period", nullable = false, length = 50)
    private String portfolioBasePeriod;

    @Column(name = "philosophy_title", nullable = false, length = 100)
    private String philosophyTitle;

    @Column(name = "philosophy_description", nullable = false, columnDefinition = "text")
    private String philosophyDescription;

    @Column(name = "recommendation_description", nullable = false, columnDefinition = "text")
    private String recommendationDescription;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public MasterCode getMasterCode() {
        return masterCode;
    }

    public static Master create(
            MasterCode masterCode,
            String masterName,
            String quote,
            String imageFileName,
            String returnRate,
            String portfolioBasePeriod,
            String philosophyTitle,
            String philosophyDescription,
            String recommendationDescription,
            int displayOrder
    ) {
        return Master.builder()
                .masterCode(masterCode)
                .masterName(masterName)
                .quote(quote)
                .imageFileName(imageFileName)
                .returnRate(returnRate)
                .portfolioBasePeriod(portfolioBasePeriod)
                .philosophyTitle(philosophyTitle)
                .philosophyDescription(philosophyDescription)
                .recommendationDescription(recommendationDescription)
                .displayOrder(displayOrder)
                .build();
    }

    public void updateContent(
            MasterCode masterCode,
            String masterName,
            String quote,
            String imageFileName,
            String returnRate,
            String portfolioBasePeriod,
            String philosophyTitle,
            String philosophyDescription,
            String recommendationDescription,
            int displayOrder
    ) {
        this.masterCode = masterCode;
        this.masterName = masterName;
        this.quote = quote;
        this.imageFileName = imageFileName;
        this.returnRate = returnRate;
        this.portfolioBasePeriod = portfolioBasePeriod;
        this.philosophyTitle = philosophyTitle;
        this.philosophyDescription = philosophyDescription;
        this.recommendationDescription = recommendationDescription;
        this.displayOrder = displayOrder;
    }
}
