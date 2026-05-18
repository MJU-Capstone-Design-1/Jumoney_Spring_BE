package com.mju.Jumoney.domain.master.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(
        name = "master_principles",
        indexes = {
                @Index(name = "idx_master_principle_master_id", columnList = "master_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterPrinciple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_principle_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> details;

    public static MasterPrinciple create(
            Master master,
            String title,
            String description,
            List<String> details
    ) {
        return MasterPrinciple.builder()
                .master(master)
                .title(title)
                .description(description)
                .details(details == null ? null : List.copyOf(details))
                .build();
    }
}
