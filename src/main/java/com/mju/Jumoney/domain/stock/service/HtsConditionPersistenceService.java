package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.domain.stock.domain.HtsStock;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import com.mju.Jumoney.domain.stock.repository.HtsStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HtsConditionPersistenceService {

    private final HtsStockRepository htsStockRepository;

    // REQUIRES_NEW -> 새로운 트랜잭션 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replace(HtsSearchType searchType, LocalDate baseDate, List<Stock> stocks) {
        List<HtsStock> htsStocks = stocks.stream()
                .map(stock -> HtsStock.create(stock, searchType, baseDate))
                .toList();

        htsStockRepository.deleteBySearchTypeAndBaseDate(searchType, baseDate);
        htsStockRepository.flush();
        htsStockRepository.saveAll(htsStocks);
    }
}
