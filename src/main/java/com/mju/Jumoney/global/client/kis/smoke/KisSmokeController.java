package com.mju.Jumoney.global.client.kis.smoke;

import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceBacktestDataStatusResponse;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceBacktestDataSyncResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentChartCandleSyncResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentChartCandleSyncStatusResponse;
import com.mju.Jumoney.domain.mockinvestment.enums.MockInvestmentChartPeriod;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncResponse;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultOutput;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionTitleOutput;
import com.mju.Jumoney.global.client.kis.smoke.dto.BatchJobRunResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeResponse;
import com.mju.Jumoney.global.client.kis.smoke.dto.StockIndicatorBatchStatusResponse;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import com.mju.Jumoney.global.smoke.SmokeAdminKeyValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Tag(name = "KIS Smoke", description = "KIS API 호출 검증 및 배치 수동 실행")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/smoke/kis")
public class KisSmokeController {

    private final KisSmokeService kisSmokeService;
    private final SmokeAdminKeyValidator smokeAdminKeyValidator;

    @Value("${kis.hts.user-id:}")
    private String configuredHtsUserId;

    @Operation(
            summary = "KIS API 호출 검증",
            description = "입력 종목 코드로 현재까지 연동된 KIS REST API를 순차 호출하고 각 API의 성공 여부와 샘플 응답을 반환합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @GetMapping("/smoke")
    public ResponseEntity<ApiResponse<KisSmokeResponse>> smoke(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

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
        validateAdminKey(adminKey);

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
            description = "HTS에 서버저장된 조건명과 seq 목록을 조회합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @GetMapping("/hts/titles")
    public ResponseEntity<ApiResponse<List<KisHtsConditionTitleOutput>>> htsConditionTitles(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "HTS ID. 생략 시 kis.hts.user-id 설정값 사용")
            @RequestParam(required = false) String userId
    ) {
        validateAdminKey(adminKey);
        String resolvedUserId = resolveHtsUserId(userId);
        List<KisHtsConditionTitleOutput> response = kisSmokeService.getHtsConditionTitles(resolvedUserId);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "HTS 조건검색 결과조회 검증",
            description = "HTS 조건 seq로 종목검색 결과를 조회합니다. DB에는 저장하지 않습니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @GetMapping("/hts/results")
    public ResponseEntity<ApiResponse<List<KisHtsConditionResultOutput>>> htsConditionResults(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "조건 seq", example = "0")
            @RequestParam String seq,

            @Parameter(description = "HTS ID. 생략 시 kis.hts.user-id 설정값 사용")
            @RequestParam(required = false) String userId
    ) {
        validateAdminKey(adminKey);
        String resolvedUserId = resolveHtsUserId(userId);
        List<KisHtsConditionResultOutput> response = kisSmokeService.getHtsConditionResults(resolvedUserId, seq);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "HTS 조건검색 배치 수동 실행",
            description = "설정된 4개 HTS 조건검색 결과를 KIS에서 조회해 hts_stocks 테이블에 저장합니다. 운영 환경에서는 adminKey가 필요합니다. "
                    + "수동 실행은 요청한 baseDate를 그대로 사용하며, 생략 시 오늘 날짜를 사용합니다. 정기 스케줄은 직전 평일 기준으로 실행됩니다."
    )
    @PostMapping("/batch/hts-conditions")
    public ResponseEntity<ApiResponse<BatchJobRunResponse>> runHtsConditionBatch(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "저장 기준일. 생략 시 오늘 날짜. 예시는 문서 생성일 기준 전날로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        validateAdminKey(adminKey);
        BatchJobRunResponse response = kisSmokeService.runHtsConditionBatch(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "종목 지표 배치 수동 실행",
            description = "Stock 테이블 전체 종목을 순회하며 KIS 지표를 조회해 stock_indicators 테이블에 upsert합니다. 운영 환경에서는 adminKey가 필요합니다. "
                    + "수동 실행은 요청한 baseDate를 그대로 사용하며, 정기 스케줄처럼 직전 개장일 기준 배치는 다음 장 시작 전에 실행해야 합니다. "
                    + "단, 오늘 기준 실행은 KIS 투자자매매동향 일별 API 제한 때문에 15:40 이후에만 가능합니다. "
                    + "정기 스케줄은 직전 평일 기준으로 실행됩니다."
    )
    @PostMapping("/batch/stock-indicators")
    public ResponseEntity<ApiResponse<BatchJobRunResponse>> runStockIndicatorBatch(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "지표 기준일. 직전 개장일 기준 배치는 다음 장 시작 전에 실행해야 합니다. 예시는 문서 생성일 기준 전날로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        validateAdminKey(adminKey);
        BatchJobRunResponse response = kisSmokeService.runStockIndicatorBatch(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "종목 지표 배치 적재 상태 확인",
            description = "기준일이 속한 기준월(baseTime=yyyyMM)의 stock_indicators 적재 건수, "
                    + "누락 종목, 필수 컬럼 null 건수를 조회합니다. baseDate 생략 시 오늘 날짜 기준월을 사용합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @GetMapping("/batch/stock-indicators/status")
    public ResponseEntity<ApiResponse<StockIndicatorBatchStatusResponse>> getStockIndicatorBatchStatus(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "확인 기준일. 생략 시 오늘 날짜. 예시는 문서 생성일 기준 전날로 표시됩니다.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        validateAdminKey(adminKey);
        StockIndicatorBatchStatusResponse response = kisSmokeService.getStockIndicatorBatchStatus(resolveBaseDate(baseDate));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "당일 분봉 수동 동기화",
            description = "KIS 주식당일분봉조회(FHKST03010200)를 30분 단위 입력 시각으로 여러 번 호출해 당일 1분봉을 stock_candles 테이블에 upsert합니다. "
                    + "stockCode를 생략하면 등록된 전체 종목을 대상으로 실행합니다. "
                    + "KIS 응답의 최근 분봉은 아직 확정되지 않았을 수 있어 요청 시각 기준 최근 2분은 저장하지 않습니다. "
                    + "15:20~15:29 장마감 동시호가 구간은 15:19 종가 기준 volume=0 분봉으로 보강하고, 15:30은 별도 장마감 단일가 체결 봉으로 저장합니다. "
                    + "운영 환경에서는 adminKey가 필요합니다."
    )
    @PostMapping("/chart/minute/sync")
    public ResponseEntity<ApiResponse<MinuteCandleSyncResponse>> syncTodayMinuteCandles(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "종목 코드. 생략 시 전체 종목 대상", example = "005930")
            @RequestParam(required = false) String stockCode
    ) {
        validateAdminKey(adminKey);
        MinuteCandleSyncResponse response = kisSmokeService.syncTodayMinuteCandles(stockCode);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "특정 영업일 분봉 수동 동기화",
            description = "KIS 주식일별분봉조회(FHKST03010230)를 이용해 특정 영업일의 1분봉을 stock_candles 테이블에 upsert합니다. "
                    + "stockCode를 생략하면 등록된 전체 종목을 대상으로 실행합니다. "
                    + "오늘 날짜를 넣으면 당일 분봉과 동일하게 최근 2분은 저장하지 않고, 과거 영업일을 넣으면 장 마감 15:30까지 전량 확정 분봉을 저장합니다. "
                    + "과거 영업일 응답에 다른 날짜 raw가 섞여도 요청한 tradingDate 분봉만 저장하며, 15:20~15:29는 15:19 종가 기준 volume=0 분봉으로 보강합니다. "
                    + "휴장일이나 주말은 허용하지 않습니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @PostMapping("/chart/minute/sync/trading-day")
    public ResponseEntity<ApiResponse<MinuteCandleSyncResponse>> syncMinuteCandlesByTradingDay(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "동기화할 영업일", example = "2026-05-22")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradingDate,

            @Parameter(description = "종목 코드. 생략 시 전체 종목 대상", example = "005930")
            @RequestParam(required = false) String stockCode
    ) {
        validateAdminKey(adminKey);
        MinuteCandleSyncResponse response = kisSmokeService.syncMinuteCandles(stockCode, tradingDate);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "차트 기간 기준 수동 동기화",
            description = "차트 period 기준으로 필요한 캔들을 동기화합니다. "
                    + "period 생략 시 ONE_DAY, ONE_WEEK, THREE_MONTHS, ONE_YEAR, FIVE_YEARS를 오늘 또는 직전 개장일 기준으로 모두 채웁니다. "
                    + "ONE_DAY/ONE_WEEK는 분봉 동기화와 30분봉 집계를 사용하고, THREE_MONTHS/ONE_YEAR는 DAY, FIVE_YEARS는 WEEK 기간봉을 사용합니다. "
                    + "stockCode를 생략하면 등록된 전체 종목을 대상으로 실행합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @PostMapping("/chart/sync")
    public ResponseEntity<ApiResponse<MockInvestmentChartCandleSyncResponse>> syncChartCandles(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "차트 기간. 생략 시 전체 기간 동기화", example = "ONE_DAY")
            @RequestParam(required = false) MockInvestmentChartPeriod period,

            @Parameter(description = "동기화 기준일. 생략 시 오늘이 개장일이면 오늘, 아니면 직전 개장일")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "종목 코드. 생략 시 전체 종목 대상", example = "005930")
            @RequestParam(required = false) String stockCode
    ) {
        validateAdminKey(adminKey);
        MockInvestmentChartCandleSyncResponse response = kisSmokeService.syncChartCandles(stockCode, period, date);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "차트 특정 기간 수동 동기화",
            description = "지정한 기간만 차트 원천 캔들로 보정합니다. "
                    + "ONE_DAY/ONE_WEEK는 해당 기간의 영업일 분봉을 동기화하고 30분봉을 재집계합니다. "
                    + "THREE_MONTHS/ONE_YEAR는 DAY 기간봉, FIVE_YEARS는 WEEK 기간봉을 지정 기간만 upsert합니다. "
                    + "stockCode를 생략하면 등록된 전체 종목을 대상으로 실행합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @PostMapping("/chart/sync/range")
    public ResponseEntity<ApiResponse<MockInvestmentChartCandleSyncResponse>> syncChartCandlesInRange(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "차트 기간", example = "ONE_WEEK")
            @RequestParam MockInvestmentChartPeriod period,

            @Parameter(description = "동기화 시작일", example = "2026-05-18")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "동기화 종료일", example = "2026-05-22")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @Parameter(description = "종목 코드. 생략 시 전체 종목 대상", example = "005930")
            @RequestParam(required = false) String stockCode
    ) {
        validateAdminKey(adminKey);
        MockInvestmentChartCandleSyncResponse response = kisSmokeService.syncChartCandlesInRange(stockCode, period, fromDate, toDate);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "차트 기간 기준 동기화 상태 확인",
            description = "stock_candles 테이블에 저장된 특정 종목의 차트 period별 원천 캔들 범위와 건수를 확인합니다. "
                    + "period를 생략하면 ONE_DAY, ONE_WEEK, THREE_MONTHS, ONE_YEAR, FIVE_YEARS 상태를 모두 반환합니다. "
                    + "부족하면 /api/smoke/kis/chart/sync로 보정합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @GetMapping("/chart/sync/status")
    public ResponseEntity<ApiResponse<MockInvestmentChartCandleSyncStatusResponse>> getChartCandleSyncStatus(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "종목 코드", example = "005930")
            @RequestParam String stockCode,

            @Parameter(description = "차트 기간. 생략 시 전체 기간 상태 확인", example = "ONE_DAY")
            @RequestParam(required = false) MockInvestmentChartPeriod period,

            @Parameter(description = "상태 확인 기준일. 생략 시 오늘이 개장일이면 오늘, 아니면 직전 개장일")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        validateAdminKey(adminKey);
        MockInvestmentChartCandleSyncStatusResponse response = kisSmokeService.getChartCandleSyncStatus(stockCode, period, date);

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "거장의 선택 백테스트 재무 데이터 수동 동기화",
            description = "KIS 연간 재무비율/손익계산서를 조회해 master_choice_backtest_financials 테이블에 upsert합니다. "
                    + "stockCodes를 생략하면 등록된 전체 종목을 대상으로 실행합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @PostMapping("/master-choice/backtest/financials/sync")
    public ResponseEntity<ApiResponse<MasterChoiceBacktestDataSyncResponse>> syncMasterChoiceBacktestFinancials(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "쉼표로 구분한 종목 코드 목록. 생략 시 전체 종목 대상", example = "005930,000660")
            @RequestParam(required = false) String stockCodes
    ) {
        validateAdminKey(adminKey);
        MasterChoiceBacktestDataSyncResponse response =
                kisSmokeService.syncMasterChoiceBacktestFinancials(parseStockCodes(stockCodes));

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "거장의 선택 백테스트 일별 보조지표 수동 동기화",
            description = "KIS 신용잔고 일별추이와 종목별 투자자매매동향 일별 API를 조회해 "
                    + "master_choice_backtest_daily_indicators 테이블에 upsert합니다. "
                    + "stockCodes를 생략하면 등록된 전체 종목을 대상으로 실행합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @PostMapping("/master-choice/backtest/daily-indicators/sync")
    public ResponseEntity<ApiResponse<MasterChoiceBacktestDataSyncResponse>> syncMasterChoiceBacktestDailyIndicators(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "쉼표로 구분한 종목 코드 목록. 생략 시 전체 종목 대상", example = "005930,000660")
            @RequestParam(required = false) String stockCodes,

            @Parameter(description = "동기화 시작일. 생략 시 종료일 기준 1년 전")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "동기화 종료일. 생략 시 오늘")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        validateAdminKey(adminKey);
        LocalDate resolvedToDate = toDate == null ? LocalDate.now() : toDate;
        LocalDate resolvedFromDate = fromDate == null ? resolvedToDate.minusYears(1) : fromDate;
        MasterChoiceBacktestDataSyncResponse response = kisSmokeService.syncMasterChoiceBacktestDailyIndicators(
                parseStockCodes(stockCodes),
                resolvedFromDate,
                resolvedToDate
        );

        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "거장의 선택 백테스트 데이터 적재 상태 확인",
            description = "특정 종목의 백테스트 재무/일별 보조지표 적재 건수와 최신 기준일을 확인합니다. 운영 환경에서는 adminKey가 필요합니다."
    )
    @GetMapping("/master-choice/backtest/status")
    public ResponseEntity<ApiResponse<MasterChoiceBacktestDataStatusResponse>> getMasterChoiceBacktestDataStatus(
            @Parameter(description = "운영 환경 전용 관리자 키")
            @RequestParam(required = false) String adminKey,

            @Parameter(description = "종목 코드", example = "005930")
            @RequestParam String stockCode
    ) {
        validateAdminKey(adminKey);
        MasterChoiceBacktestDataStatusResponse response = kisSmokeService.getMasterChoiceBacktestDataStatus(stockCode);

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

    private void validateAdminKey(String adminKey) {
        smokeAdminKeyValidator.validate(adminKey);
    }

    private List<String> parseStockCodes(String stockCodes) {
        if (!StringUtils.hasText(stockCodes)) {
            return List.of();
        }
        return Arrays.stream(stockCodes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

}
