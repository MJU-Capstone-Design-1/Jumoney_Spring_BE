package com.mju.Jumoney.global.client.kis.smoke;

import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultOutput;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionTitleOutput;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stock.service.HtsConditionBatchService;
import com.mju.Jumoney.domain.stock.service.StockIndicatorBatchService;
import com.mju.Jumoney.global.client.kis.smoke.dto.HtsConditionBatchRunResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeApiResult;
import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.MissingStockIndicatorResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.StockIndicatorBatchRunResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.StockIndicatorBatchStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Profile("local")
@RequiredArgsConstructor
public class KisSmokeService {

    private static final DateTimeFormatter BASE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String TR_ID_CURRENT_PRICE = "FHKST01010100";
    private static final String TR_ID_EXECUTION_STRENGTH = "FHKST01010300";
    private static final String TR_ID_FINANCIAL_RATIO = "FHKST66430300";
    private static final String TR_ID_INCOME_STATEMENT = "FHKST66430200";
    private static final String TR_ID_DIVIDEND = "HHKDB669102C0";
    private static final String TR_ID_CREDIT_BALANCE = "FHPST04760000";
    private static final String TR_ID_INVESTOR_TRADE_DAILY = "FHPTJ04160001";

    private final KisApiClient kisApiClient;
    private final HtsConditionBatchService htsConditionBatchService;
    private final StockIndicatorBatchService stockIndicatorBatchService;
    private final StockRepository stockRepository;
    private final StockIndicatorRepository stockIndicatorRepository;

    public KisSmokeResponse smoke(String stockCode, LocalDate baseDate, LocalDate dividendFrom, LocalDate dividendTo) {
        List<KisSmokeApiResult> results = new ArrayList<>();

        results.add(callSingle(1, "주식현재가 시세", TR_ID_CURRENT_PRICE,
                () -> kisApiClient.getCurrentPrice(stockCode)));
        results.add(callSingle(2, "주식현재가 체결", TR_ID_EXECUTION_STRENGTH,
                () -> kisApiClient.getExecutionStrength(stockCode)));
        results.add(callList(3, "국내주식 재무비율", TR_ID_FINANCIAL_RATIO,
                () -> kisApiClient.getFinancialRatios(stockCode)));
        results.add(callList(4, "국내주식 손익계산서", TR_ID_INCOME_STATEMENT,
                () -> kisApiClient.getIncomeStatements(stockCode)));
        results.add(callList(5, "예탁원정보 배당일정", TR_ID_DIVIDEND,
                () -> kisApiClient.getDividends(stockCode, dividendFrom, dividendTo)));
        results.add(callList(6, "국내주식 신용잔고 일별추이", TR_ID_CREDIT_BALANCE,
                () -> kisApiClient.getDailyCreditBalances(stockCode, baseDate)));
        results.add(callList(7, "종목별 투자자매매동향 일별", TR_ID_INVESTOR_TRADE_DAILY,
                () -> kisApiClient.getInvestorTradesDaily(stockCode, baseDate)));

        int successCount = (int) results.stream()
                .filter(KisSmokeApiResult::success)
                .count();

        return new KisSmokeResponse(
                stockCode,
                baseDate,
                dividendFrom,
                dividendTo,
                results.size(),
                successCount,
                results.size() - successCount,
                results
        );
    }

    private KisSmokeApiResult callSingle(int step, String name, String trId, Supplier<?> supplier) {
        try {
            Object sample = supplier.get();
            return KisSmokeApiResult.success(step, name, trId, sample == null ? 0 : 1, sample);
        } catch (Exception e) {
            return KisSmokeApiResult.failure(step, name, trId, e.getMessage());
        }
    }

    private <T> KisSmokeApiResult callList(int step, String name, String trId, Supplier<List<T>> supplier) {
        try {
            List<T> items = supplier.get();
            Object sample = items.isEmpty() ? null : items.get(0);
            return KisSmokeApiResult.success(step, name, trId, items.size(), sample);
        } catch (Exception e) {
            return KisSmokeApiResult.failure(step, name, trId, e.getMessage());
        }
    }

    public List<KisHtsConditionTitleOutput> getHtsConditionTitles(String htsUserId) {
        return kisApiClient.getHtsConditionTitles(htsUserId);
    }

    public List<KisHtsConditionResultOutput> getHtsConditionResults(String htsUserId, String seq) {
        return kisApiClient.getHtsConditionResults(htsUserId, seq);
    }

    public HtsConditionBatchRunResponse runHtsConditionBatch(LocalDate baseDate) {
        Map<HtsSearchType, Integer> savedCounts = htsConditionBatchService.syncAll(baseDate);
        return HtsConditionBatchRunResponse.of(baseDate, savedCounts);
    }

    public StockIndicatorBatchRunResponse runStockIndicatorBatch(LocalDate baseDate) {
        StockIndicatorBatchService.StockIndicatorBatchResult result = stockIndicatorBatchService.syncAll(baseDate);
        return StockIndicatorBatchRunResponse.from(result);
    }

    public StockIndicatorBatchStatusResponse getStockIndicatorBatchStatus(LocalDate baseDate) {
        String baseTime = toBaseTime(baseDate);
        long stockCount = stockRepository.count();
        long indicatorCount = stockIndicatorRepository.countByBaseTime(baseTime);
        long invalidRequiredFieldCount = stockIndicatorRepository.countInvalidRequiredFieldsByBaseTime(baseTime);
        List<MissingStockIndicatorResponse> missingStocks = stockIndicatorRepository.findStocksWithoutIndicator(baseTime).stream()
                .map(MissingStockIndicatorResponse::from)
                .toList();

        return new StockIndicatorBatchStatusResponse(
                baseDate,
                baseTime,
                stockCount,
                indicatorCount,
                missingStocks.size(),
                invalidRequiredFieldCount,
                stockCount == indicatorCount && missingStocks.isEmpty() && invalidRequiredFieldCount == 0,
                missingStocks
        );
    }

    private String toBaseTime(LocalDate baseDate) {
        return YearMonth.from(baseDate).format(BASE_TIME_FORMATTER);
    }
}
