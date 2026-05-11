package com.mju.Jumoney.global.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockIndicatorBatchScheduler {

    private final JobOperator jobOperator;
    private final BatchBaseDateResolver batchBaseDateResolver;

    @Qualifier(StockDataBatchJobConfig.STOCK_INDICATOR_JOB_NAME)
    private final Job stockIndicatorBatchJob;

    @Scheduled(
            cron = "${kis.batch.stock-indicator.cron:0 0 6 * * TUE-SAT}",
            zone = "${kis.batch.zone-id:Asia/Seoul}"
    )
    public void runStockIndicatorBatch() throws Exception {
        LocalDate baseDate = batchBaseDateResolver.resolveScheduledBaseDate();
        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate(StockDataBatchJobConfig.JOB_PARAM_BASE_DATE, baseDate, true)
                .toJobParameters();

        log.info("[StockIndicatorBatchScheduler] 종목 지표 배치 시작: baseDate={}", baseDate);
        jobOperator.start(stockIndicatorBatchJob, jobParameters);
    }
}
