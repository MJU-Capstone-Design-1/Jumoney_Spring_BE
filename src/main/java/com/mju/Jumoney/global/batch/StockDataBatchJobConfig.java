package com.mju.Jumoney.global.batch;

import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import com.mju.Jumoney.domain.stock.service.HtsConditionBatchService;
import com.mju.Jumoney.domain.stock.service.StockIndicatorBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class StockDataBatchJobConfig {

    public static final String JOB_PARAM_BASE_DATE = "baseDate";
    public static final String STOCK_INDICATOR_JOB_NAME = "stockIndicatorBatchJob";
    public static final String HTS_CONDITION_JOB_NAME = "htsConditionBatchJob";
    public static final String RESULT_BASE_TIME = "baseTime";
    public static final String RESULT_TOTAL_COUNT = "totalCount";
    public static final String RESULT_SUCCESS_COUNT = "successCount";
    public static final String RESULT_FAILURE_COUNT = "failureCount";
    public static final String RESULT_TOTAL_SAVED_COUNT = "totalSavedCount";
    public static final String RESULT_HTS_SAVED_COUNT_PREFIX = "htsSavedCount.";

    private final StockIndicatorBatchService stockIndicatorBatchService;
    private final HtsConditionBatchService htsConditionBatchService;

    @Bean
    public Job stockIndicatorBatchJob(JobRepository jobRepository, Step stockIndicatorBatchStep) {
        return new JobBuilder(STOCK_INDICATOR_JOB_NAME, jobRepository)
                .start(stockIndicatorBatchStep)
                .build();
    }

    @Bean
    public Step stockIndicatorBatchStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("stockIndicatorBatchStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate baseDate = resolveBaseDate(chunkContext.getStepContext().getJobParameters().get(JOB_PARAM_BASE_DATE));
                    StockIndicatorBatchService.StockIndicatorBatchResult result = stockIndicatorBatchService.syncAll(baseDate);
                    JobExecution jobExecution = jobExecution(chunkContext);
                    jobExecution.getExecutionContext().putString(RESULT_BASE_TIME, result.baseTime());
                    jobExecution.getExecutionContext().putInt(RESULT_TOTAL_COUNT, result.totalCount());
                    jobExecution.getExecutionContext().putInt(RESULT_SUCCESS_COUNT, result.successCount());
                    jobExecution.getExecutionContext().putInt(RESULT_FAILURE_COUNT, result.failureCount());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job htsConditionBatchJob(JobRepository jobRepository, Step htsConditionBatchStep) {
        return new JobBuilder(HTS_CONDITION_JOB_NAME, jobRepository)
                .start(htsConditionBatchStep)
                .build();
    }

    @Bean
    public Step htsConditionBatchStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("htsConditionBatchStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate baseDate = resolveBaseDate(chunkContext.getStepContext().getJobParameters().get(JOB_PARAM_BASE_DATE));
                    Map<HtsSearchType, Integer> savedCounts = htsConditionBatchService.syncAll(baseDate);
                    JobExecution jobExecution = jobExecution(chunkContext);
                    int totalSavedCount = savedCounts.values().stream()
                            .mapToInt(Integer::intValue)
                            .sum();
                    jobExecution.getExecutionContext().putInt(RESULT_TOTAL_SAVED_COUNT, totalSavedCount);
                    savedCounts.forEach((searchType, savedCount) ->
                            jobExecution.getExecutionContext().putInt(RESULT_HTS_SAVED_COUNT_PREFIX + searchType.name(), savedCount)
                    );
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private LocalDate resolveBaseDate(Object baseDate) {
        if (baseDate == null) {
            return LocalDate.now();
        }
        return LocalDate.parse(baseDate.toString());
    }

    private JobExecution jobExecution(ChunkContext chunkContext) {
        return chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution();
    }
}
