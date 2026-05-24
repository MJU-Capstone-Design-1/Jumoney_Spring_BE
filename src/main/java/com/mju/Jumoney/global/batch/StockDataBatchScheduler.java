package com.mju.Jumoney.global.batch;

import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentChartCandleSyncResponse;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentChartSyncService;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncResponse;
import com.mju.Jumoney.domain.stock.service.StockMinuteCandleSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
public class StockDataBatchScheduler {

    private final JobOperator jobOperator;
    private final BatchBaseDateResolver batchBaseDateResolver;
    private final MarketCalendarService marketCalendarService;
    private final StockMinuteCandleSyncService stockMinuteCandleSyncService;
    private final MockInvestmentChartSyncService mockInvestmentChartSyncService;
    private final Job stockIndicatorBatchJob;
    private final Job htsConditionBatchJob;
    private final boolean stockIndicatorEnabled;
    private final boolean htsConditionEnabled;
    private final boolean minuteCandleEnabled;
    private final ZoneId zoneId;

    public StockDataBatchScheduler(
            JobOperator jobOperator,
            BatchBaseDateResolver batchBaseDateResolver,
            MarketCalendarService marketCalendarService,
            StockMinuteCandleSyncService stockMinuteCandleSyncService,
            MockInvestmentChartSyncService mockInvestmentChartSyncService,
            @Qualifier(StockDataBatchJobConfig.STOCK_INDICATOR_JOB_NAME) Job stockIndicatorBatchJob,
            @Qualifier(StockDataBatchJobConfig.HTS_CONDITION_JOB_NAME) Job htsConditionBatchJob,
            @Value("${kis.batch.stock-indicator.enabled:true}") boolean stockIndicatorEnabled,
            @Value("${kis.batch.hts-condition.enabled:true}") boolean htsConditionEnabled,
            @Value("${kis.batch.minute-candle.enabled:true}") boolean minuteCandleEnabled,
            @Value("${kis.batch.zone-id:Asia/Seoul}") String zoneId
    ) {
        this.jobOperator = jobOperator;
        this.batchBaseDateResolver = batchBaseDateResolver;
        this.marketCalendarService = marketCalendarService;
        this.stockMinuteCandleSyncService = stockMinuteCandleSyncService;
        this.mockInvestmentChartSyncService = mockInvestmentChartSyncService;
        this.stockIndicatorBatchJob = stockIndicatorBatchJob;
        this.htsConditionBatchJob = htsConditionBatchJob;
        this.stockIndicatorEnabled = stockIndicatorEnabled;
        this.htsConditionEnabled = htsConditionEnabled;
        this.minuteCandleEnabled = minuteCandleEnabled;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Scheduled(
            cron = "${kis.batch.stock-indicator.cron:0 0 6 * * TUE-SAT}",
            zone = "${kis.batch.zone-id:Asia/Seoul}"
    )
    public void runStockIndicatorBatch() {
        runBatchJob(
                stockIndicatorEnabled,
                stockIndicatorBatchJob,
                StockDataBatchJobConfig.STOCK_INDICATOR_JOB_NAME
        );
    }

    @Scheduled(
            cron = "${kis.batch.hts-condition.cron:0 30 6 * * TUE-SAT}",
            zone = "${kis.batch.zone-id:Asia/Seoul}"
    )
    public void runHtsConditionBatch() {
        runBatchJob(
                htsConditionEnabled,
                htsConditionBatchJob,
                StockDataBatchJobConfig.HTS_CONDITION_JOB_NAME
        );
    }

    @Scheduled(
            cron = "${kis.batch.minute-candle.cron:0 2,32 9-15 * * MON-FRI}",
            zone = "${kis.batch.zone-id:Asia/Seoul}"
    )
    public void runMinuteCandleSync() {
        runMinuteCandleSync("regular");
    }

    @Scheduled(
            cron = "${kis.batch.minute-candle.close-cron:0 40 15 * * MON-FRI}",
            zone = "${kis.batch.zone-id:Asia/Seoul}"
    )
    public void runMinuteCandleCloseSync() {
        runMinuteCandleSync("close");
    }

    private void runMinuteCandleSync(String scheduleType) {
        if (!minuteCandleEnabled) {
            log.info("[StockDataBatchScheduler] 분봉 동기화 스케줄 비활성화: scheduleType={}", scheduleType);
            return;
        }

        LocalDate today = LocalDate.now(zoneId);
        if (!marketCalendarService.isOpenDay(today, zoneId)) {
            log.info("[StockDataBatchScheduler] 휴장일 분봉 동기화 스킵: scheduleType={}, date={}", scheduleType, today);
            return;
        }

        log.info("[StockDataBatchScheduler] 분봉 동기화 스케줄 실행 시작: scheduleType={}, date={}", scheduleType, today);
        try {
            MinuteCandleSyncResponse response = stockMinuteCandleSyncService.syncTodayMinuteCandles(null);
            LocalDate finalDailyCandleDate = resolveFinalDailyCandleDate(today, scheduleType);
            var finalPeriodSyncs = mockInvestmentChartSyncService.syncLatestFinalPeriodCandles(null, finalDailyCandleDate);
            int finalPeriodKisRequestCount = finalPeriodSyncs.stream()
                    .mapToInt(MockInvestmentChartCandleSyncResponse.SourceSync::kisRequestCount)
                    .sum();
            int finalPeriodSavedCandleCount = finalPeriodSyncs.stream()
                    .mapToInt(MockInvestmentChartCandleSyncResponse.SourceSync::savedCandleCount)
                    .sum();
            log.info("[StockDataBatchScheduler] 분봉 동기화 스케줄 실행 완료: scheduleType={}, date={}, cutoff={}, targetStockCount={}, kisRequestCount={}, successCount={}, failureCount={}, savedCandleCount={}, skippedRecentCandleCount={}",
                    scheduleType,
                    today,
                    response.finalizationCutoffTime(),
                    response.targetStockCount(),
                    response.kisRequestCount(),
                    response.successCount(),
                    response.failureCount(),
                    response.savedCandleCount(),
                    response.skippedRecentCandleCount());
            log.info("[StockDataBatchScheduler] 최신 확정 일/주봉 동기화 완료: scheduleType={}, finalDailyCandleDate={}, sourceCount={}, kisRequestCount={}, savedCandleCount={}",
                    scheduleType,
                    finalDailyCandleDate,
                    finalPeriodSyncs.size(),
                    finalPeriodKisRequestCount,
                    finalPeriodSavedCandleCount);
        } catch (Exception e) {
            log.error("[StockDataBatchScheduler] 분봉 동기화 스케줄 실행 실패: scheduleType={}, date={}", scheduleType, today, e);
        }
    }

    private LocalDate resolveFinalDailyCandleDate(LocalDate today, String scheduleType) {
        if ("close".equals(scheduleType)) {
            return today;
        }
        return marketCalendarService.resolvePreviousOpenDay(today, 14, zoneId);
    }

    private void runBatchJob(boolean enabled, Job job, String jobName) {
        if (!enabled) {
            log.info("[StockDataBatchScheduler] 배치 스케줄 비활성화: jobName={}", jobName);
            return;
        }

        LocalDate baseDate = batchBaseDateResolver.resolveScheduledBaseDate();
        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate(StockDataBatchJobConfig.JOB_PARAM_BASE_DATE, baseDate, true)
                .toJobParameters();

        log.info("[StockDataBatchScheduler] 배치 스케줄 실행 시작: jobName={}, baseDate={}", jobName, baseDate);
        try {
            JobExecution jobExecution = jobOperator.start(job, jobParameters);
            log.info("[StockDataBatchScheduler] 배치 Job 시작 완료: jobName={}, baseDate={}, executionId={}, status={}",
                    jobName, baseDate, jobExecution.getId(), jobExecution.getStatus());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[StockDataBatchScheduler] 이미 완료된 배치라 스킵: jobName={}, baseDate={}", jobName, baseDate);
        } catch (JobExecutionAlreadyRunningException e) {
            log.info("[StockDataBatchScheduler] 이미 실행 중인 배치라 스킵: jobName={}, baseDate={}", jobName, baseDate);
        } catch (JobRestartException | InvalidJobParametersException e) {
            log.warn("[StockDataBatchScheduler] 배치 실행 불가: jobName={}, baseDate={}, error={}",
                    jobName, baseDate, e.getMessage());
        } catch (Exception e) {
            log.error("[StockDataBatchScheduler] 배치 스케줄 실행 실패: jobName={}, baseDate={}",
                    jobName, baseDate, e);
        }
    }
}
