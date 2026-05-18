package com.mju.Jumoney.domain.stockterm.domain;

import com.mju.Jumoney.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_term_learnings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_term_learning_user_term", columnNames = {"user_id", "term_id"})
        },
        indexes = {
                @Index(name = "idx_stock_term_learning_user_id", columnList = "user_id"),
                @Index(name = "idx_stock_term_learning_term_id", columnList = "term_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class StockTermLearning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private StockTerm stockTerm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public static StockTermLearning create(StockTerm stockTerm, User user) {
        return StockTermLearning.builder()
                .stockTerm(stockTerm)
                .user(user)
                .build();
    }
}
