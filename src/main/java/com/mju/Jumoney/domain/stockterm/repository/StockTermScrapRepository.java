package com.mju.Jumoney.domain.stockterm.repository;

import com.mju.Jumoney.domain.stockterm.domain.StockTermScrap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StockTermScrapRepository extends JpaRepository<StockTermScrap, Long> {

    List<StockTermScrap> findByUserIdAndStockTermIdIn(Long userId, Collection<Long> stockTermIds);

    boolean existsByUserIdAndStockTermId(Long userId, Long stockTermId);
}
