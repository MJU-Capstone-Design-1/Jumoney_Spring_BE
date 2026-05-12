package com.mju.Jumoney.domain.stock.repository;

import com.mju.Jumoney.domain.stock.domain.HtsStock;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HtsStockRepository extends JpaRepository<HtsStock, Long> {

    void deleteBySearchTypeAndBaseDate(HtsSearchType searchType, LocalDate baseDate);

    @Query("""
            select max(hs.baseDate)
            from HtsStock hs
            """)
    Optional<LocalDate> findLatestBaseDate();

    @Query("""
            select hs
            from HtsStock hs
            join fetch hs.stock
            where hs.searchType = :searchType
              and hs.baseDate = :baseDate
            """)
    List<HtsStock> findBySearchTypeAndBaseDateWithStock(
            @Param("searchType") HtsSearchType searchType,
            @Param("baseDate") LocalDate baseDate
    );
}
