package com.mju.Jumoney.domain.stockterm.repository;

import com.mju.Jumoney.domain.stockterm.domain.StockTermLearning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StockTermLearningRepository extends JpaRepository<StockTermLearning, Long> {

    List<StockTermLearning> findByUserIdAndStockTermIdIn(Long userId, Collection<Long> stockTermIds);

    boolean existsByUserIdAndStockTermId(Long userId, Long stockTermId);
}
