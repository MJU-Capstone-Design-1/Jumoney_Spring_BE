package com.mju.Jumoney.domain.stockterm.domain;

import com.mju.Jumoney.domain.stockterm.enums.StockTermCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "stock_terms",
        indexes = {
                @Index(name = "idx_stock_term_category", columnList = "category"),
                @Index(name = "idx_stock_term_term_name", columnList = "term_name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class StockTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private StockTermCategory category;

    @Column(name = "term_name", nullable = false, length = 50)
    private String termName;

    @Column(name = "subtitle", nullable = false, length = 100)
    private String subtitle;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "image_file_name", nullable = false, length = 100)
    private String imageFileName;

    public static StockTerm create(
            StockTermCategory category,
            String termName,
            String subtitle,
            String description,
            String imageFileName
    ) {
        return StockTerm.builder()
                .category(category)
                .termName(termName)
                .subtitle(subtitle)
                .description(description)
                .imageFileName(imageFileName)
                .build();
    }

    public void updateContent(String subtitle, String description, String imageFileName) {
        this.subtitle = subtitle;
        this.description = description;
        this.imageFileName = imageFileName;
    }
}
