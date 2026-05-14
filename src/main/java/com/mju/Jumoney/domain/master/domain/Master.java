package com.mju.Jumoney.domain.master.domain;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "masters",
        indexes = {
                @Index(name = "idx_master_name", columnList = "master_name", unique = true)
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

    @Column(name = "recommendation_description", nullable = false, columnDefinition = "text")
    private String recommendationDescription;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Transient
    public MasterCode getMasterCode() {
        return MasterCode.fromLabel(masterName);
    }

    public static Master create(
            String masterName,
            String recommendationDescription,
            int displayOrder
    ) {
        return Master.builder()
                .masterName(masterName)
                .recommendationDescription(recommendationDescription)
                .displayOrder(displayOrder)
                .build();
    }

    public void updateContent(
            String masterName,
            String recommendationDescription,
            int displayOrder
    ) {
        this.masterName = masterName;
        this.recommendationDescription = recommendationDescription;
        this.displayOrder = displayOrder;
    }
}
