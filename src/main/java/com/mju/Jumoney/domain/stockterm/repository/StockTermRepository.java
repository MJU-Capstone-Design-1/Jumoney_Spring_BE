package com.mju.Jumoney.domain.stockterm.repository;

import com.mju.Jumoney.domain.stockterm.domain.StockTerm;
import com.mju.Jumoney.domain.stockterm.enums.StockTermCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockTermRepository extends JpaRepository<StockTerm, Long> {

    Optional<StockTerm> findByCategoryAndTermName(StockTermCategory category, String termName);

    List<StockTerm> findByCategoryOrderByIdAsc(StockTermCategory category);
}
