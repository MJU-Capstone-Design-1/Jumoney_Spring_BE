package com.mju.Jumoney.global.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class StockDataBatchScheduler {

    private final JobOperator jobOperator;
    private final BatchBaseDateResolver batchBaseDateResolver;
    private final Job stockIndicatorBatchJob;
    private final Job htsConditionBatchJob;
    private final boolean stockIndicatorEnabled;
    private final boolean htsConditionEnabled;

    public StockDataBatchScheduler(
            JobOperator jobOperator,
            BatchBaseDateResolver batchBaseDateResolver,
            @Qualifier(StockDataBatchJobConfig.STOCK_INDICATOR_JOB_NAME) Job stockIndicatorBatchJob,
            @Qualifier(StockDataBatchJobConfig.HTS_CONDITION_JOB_NAME) Job htsConditionBatchJob,
            @Value("${kis.batch.stock-indicator.enabled:true}") boolean stockIndicatorEnabled,
            @Value("${kis.batch.hts-condition.enabled:true}") boolean htsConditionEnabled
    ) {
        this.jobOperator = jobOperator;
        this.batchBaseDateResolver = batchBaseDateResolver;
        this.stockIndicatorBatchJob = stockIndicatorBatchJob;
        this.htsConditionBatchJob = htsConditionBatchJob;
        this.stockIndicatorEnabled = stockIndicatorEnabled;
        this.htsConditionEnabled = htsConditionEnabled;
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
