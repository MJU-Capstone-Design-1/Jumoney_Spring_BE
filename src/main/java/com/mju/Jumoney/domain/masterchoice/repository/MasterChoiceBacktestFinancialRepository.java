package com.mju.Jumoney.domain.masterchoice.repository;

import com.mju.Jumoney.domain.masterchoice.domain.MasterChoiceBacktestFinancial;
import com.mju.Jumoney.domain.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterChoiceBacktestFinancialRepository extends JpaRepository<MasterChoiceBacktestFinancial, Long> {

    Optional<MasterChoiceBacktestFinancial> findByStockAndSettlementYearMonth(Stock stock, String settlementYearMonth);

    List<MasterChoiceBacktestFinancial> findByStockOrderByAvailableDateAsc(Stock stock);

    long countByStock(Stock stock);

    Optional<MasterChoiceBacktestFinancial> findTopByStockOrderBySettlementYearMonthDesc(Stock stock);
}
