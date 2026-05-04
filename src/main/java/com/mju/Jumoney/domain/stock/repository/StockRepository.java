package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {
}
