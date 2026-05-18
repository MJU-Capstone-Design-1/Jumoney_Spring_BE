package com.mju.Jumoney.domain.stockterm.service;

import com.mju.Jumoney.domain.stockterm.domain.StockTerm;
import com.mju.Jumoney.domain.stockterm.domain.StockTermScrap;
import com.mju.Jumoney.domain.stockterm.dto.StockTermScrapToggleResponse;
import com.mju.Jumoney.domain.stockterm.exception.StockTermErrorCode;
import com.mju.Jumoney.domain.stockterm.repository.StockTermRepository;
import com.mju.Jumoney.domain.stockterm.repository.StockTermScrapRepository;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockTermCommandService {

    private final StockTermRepository stockTermRepository;
    private final StockTermScrapRepository stockTermScrapRepository;
    private final UserRepository userRepository;

    @Transactional
    public StockTermScrapToggleResponse toggleScrap(Long userId, Long termId) {
        StockTerm stockTerm = findStockTermById(termId);

        return stockTermScrapRepository.findByUserIdAndStockTermId(userId, termId)
                .map(existingScrap -> {
                    stockTermScrapRepository.delete(existingScrap);
                    return new StockTermScrapToggleResponse(termId, false);
                })
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    stockTermScrapRepository.save(StockTermScrap.create(stockTerm, user));
                    return new StockTermScrapToggleResponse(termId, true);
                });
    }

    private StockTerm findStockTermById(Long termId) {
        return stockTermRepository.findById(termId)
                .orElseThrow(() -> new CustomException(StockTermErrorCode.STOCK_TERM_NOT_FOUND));
    }
}
