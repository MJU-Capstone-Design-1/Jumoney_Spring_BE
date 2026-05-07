package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.HtsStock;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HtsStockRepository extends JpaRepository<HtsStock, Long> {

    void deleteBySearchTypeAndBaseDate(HtsSearchType searchType, LocalDate baseDate);
}
