package com.mju.Jumoney.global.client.kis.smoke.dto;

import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import com.mju.Jumoney.global.batch.StockDataBatchJobConfig;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record BatchJobRunResponse(
        String jobName,
        long jobExecutionId,
        long jobInstanceId,
        LocalDate baseDate,
        String status,
        String exitCode,
        String exitDescription,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String baseTime,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        Integer totalSavedCount,
        Map<HtsSearchType, Integer> savedCounts
) {
    public static BatchJobRunResponse from(String jobName, LocalDate baseDate, JobExecution jobExecution) {
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        return new BatchJobRunResponse(
                jobName,
                jobExecution.getId(),
                jobExecution.getJobInstanceId(),
                baseDate,
                jobExecution.getStatus().name(),
                jobExecution.getExitStatus().getExitCode(),
                jobExecution.getExitStatus().getExitDescription(),
                jobExecution.getStartTime(),
                jobExecution.getEndTime(),
                optionalString(executionContext, StockDataBatchJobConfig.RESULT_BASE_TIME),
                optionalInt(executionContext, StockDataBatchJobConfig.RESULT_TOTAL_COUNT),
                optionalInt(executionContext, StockDataBatchJobConfig.RESULT_SUCCESS_COUNT),
                optionalInt(executionContext, StockDataBatchJobConfig.RESULT_FAILURE_COUNT),
                optionalInt(executionContext, StockDataBatchJobConfig.RESULT_TOTAL_SAVED_COUNT),
                htsSavedCounts(executionContext)
        );
    }

    private static String optionalString(ExecutionContext executionContext, String key) {
        if (!executionContext.containsKey(key)) {
            return null;
        }
        return executionContext.getString(key);
    }

    private static Integer optionalInt(ExecutionContext executionContext, String key) {
        if (!executionContext.containsKey(key)) {
            return null;
        }
        return executionContext.getInt(key);
    }

    private static Map<HtsSearchType, Integer> htsSavedCounts(ExecutionContext executionContext) {
        Map<HtsSearchType, Integer> savedCounts = new LinkedHashMap<>();
        for (HtsSearchType searchType : HtsSearchType.values()) {
            String key = StockDataBatchJobConfig.RESULT_HTS_SAVED_COUNT_PREFIX + searchType.name();
            if (executionContext.containsKey(key)) {
                savedCounts.put(searchType, executionContext.getInt(key));
            }
        }
        return savedCounts.isEmpty() ? null : savedCounts;
    }
}
