package com.mju.Jumoney.global.client.kis.smoke;

import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncResponse;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncStatusResponse;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stock.service.StockMinuteCandleSyncService;
import com.mju.Jumoney.global.batch.BatchBaseDateResolver;
import com.mju.Jumoney.global.batch.StockDataBatchJobConfig;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultOutput;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionTitleOutput;
import com.mju.Jumoney.global.client.kis.smoke.dto.*;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
@Profile({"local", "prod"})
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
    private final StockRepository stockRepository;
    private final StockIndicatorRepository stockIndicatorRepository;
    private final JobOperator jobOperator;
    private final Job stockIndicatorBatchJob;
    private final Job htsConditionBatchJob;
    private final BatchBaseDateResolver batchBaseDateResolver;
    private final StockMinuteCandleSyncService stockMinuteCandleSyncService;

    public KisSmokeService(KisApiClient kisApiClient,
                           StockRepository stockRepository,
                           StockIndicatorRepository stockIndicatorRepository,
                           JobOperator jobOperator,
                           @Qualifier(StockDataBatchJobConfig.STOCK_INDICATOR_JOB_NAME) Job stockIndicatorBatchJob,
                           @Qualifier(StockDataBatchJobConfig.HTS_CONDITION_JOB_NAME) Job htsConditionBatchJob,
                           BatchBaseDateResolver batchBaseDateResolver,
                           StockMinuteCandleSyncService stockMinuteCandleSyncService) {
        this.kisApiClient = kisApiClient;
        this.stockRepository = stockRepository;
        this.stockIndicatorRepository = stockIndicatorRepository;
        this.jobOperator = jobOperator;
        this.stockIndicatorBatchJob = stockIndicatorBatchJob;
        this.htsConditionBatchJob = htsConditionBatchJob;
        this.batchBaseDateResolver = batchBaseDateResolver;
        this.stockMinuteCandleSyncService = stockMinuteCandleSyncService;
    }

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

    public BatchJobRunResponse runHtsConditionBatch(LocalDate baseDate) {
        return runBatchJob(htsConditionBatchJob, StockDataBatchJobConfig.HTS_CONDITION_JOB_NAME, baseDate);
    }

    public BatchJobRunResponse runStockIndicatorBatch(LocalDate baseDate) {
        batchBaseDateResolver.validateStockIndicatorManualBaseDate(baseDate);
        return runBatchJob(stockIndicatorBatchJob, StockDataBatchJobConfig.STOCK_INDICATOR_JOB_NAME, baseDate);
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

    public MinuteCandleSyncResponse syncTodayMinuteCandles(String stockCode) {
        return stockMinuteCandleSyncService.syncTodayMinuteCandles(stockCode);
    }

    public MinuteCandleSyncResponse syncMinuteCandles(String stockCode, LocalDate tradingDate) {
        return stockMinuteCandleSyncService.syncMinuteCandles(stockCode, tradingDate);
    }

    public MinuteCandleSyncStatusResponse getTodayMinuteCandleSyncStatus(String stockCode, LocalDate date) {
        return stockMinuteCandleSyncService.getTodayMinuteCandleSyncStatus(stockCode, date);
    }

    private String toBaseTime(LocalDate baseDate) {
        return YearMonth.from(baseDate).format(BASE_TIME_FORMATTER);
    }

    private BatchJobRunResponse runBatchJob(Job job, String jobName, LocalDate baseDate) {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate(StockDataBatchJobConfig.JOB_PARAM_BASE_DATE, baseDate, true)
                .toJobParameters();

        try {
            JobExecution jobExecution = jobOperator.start(job, jobParameters);
            return BatchJobRunResponse.from(jobName, baseDate, jobExecution);
        } catch (Exception e) {
            throw new IllegalStateException("Spring Batch Job 실행 실패: jobName=" + jobName + ", baseDate=" + baseDate, e);
        }
    }
}
