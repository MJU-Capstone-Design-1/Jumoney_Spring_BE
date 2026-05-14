package com.mju.Jumoney.global.client.kis.smoke;

import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultOutput;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionTitleOutput;
import com.mju.Jumoney.global.client.kis.smoke.dto.BatchJobRunResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.StockIndicatorBatchStatusResponse;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Local KIS Smoke", description = "local 프로필 전용 KIS API 호출 검증")
@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/local/kis")
public class KisSmokeController {

    private final KisSmokeService kisSmokeService;

    @Value("${kis.hts.user-id:}")
    private String configuredHtsUserId;

    @Operation(
            summary = "KIS API 호출 검증",
            description = "local 프로필에서만 활성화됩니다. 입력 종목 코드로 현재까지 연동된 KIS REST API를 순차 호출하고 각 API의 성공 여부와 샘플 응답을 반환합니다."
    )
    @GetMapping("/smoke")
    public ResponseEntity<ApiResponse<KisSmokeResponse>> smoke(
            @Parameter(description = "종목 코드", example = "005930")
            @RequestParam(defaultValue = "005930") String stockCode,

            @Parameter(description = "신용잔고/투자자매매동향 조회 기준일. 예시는 문서 생성일 기준 전날로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,

            @Parameter(description = "배당일정 조회 시작일. 예시는 문서 생성일 기준 1년 전으로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dividendFrom,

            @Parameter(description = "배당일정 조회 종료일. 예시는 문서 생성일 기준 오늘로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dividendTo
    ) {
        LocalDate resolvedBaseDate = baseDate == null ? LocalDate.now().minusDays(1) : baseDate;
        LocalDate resolvedDividendTo = dividendTo == null ? LocalDate.now() : dividendTo;
        LocalDate resolvedDividendFrom = dividendFrom == null ? resolvedDividendTo.minusYears(1) : dividendFrom;

        KisSmokeResponse response = kisSmokeService.smoke(
                stockCode,
                resolvedBaseDate,
                resolvedDividendFrom,
                resolvedDividendTo
        );

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "HTS 조건검색 목록조회 검증",
            description = "local 프로필에서만 활성화됩니다. HTS에 서버저장된 조건명과 seq 목록을 조회합니다."
    )
    @GetMapping("/hts/titles")
    public ResponseEntity<ApiResponse<List<KisHtsConditionTitleOutput>>> htsConditionTitles(
            @Parameter(description = "HTS ID. 생략 시 kis.hts.user-id 설정값 사용")
            @RequestParam(required = false) String userId
    ) {
        String resolvedUserId = resolveHtsUserId(userId);
        List<KisHtsConditionTitleOutput> response = kisSmokeService.getHtsConditionTitles(resolvedUserId);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "HTS 조건검색 결과조회 검증",
            description = "local 프로필에서만 활성화됩니다. HTS 조건 seq로 종목검색 결과를 조회합니다. DB에는 저장하지 않습니다."
    )
    @GetMapping("/hts/results")
    public ResponseEntity<ApiResponse<List<KisHtsConditionResultOutput>>> htsConditionResults(
            @Parameter(description = "조건 seq", example = "0")
            @RequestParam String seq,

            @Parameter(description = "HTS ID. 생략 시 kis.hts.user-id 설정값 사용")
            @RequestParam(required = false) String userId
    ) {
        String resolvedUserId = resolveHtsUserId(userId);
        List<KisHtsConditionResultOutput> response = kisSmokeService.getHtsConditionResults(resolvedUserId, seq);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "HTS 조건검색 배치 수동 실행",
            description = "local 프로필에서만 활성화됩니다. 설정된 4개 HTS 조건검색 결과를 KIS에서 조회해 hts_stocks 테이블에 저장합니다. "
                    + "수동 실행은 요청한 baseDate를 그대로 사용하며, 생략 시 오늘 날짜를 사용합니다. 정기 스케줄은 직전 평일 기준으로 실행됩니다."
    )
    @PostMapping("/batch/hts-conditions")
    public ResponseEntity<ApiResponse<BatchJobRunResponse>> runHtsConditionBatch(
            @Parameter(description = "저장 기준일. 생략 시 오늘 날짜. 예시는 문서 생성일 기준 전날로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        BatchJobRunResponse response = kisSmokeService.runHtsConditionBatch(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "종목 지표 배치 수동 실행",
            description = "local 프로필에서만 활성화됩니다. Stock 테이블 전체 종목을 순회하며 KIS 지표를 조회해 stock_indicators 테이블에 upsert합니다. "
                    + "수동 실행은 요청한 baseDate를 그대로 사용하며, 정기 스케줄처럼 직전 개장일 기준 배치는 다음 장 시작 전에 실행해야 합니다. "
                    + "단, 오늘 기준 실행은 KIS 투자자매매동향 일별 API 제한 때문에 15:40 이후에만 가능합니다. "
                    + "정기 스케줄은 직전 평일 기준으로 실행됩니다."
    )
    @PostMapping("/batch/stock-indicators")
    public ResponseEntity<ApiResponse<BatchJobRunResponse>> runStockIndicatorBatch(
            @Parameter(description = "지표 기준일. 직전 개장일 기준 배치는 다음 장 시작 전에 실행해야 합니다. 예시는 문서 생성일 기준 전날로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        BatchJobRunResponse response = kisSmokeService.runStockIndicatorBatch(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "종목 지표 배치 적재 상태 확인",
            description = "local 프로필에서만 활성화됩니다. 기준일이 속한 기준월(baseTime=yyyyMM)의 stock_indicators 적재 건수, "
                    + "누락 종목, 필수 컬럼 null 건수를 조회합니다. baseDate 생략 시 오늘 날짜 기준월을 사용합니다."
    )
    @GetMapping("/batch/stock-indicators/status")
    public ResponseEntity<ApiResponse<StockIndicatorBatchStatusResponse>> getStockIndicatorBatchStatus(
            @Parameter(description = "확인 기준일. 생략 시 오늘 날짜. 예시는 문서 생성일 기준 전날로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        StockIndicatorBatchStatusResponse response = kisSmokeService.getStockIndicatorBatchStatus(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    private String resolveHtsUserId(String userId) {
        if (StringUtils.hasText(userId)) {
            return userId;
        }
        if (StringUtils.hasText(configuredHtsUserId)) {
            return configuredHtsUserId;
        }
        throw new IllegalStateException("HTS ID가 필요합니다. userId 파라미터 또는 kis.hts.user-id를 설정하세요.");
    }

    private LocalDate resolveBaseDate(LocalDate baseDate) {
        return baseDate == null ? LocalDate.now() : baseDate;
    }

}
