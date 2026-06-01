package com.mju.Jumoney.domain.masterchoice.repository;

import com.mju.Jumoney.domain.masterchoice.domain.MasterChoiceBacktestDailyIndicator;
import com.mju.Jumoney.domain.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MasterChoiceBacktestDailyIndicatorRepository extends JpaRepository<MasterChoiceBacktestDailyIndicator, Long> {

    Optional<MasterChoiceBacktestDailyIndicator> findByStockAndTradeDate(Stock stock, LocalDate tradeDate);

    List<MasterChoiceBacktestDailyIndicator> findByStockAndTradeDateBetweenOrderByTradeDateAsc(Stock stock,
                                                                                               LocalDate fromDate,
                                                                                               LocalDate toDate);

    long countByStock(Stock stock);

    Optional<MasterChoiceBacktestDailyIndicator> findTopByStockOrderByTradeDateDesc(Stock stock);
}
