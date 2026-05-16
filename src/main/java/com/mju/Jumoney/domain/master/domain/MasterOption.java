package com.mju.Jumoney.domain.master.domain;

import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "master_options",
        indexes = {
                @Index(name = "idx_master_option_master_id", columnList = "master_id"),
                @Index(name = "idx_master_option_logic_code", columnList = "logic_code", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;

    @Column(nullable = false, length = 100)
    private String content;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "logic_code", nullable = false, unique = true, length = 50)
    private MasterOptionLogicCode logicCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public static MasterOption create(
            Master master,
            String content,
            String description,
            MasterOptionLogicCode logicCode,
            int displayOrder
    ) {
        return MasterOption.builder()
                .master(master)
                .content(content)
                .description(description)
                .logicCode(logicCode)
                .displayOrder(displayOrder)
                .build();
    }

    public void updateContent(Master master, String content, String description, int displayOrder) {
        this.master = master;
        this.content = content;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
