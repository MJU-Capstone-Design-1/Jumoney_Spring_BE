package com.mju.Jumoney.global.client.kis.smoke;

import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeResponse;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Local KIS Smoke", description = "local 프로필 전용 KIS API 호출 검증")
@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/local/kis")
public class KisSmokeController {

    private final KisSmokeService kisSmokeService;

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
}
