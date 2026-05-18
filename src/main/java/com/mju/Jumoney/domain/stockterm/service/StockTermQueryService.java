package com.mju.Jumoney.domain.stockterm.service;

import com.mju.Jumoney.domain.stockterm.domain.StockTerm;
import com.mju.Jumoney.domain.stockterm.domain.StockTermLearning;
import com.mju.Jumoney.domain.stockterm.domain.StockTermScrap;
import com.mju.Jumoney.domain.stockterm.dto.ScrappedStockTermResponse;
import com.mju.Jumoney.domain.stockterm.dto.StockTermCategoryResponse;
import com.mju.Jumoney.domain.stockterm.dto.StockTermCategoryTermsResponse;
import com.mju.Jumoney.domain.stockterm.dto.StockTermDetailResponse;
import com.mju.Jumoney.domain.stockterm.dto.StockTermSummaryResponse;
import com.mju.Jumoney.domain.stockterm.exception.StockTermErrorCode;
import com.mju.Jumoney.domain.stockterm.repository.StockTermLearningRepository;
import com.mju.Jumoney.domain.stockterm.repository.StockTermRepository;
import com.mju.Jumoney.domain.stockterm.repository.StockTermScrapRepository;
import com.mju.Jumoney.domain.stockterm.enums.StockTermCategory;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTermQueryService {

    private final StockTermRepository stockTermRepository;
    private final StockTermScrapRepository stockTermScrapRepository;
    private final StockTermLearningRepository stockTermLearningRepository;
    private final UserRepository userRepository;

    public List<StockTermCategoryResponse> getCategories() {
        return Arrays.stream(StockTermCategory.values())
                .map(category -> new StockTermCategoryResponse(
                        category.getCategoryId(),
                        category.getLabel()
                ))
                .toList();
    }

    public StockTermCategoryTermsResponse getTermsByCategory(Long userId, int categoryId) {
        StockTermCategory category = resolveCategory(categoryId);
        List<StockTerm> stockTerms = findStockTermsByCategory(category);
        if (stockTerms.isEmpty()) {
            return new StockTermCategoryTermsResponse(category.getCategoryId(), category.getLabel(), List.of());
        }

        List<Long> termIds = extractTermIds(stockTerms);
        Set<Long> scrappedTermIds = findScrappedTermIds(userId, termIds);
        Set<Long> learnedTermIds = findLearnedTermIds(userId, termIds);

        return toCategoryTermsResponse(category, stockTerms, scrappedTermIds, learnedTermIds);
    }

    public List<ScrappedStockTermResponse> getScrappedTerms(Long userId) {
        List<StockTermScrap> scraps = findScrapsByUserId(userId);
        if (scraps.isEmpty()) {
            return List.of();
        }

        List<Long> termIds = scraps.stream()
                .map(scrap -> scrap.getStockTerm().getId())
                .toList();
        Set<Long> learnedTermIds = findLearnedTermIds(userId, termIds);

        return scraps.stream()
                .map(scrap -> toScrappedStockTermResponse(scrap, learnedTermIds))
                .toList();
    }

    @Transactional
    public StockTermDetailResponse getTermDetail(Long userId, Long termId) {
        StockTerm stockTerm = findStockTermById(termId);
        boolean isScrapped = stockTermScrapRepository.existsByUserIdAndStockTermId(userId, termId);
        trackLearningIfNeeded(userId, termId, stockTerm);
        return toDetailResponse(stockTerm, isScrapped);
    }

    // ========== 조회 메서드 ==========
    private StockTermCategory resolveCategory(int categoryId) {
        try {
            return StockTermCategory.fromCategoryId(categoryId);
        } catch (IllegalArgumentException e) {
            throw new CustomException(StockTermErrorCode.STOCK_TERM_CATEGORY_NOT_FOUND);
        }
    }

    private List<StockTerm> findStockTermsByCategory(StockTermCategory category) {
        return stockTermRepository.findByCategoryOrderByIdAsc(category);
    }

    private StockTerm findStockTermById(Long termId) {
        return stockTermRepository.findById(termId)
                .orElseThrow(() -> new CustomException(StockTermErrorCode.STOCK_TERM_NOT_FOUND));
    }

    private List<Long> extractTermIds(List<StockTerm> stockTerms) {
        return stockTerms.stream()
                .map(StockTerm::getId)
                .toList();
    }

    private Set<Long> findScrappedTermIds(Long userId, List<Long> termIds) {
        return stockTermScrapRepository.findByUserIdAndStockTermIdIn(userId, termIds).stream()
                .map(scrap -> scrap.getStockTerm().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> findLearnedTermIds(Long userId, List<Long> termIds) {
        return stockTermLearningRepository.findByUserIdAndStockTermIdIn(userId, termIds).stream()
                .map(learning -> learning.getStockTerm().getId())
                .collect(Collectors.toSet());
    }

    private List<StockTermScrap> findScrapsByUserId(Long userId) {
        return stockTermScrapRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ========== 비즈니스 메서드 ==========
    private void trackLearningIfNeeded(Long userId, Long termId, StockTerm stockTerm) {
        if (stockTermLearningRepository.existsByUserIdAndStockTermId(userId, termId)) {
            return;
        }

        User user = userRepository.getReferenceById(userId);
        try {
            stockTermLearningRepository.save(StockTermLearning.create(stockTerm, user));
        } catch (DataIntegrityViolationException ignored) {
            // 동시 요청으로 이미 학습 기록이 생성된 경우 무시
        }
    }

    private StockTermCategoryTermsResponse toCategoryTermsResponse(
            StockTermCategory category,
            List<StockTerm> stockTerms,
            Set<Long> scrappedTermIds,
            Set<Long> learnedTermIds
    ) {
        List<StockTermSummaryResponse> terms = stockTerms.stream()
                .map(term -> new StockTermSummaryResponse(
                        term.getId(),
                        term.getTermName(),
                        scrappedTermIds.contains(term.getId()),
                        learnedTermIds.contains(term.getId())
                ))
                .toList();

        return new StockTermCategoryTermsResponse(
                category.getCategoryId(),
                category.getLabel(),
                terms
        );
    }

    private StockTermDetailResponse toDetailResponse(StockTerm stockTerm, boolean isScrapped) {
        return new StockTermDetailResponse(
                stockTerm.getId(),
                stockTerm.getCategory().getCategoryId(),
                stockTerm.getCategory().getLabel(),
                stockTerm.getTermName(),
                stockTerm.getDescription(),
                isScrapped,
                true
        );
    }

    private ScrappedStockTermResponse toScrappedStockTermResponse(StockTermScrap scrap, Set<Long> learnedTermIds) {
        StockTerm term = scrap.getStockTerm();
        return new ScrappedStockTermResponse(
                term.getId(),
                term.getCategory().getLabel(),
                term.getTermName(),
                learnedTermIds.contains(term.getId())
        );
    }
}
