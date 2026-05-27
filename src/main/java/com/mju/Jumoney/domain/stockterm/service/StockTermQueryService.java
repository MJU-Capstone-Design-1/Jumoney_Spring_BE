package com.mju.Jumoney.domain.stockterm.service;

import com.mju.Jumoney.domain.stockterm.domain.StockTerm;
import com.mju.Jumoney.domain.stockterm.domain.StockTermLearning;
import com.mju.Jumoney.domain.stockterm.domain.StockTermScrap;
import com.mju.Jumoney.domain.stockterm.domain.TodayStockTerm;
import com.mju.Jumoney.domain.stockterm.dto.*;
import com.mju.Jumoney.domain.stockterm.enums.StockTermCategory;
import com.mju.Jumoney.domain.stockterm.exception.StockTermErrorCode;
import com.mju.Jumoney.domain.stockterm.repository.StockTermLearningRepository;
import com.mju.Jumoney.domain.stockterm.repository.StockTermRepository;
import com.mju.Jumoney.domain.stockterm.repository.StockTermScrapRepository;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTermQueryService {

    private final StockTermRepository stockTermRepository;
    private final StockTermScrapRepository stockTermScrapRepository;
    private final StockTermLearningRepository stockTermLearningRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Value("${home.stock-term.zone-id:Asia/Seoul}")
    private String zoneIdProperty;

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
    public TodayStockTermResponse getTodayStockTerm() {
        TodayStockTerm todayStockTerm = getOrCreateTodayStockTerm(resolveToday());
        return toTodayStockTermResponse(todayStockTerm.getStockTerm());
    }

    @Transactional
    public StockTermDetailResponse getTermDetail(Long userId, Long termId) {
        StockTerm stockTerm = findStockTermById(termId);
        boolean isScrapped = userId != null && stockTermScrapRepository.existsByUserIdAndStockTermId(userId, termId);
        boolean isLearned = userId != null;
        trackLearningIfNeeded(userId, termId, stockTerm);
        return toDetailResponse(stockTerm, isScrapped, isLearned);
    }

    @Scheduled(
            cron = "${home.stock-term.cron:0 0 0 * * *}",
            zone = "${home.stock-term.zone-id:Asia/Seoul}"
    )
    @Transactional
    public void selectTodayStockTerm() {
        LocalDate today = resolveToday();
        TodayStockTerm todayStockTerm = getOrCreateTodayStockTerm(today);
        log.info("[StockTermQueryService] 오늘의 주식 용어 선정 완료: date={}, termId={}, termName={}",
                today,
                todayStockTerm.getStockTerm().getId(),
                todayStockTerm.getStockTerm().getTermName());
    }

    // ========== 검증 메서드 ==========
    private StockTermCategory resolveCategory(int categoryId) {
        try {
            return StockTermCategory.fromCategoryId(categoryId);
        } catch (IllegalArgumentException e) {
            throw new CustomException(StockTermErrorCode.STOCK_TERM_CATEGORY_NOT_FOUND);
        }
    }

    // ========== 조회 메서드 ==========
    private List<StockTerm> findStockTermsByCategory(StockTermCategory category) {
        return stockTermRepository.findByCategoryOrderByIdAsc(category);
    }

    private StockTerm findStockTermById(Long termId) {
        return stockTermRepository.findById(termId)
                .orElseThrow(() -> new CustomException(StockTermErrorCode.STOCK_TERM_NOT_FOUND));
    }

    private TodayStockTerm findTodayStockTermByTargetDate(LocalDate targetDate) {
        List<TodayStockTerm> result = entityManager.createQuery(
                        "select todayStockTerm from TodayStockTerm todayStockTerm " +
                                "join fetch todayStockTerm.stockTerm " +
                                "where todayStockTerm.targetDate = :targetDate",
                        TodayStockTerm.class
                )
                .setParameter("targetDate", targetDate)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    private List<Long> extractTermIds(List<StockTerm> stockTerms) {
        return stockTerms.stream()
                .map(StockTerm::getId)
                .toList();
    }

    private Set<Long> findScrappedTermIds(Long userId, List<Long> termIds) {
        if (userId == null) {
            return Set.of();
        }
        return stockTermScrapRepository.findByUserIdAndStockTermIdIn(userId, termIds).stream()
                .map(scrap -> scrap.getStockTerm().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> findLearnedTermIds(Long userId, List<Long> termIds) {
        if (userId == null) {
            return Set.of();
        }
        return stockTermLearningRepository.findByUserIdAndStockTermIdIn(userId, termIds).stream()
                .map(learning -> learning.getStockTerm().getId())
                .collect(Collectors.toSet());
    }

    private List<StockTermScrap> findScrapsByUserId(Long userId) {
        return stockTermScrapRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private LocalDate resolveToday() {
        return LocalDate.now(ZoneId.of(zoneIdProperty));
    }

    private StockTerm findRandomStockTerm() {
        return stockTermRepository.findRandomStockTerm()
                .orElseThrow(() -> new CustomException(StockTermErrorCode.STOCK_TERM_NOT_FOUND));
    }

    // ========== 비즈니스 메서드 ==========
    private TodayStockTerm getOrCreateTodayStockTerm(LocalDate targetDate) {
        TodayStockTerm todayStockTerm = findTodayStockTermByTargetDate(targetDate);
        if (todayStockTerm != null) {
            return todayStockTerm;
        }

        try {
            TodayStockTerm created = TodayStockTerm.create(findRandomStockTerm(), targetDate);
            entityManager.persist(created);
            entityManager.flush();
            return created;
        } catch (RuntimeException e) {
            TodayStockTerm existing = findTodayStockTermByTargetDate(targetDate);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

    private void trackLearningIfNeeded(Long userId, Long termId, StockTerm stockTerm) {
        if (userId == null) {
            return;
        }
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

    // ========== 응답 변환 메서드 ==========
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

    private StockTermDetailResponse toDetailResponse(StockTerm stockTerm, boolean isScrapped, boolean isLearned) {
        return new StockTermDetailResponse(
                stockTerm.getId(),
                stockTerm.getCategory().getCategoryId(),
                stockTerm.getCategory().getLabel(),
                stockTerm.getTermName(),
                stockTerm.getDescription(),
                isScrapped,
                isLearned
        );
    }

    private TodayStockTermResponse toTodayStockTermResponse(StockTerm stockTerm) {
        return new TodayStockTermResponse(
                stockTerm.getId(),
                stockTerm.getTermName(),
                stockTerm.getDescription()
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
