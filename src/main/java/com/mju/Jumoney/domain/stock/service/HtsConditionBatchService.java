package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.domain.stock.domain.HtsStock;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import com.mju.Jumoney.domain.stock.repository.HtsStockRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HtsConditionBatchService {

    private final KisApiClient kisApiClient;
    private final StockRepository stockRepository;
    private final HtsStockRepository htsStockRepository;

    @Value("${kis.hts.user-id:}")
    private String htsUserId;

    @Value("${kis.hts.conditions.stability-seq:}")
    private String stabilitySeq;

    @Value("${kis.hts.conditions.safe-pursuit-seq:}")
    private String safePursuitSeq;

    @Value("${kis.hts.conditions.profit-pursuit-seq:}")
    private String profitPursuitSeq;

    @Value("${kis.hts.conditions.aggressive-seq:}")
    private String aggressiveSeq;

    @Transactional
    public Map<HtsSearchType, Integer> syncAll(LocalDate baseDate) {
        validateConfig();

        Map<HtsSearchType, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<HtsSearchType, String> entry : configuredSequences().entrySet()) {
            int savedCount = sync(entry.getKey(), entry.getValue(), baseDate);
            result.put(entry.getKey(), savedCount);
        }
        return result;
    }

    @Transactional
    public int sync(HtsSearchType searchType, String seq, LocalDate baseDate) {
        List<KisHtsConditionResultOutput> htsResults = kisApiClient.getHtsConditionResults(htsUserId, seq);
        List<String> stockCodesInKisResult = htsResults.stream()
                .map(KisHtsConditionResultOutput::stockCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, Stock> kospi200StocksByCode = stockRepository.findByStockCodeIn(stockCodesInKisResult).stream()
                .collect(Collectors.toMap(Stock::getStockCode, Function.identity()));

        List<HtsStock> htsStocks = stockCodesInKisResult.stream()
                .map(kospi200StocksByCode::get)
                .filter(stock -> stock != null)
                .map(stock -> HtsStock.create(stock, searchType, baseDate))
                .toList();

        htsStockRepository.deleteBySearchTypeAndBaseDate(searchType, baseDate);
        htsStockRepository.saveAll(htsStocks);

        int skippedCount = stockCodesInKisResult.size() - htsStocks.size();
        if (skippedCount > 0) {
            log.warn("[HTS Condition Batch] KOSPI 200 대상이 아닌 조건검색 종목 제외: searchType={}, receivedCount={}, savedCount={}, skippedCount={}",
                    searchType, stockCodesInKisResult.size(), htsStocks.size(), skippedCount);
        }

        log.info("[HTS Condition Batch] 조건검색 결과 저장 완료: searchType={}, baseDate={}, receivedCount={}, savedCount={}",
                searchType, baseDate, stockCodesInKisResult.size(), htsStocks.size());
        return htsStocks.size();
    }

    private Map<HtsSearchType, String> configuredSequences() {
        Map<HtsSearchType, String> sequences = new LinkedHashMap<>();
        sequences.put(HtsSearchType.STABILITY, stabilitySeq);
        sequences.put(HtsSearchType.SAFE_PURSUIT, safePursuitSeq);
        sequences.put(HtsSearchType.PROFIT_PURSUIT, profitPursuitSeq);
        sequences.put(HtsSearchType.AGGRESSIVE, aggressiveSeq);
        return sequences;
    }

    private void validateConfig() {
        if (!StringUtils.hasText(htsUserId)) {
            throw new IllegalStateException("kis.hts.user-id 설정이 필요합니다.");
        }
        configuredSequences().forEach((searchType, seq) -> {
            if (!StringUtils.hasText(seq)) {
                throw new IllegalStateException("HTS 조건검색 seq 설정이 필요합니다. searchType=" + searchType);
            }
        });
    }
}
