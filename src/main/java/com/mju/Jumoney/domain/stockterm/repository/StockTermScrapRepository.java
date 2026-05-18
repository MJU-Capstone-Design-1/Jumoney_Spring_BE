package com.mju.Jumoney.domain.stockterm.repository;

import com.mju.Jumoney.domain.stockterm.domain.StockTermScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockTermScrapRepository extends JpaRepository<StockTermScrap, Long> {

    List<StockTermScrap> findByUserIdAndStockTermIdIn(Long userId, Collection<Long> stockTermIds);

    boolean existsByUserIdAndStockTermId(Long userId, Long stockTermId);

    Optional<StockTermScrap> findByUserIdAndStockTermId(Long userId, Long stockTermId);

    @EntityGraph(attributePaths = "stockTerm")
    List<StockTermScrap> findByUserIdOrderByCreatedAtDesc(Long userId);
}
