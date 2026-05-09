package com.mju.Jumoney.global.client.kis.smoke;

import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultOutput;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionTitleOutput;
import com.mju.Jumoney.global.client.kis.smoke.dto.HtsConditionBatchRunResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.StockIndicatorBatchRunResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

            @Parameter(description = "신용잔고/투자자매매동향 조회 기준일", example = "2026-05-04")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,

            @Parameter(description = "배당일정 조회 시작일", example = "2025-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dividendFrom,

            @Parameter(description = "배당일정 조회 종료일", example = "2026-05-06")
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
            description = "local 프로필에서만 활성화됩니다. 설정된 4개 HTS 조건검색 결과를 KIS에서 조회해 hts_stocks 테이블에 저장합니다."
    )
    @PostMapping("/batch/hts-conditions")
    public ResponseEntity<ApiResponse<HtsConditionBatchRunResponse>> runHtsConditionBatch(
            @Parameter(description = "저장 기준일. 생략 시 오늘 날짜", example = "2026-05-07")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        HtsConditionBatchRunResponse response = kisSmokeService.runHtsConditionBatch(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "종목 지표 배치 수동 실행",
            description = "local 프로필에서만 활성화됩니다. Stock 테이블 전체 종목을 순회하며 KIS 지표를 조회해 stock_indicators 테이블에 upsert합니다."
    )
    @PostMapping("/batch/stock-indicators")
    public ResponseEntity<ApiResponse<StockIndicatorBatchRunResponse>> runStockIndicatorBatch(
            @Parameter(description = "지표 기준일. 생략 시 오늘 날짜", example = "2026-05-07")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        StockIndicatorBatchRunResponse response = kisSmokeService.runStockIndicatorBatch(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "종목 지표 배치 적재 상태 확인",
            description = "local 프로필에서만 활성화됩니다. 기준월 stock_indicators 적재 건수, 누락 종목, 필수 컬럼 null 건수를 조회합니다."
    )
    @GetMapping("/batch/stock-indicators/status")
    public ResponseEntity<ApiResponse<StockIndicatorBatchStatusResponse>> getStockIndicatorBatchStatus(
            @Parameter(description = "확인 기준일. 생략 시 오늘 날짜", example = "2026-05-07")
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
