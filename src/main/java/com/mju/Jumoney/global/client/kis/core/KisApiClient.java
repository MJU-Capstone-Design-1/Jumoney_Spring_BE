package com.mju.Jumoney.global.client.kis.core;

import com.mju.Jumoney.global.client.kis.dto.common.KisApiResponse;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultOutput;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionResultResponse;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionTitleOutput;
import com.mju.Jumoney.global.client.kis.dto.condition.KisHtsConditionTitleResponse;
import com.mju.Jumoney.global.client.kis.dto.dividend.KisDividendMetrics;
import com.mju.Jumoney.global.client.kis.dto.dividend.KisDividendResponse;
import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioResponse;
import com.mju.Jumoney.global.client.kis.dto.finance.KisIncomeStatementMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisIncomeStatementResponse;
import com.mju.Jumoney.global.client.kis.dto.market.KisDomesticHolidayOutput;
import com.mju.Jumoney.global.client.kis.dto.market.KisDomesticHolidayResponse;
import com.mju.Jumoney.global.client.kis.dto.price.KisCurrentPriceMetrics;
import com.mju.Jumoney.global.client.kis.dto.price.KisCurrentPriceResponse;
import com.mju.Jumoney.global.client.kis.dto.price.KisExecutionStrengthMetrics;
import com.mju.Jumoney.global.client.kis.dto.price.KisExecutionStrengthResponse;
import com.mju.Jumoney.global.client.kis.dto.trading.KisCreditBalanceMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisCreditBalanceResponse;
import com.mju.Jumoney.global.client.kis.dto.trading.KisInvestorTradeDailyMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisInvestorTradeDailyResponse;
import com.mju.Jumoney.global.client.kis.enums.KisFinancialPeriod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

// KIS REST API 호출을 담당 (토큰 발급/캐싱은 KisTokenManager에 위임)
@Slf4j
@Component
public class KisApiClient {

    private static final String RESULT_SUCCESS = "0";
    private static final String CUSTOMER_TYPE_PERSONAL = "P";
    private static final String MARKET_DIV_CODE_KRX = "J";
    private static final String CREDIT_BALANCE_SCREEN_DIV_CODE = "20476";
    private static final String EMPTY_HTS_RESULT_CODE = "MCA05918";
    private static final String HTS_CONDITION_NOT_SAVED_CODE = "MCA05762";
    private static final DateTimeFormatter KIS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private static final String TR_ID_CURRENT_PRICE = "FHKST01010100";
    private static final String TR_ID_EXECUTION_STRENGTH = "FHKST01010300";
    private static final String TR_ID_FINANCIAL_RATIO = "FHKST66430300";
    private static final String TR_ID_INCOME_STATEMENT = "FHKST66430200";
    private static final String TR_ID_DIVIDEND = "HHKDB669102C0";
    private static final String TR_ID_CREDIT_BALANCE = "FHPST04760000";
    private static final String TR_ID_INVESTOR_TRADE_DAILY = "FHPTJ04160001";
    private static final String TR_ID_HTS_CONDITION_TITLE = "HHKST03900300";
    private static final String TR_ID_HTS_CONDITION_RESULT = "HHKST03900400";
    private static final String TR_ID_DOMESTIC_HOLIDAY = "CTCA0903R";

    private final WebClient webClient;
    private final KisTokenManager kisTokenManager;
    private final KisMetricMapper kisMetricMapper;
    private final KisRateLimiter kisRateLimiter;

    @Value("${kis.appkey}")
    private String appKey;

    @Value("${kis.appsecret}")
    private String appSecret;

    @Value("${kis.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${kis.retry.initial-delay-millis:700}")
    private long initialRetryDelayMillis;

    @Value("${kis.retry.multiplier:2.0}")
    private double retryDelayMultiplier;

    @Value("${kis.retry.max-delay-millis:3000}")
    private long maxRetryDelayMillis;

    public KisApiClient(@Qualifier("kisWebClient") WebClient webClient,
                        KisTokenManager kisTokenManager,
                        KisMetricMapper kisMetricMapper,
                        KisRateLimiter kisRateLimiter) {
        this.webClient = webClient;
        this.kisTokenManager = kisTokenManager;
        this.kisMetricMapper = kisMetricMapper;
        this.kisRateLimiter = kisRateLimiter;
    }

    // 주식현재가 시세 API (FHKST01010100): PER/PBR/시가총액/거래대금/현재가 fallback을 한 번에 가져옵니다.
    // 반환 타입은 필요한 데이터만 모아둔 DTO, 파라메터는 종목 코드
    public KisCurrentPriceMetrics getCurrentPrice(String stockCode) {
        return callWithRetry("현재가 시세", stockCode, () -> {
            kisRateLimiter.acquire();
            KisCurrentPriceResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("fid_cond_mrkt_div_code", MARKET_DIV_CODE_KRX)
                            .queryParam("fid_input_iscd", stockCode)
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_CURRENT_PRICE))
                    .retrieve()
                    .bodyToMono(KisCurrentPriceResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 현재가 시세 조회 실패: stockCode=" + stockCode, e))
                    .block();

            validateSuccess(response, TR_ID_CURRENT_PRICE);
            if (response.output() == null) {
                throw new KisApiException("[KIS] 현재가 시세 응답 output이 비어있습니다. stockCode=" + stockCode);
            }
            return kisMetricMapper.toCurrentPriceMetrics(response.output());
        });
    }

    // 주식현재가 체결 API (FHKST01010300): 초단기 추천 정렬에 사용할 당일 체결강도(tday_rltv)를 가져옵니다.
    public KisExecutionStrengthMetrics getExecutionStrength(String stockCode) {
        return callWithRetry("현재가 체결", stockCode, () -> {
            kisRateLimiter.acquire();
            KisExecutionStrengthResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-ccnl")
                            .queryParam("fid_cond_mrkt_div_code", MARKET_DIV_CODE_KRX)
                            .queryParam("fid_input_iscd", stockCode)
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_EXECUTION_STRENGTH))
                    .retrieve()
                    .bodyToMono(KisExecutionStrengthResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 현재가 체결 조회 실패: stockCode=" + stockCode, e))
                    .block();

            validateSuccess(response, TR_ID_EXECUTION_STRENGTH);
            if (response.output() == null || response.output().isEmpty()) {
                throw new KisApiException("[KIS] 현재가 체결 응답 output이 비어있습니다. stockCode=" + stockCode);
            }
            return kisMetricMapper.toExecutionStrengthMetrics(response.output().get(0));
        });
    }

    // 국내주식 재무비율 API (FHKST66430300): 분기별 배열을 받아 EPS 성장률/ROE/부채비율 계산의 원천 데이터로 사용합니다.
    public List<KisFinancialRatioMetrics> getFinancialRatios(String stockCode) {
        return getFinancialRatios(stockCode, KisFinancialPeriod.QUARTER);
    }

    // 국내주식 재무비율 API (FHKST66430300): YEAR/QUARTER를 선택할 수 있게 열어두어 배치 정책 변경 시 API 클라이언트를 다시 고치지 않도록 합니다.
    public List<KisFinancialRatioMetrics> getFinancialRatios(String stockCode, KisFinancialPeriod period) {
        return callWithRetry("재무비율", stockCode, () -> {
            kisRateLimiter.acquire();
            KisFinancialRatioResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/finance/financial-ratio")
                            .queryParam("fid_div_cls_code", period.getCode())
                            .queryParam("fid_cond_mrkt_div_code", MARKET_DIV_CODE_KRX)
                            .queryParam("fid_input_iscd", stockCode)
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_FINANCIAL_RATIO))
                    .retrieve()
                    .bodyToMono(KisFinancialRatioResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 재무비율 조회 실패: stockCode=" + stockCode, e))
                    .block();

            validateSuccess(response, TR_ID_FINANCIAL_RATIO);
            if (response.output() == null) {
                return List.of();
            }
            return response.output().stream()
                    .map(kisMetricMapper::toFinancialRatioMetrics)
                    .toList();
        });
    }

    // 국내주식 손익계산서 API (FHKST66430200): 매출액/영업이익과 결산년월을 가져와 성장률, 영업이익률 계산에 사용합니다.
    public List<KisIncomeStatementMetrics> getIncomeStatements(String stockCode) {
        return getIncomeStatements(stockCode, KisFinancialPeriod.QUARTER);
    }

    // 국내주식 손익계산서 API (FHKST66430200): 재무비율과 동일하게 YEAR/QUARTER 조회를 지원합니다.
    public List<KisIncomeStatementMetrics> getIncomeStatements(String stockCode, KisFinancialPeriod period) {
        return callWithRetry("손익계산서", stockCode, () -> {
            kisRateLimiter.acquire();
            KisIncomeStatementResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/finance/income-statement")
                            .queryParam("fid_div_cls_code", period.getCode())
                            .queryParam("fid_cond_mrkt_div_code", MARKET_DIV_CODE_KRX)
                            .queryParam("fid_input_iscd", stockCode)
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_INCOME_STATEMENT))
                    .retrieve()
                    .bodyToMono(KisIncomeStatementResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 손익계산서 조회 실패: stockCode=" + stockCode, e))
                    .block();

            validateSuccess(response, TR_ID_INCOME_STATEMENT);
            if (response.output() == null) {
                return List.of();
            }
            return response.output().stream()
                    .map(kisMetricMapper::toIncomeStatementMetrics)
                    .toList();
        });
    }

    // 예탁원정보(배당일정) API (HHKDB669102C0): 기간 내 배당 이벤트를 조회해 현금배당금 기반 시가배당률 계산에 사용합니다.
    public List<KisDividendMetrics> getDividends(String stockCode, LocalDate from, LocalDate to) {
        return callWithRetry("배당일정", stockCode, () -> {
            kisRateLimiter.acquire();
            KisDividendResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/ksdinfo/dividend")
                            .queryParam("cts", "")
                            .queryParam("gb1", "0")
                            .queryParam("f_dt", formatDate(from))
                            .queryParam("t_dt", formatDate(to))
                            .queryParam("sht_cd", stockCode)
                            .queryParam("high_gb", "0")
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_DIVIDEND))
                    .retrieve()
                    .bodyToMono(KisDividendResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 배당일정 조회 실패: stockCode=" + stockCode, e))
                    .block();

            validateSuccess(response, TR_ID_DIVIDEND);
            if (response.output() == null) {
                return List.of();
            }
            return response.output().stream()
                    .map(kisMetricMapper::toDividendMetrics)
                    .toList();
        });
    }

    // 국내주식 신용잔고 일별추이 API (FHPST04760000): 레이 달리오 조건의 전체 융자 잔고 비율을 배치로 적재합니다.
    public List<KisCreditBalanceMetrics> getDailyCreditBalances(String stockCode, LocalDate settlementDate) {
        return callWithRetry("신용잔고", stockCode, () -> {
            kisRateLimiter.acquire();
            KisCreditBalanceResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/daily-credit-balance")
                            .queryParam("fid_cond_mrkt_div_code", MARKET_DIV_CODE_KRX)
                            .queryParam("fid_cond_scr_div_code", CREDIT_BALANCE_SCREEN_DIV_CODE)
                            .queryParam("fid_input_iscd", stockCode)
                            .queryParam("fid_input_date_1", formatDate(settlementDate))
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_CREDIT_BALANCE))
                    .retrieve()
                    .bodyToMono(KisCreditBalanceResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 신용잔고 조회 실패: stockCode=" + stockCode, e))
                    .block();

            validateSuccess(response, TR_ID_CREDIT_BALANCE);
            if (response.output() == null) {
                return List.of();
            }
            return response.output().stream()
                    .map(kisMetricMapper::toCreditBalanceMetrics)
                    .toList();
        });
    }

    // 종목별 투자자매매동향(일별) API (FHPTJ04160001): 최근 20거래일 기관 순매수 합산의 원천 데이터를 가져옵니다.
    public List<KisInvestorTradeDailyMetrics> getInvestorTradesDaily(String stockCode, LocalDate date) {
        return callWithRetry("투자자매매동향", stockCode, () -> {
            kisRateLimiter.acquire();
            KisInvestorTradeDailyResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily")
                            .queryParam("fid_cond_mrkt_div_code", MARKET_DIV_CODE_KRX)
                            .queryParam("fid_input_iscd", stockCode)
                            .queryParam("fid_input_date_1", formatDate(date))
                            .queryParam("fid_org_adj_prc", "")
                            .queryParam("fid_etc_cls_code", "")
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_INVESTOR_TRADE_DAILY))
                    .retrieve()
                    .bodyToMono(KisInvestorTradeDailyResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 투자자매매동향 조회 실패: stockCode=" + stockCode, e))
                    .block();

            validateSuccess(response, TR_ID_INVESTOR_TRADE_DAILY);
            if (response.output() == null) {
                return List.of();
            }
            return response.output().stream()
                    .map(kisMetricMapper::toInvestorTradeDailyMetrics)
                    .toList();
        });
    }

    // 종목조건검색 목록조회 API (HHKST03900300): HTS에 서버저장된 사용자 조건의 seq 목록을 가져옵니다.
    public List<KisHtsConditionTitleOutput> getHtsConditionTitles(String htsUserId) {
        return callWithRetry("HTS 조건검색 목록", htsUserId, () -> {
            kisRateLimiter.acquire();
            KisHtsConditionTitleResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/psearch-title")
                            .queryParam("user_id", htsUserId)
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_HTS_CONDITION_TITLE))
                    .retrieve()
                    .bodyToMono(KisHtsConditionTitleResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] HTS 조건검색 목록 조회 실패: userId=" + htsUserId, e))
                    .block();

            validateHtsConditionSaved(response, htsUserId);
            validateSuccess(response, TR_ID_HTS_CONDITION_TITLE);
            if (response.output() == null) {
                return List.of();
            }
            return response.output();
        });
    }

    // 종목조건검색조회 API (HHKST03900400): HTS 조건 seq에 해당하는 종목 결과를 가져옵니다.
    public List<KisHtsConditionResultOutput> getHtsConditionResults(String htsUserId, String seq) {
        return callWithRetry("HTS 조건검색 결과", htsUserId + ":" + seq, () -> {
            kisRateLimiter.acquire();
            KisHtsConditionResultResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/psearch-result")
                            .queryParam("user_id", htsUserId)
                            .queryParam("seq", seq)
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_HTS_CONDITION_RESULT))
                    .retrieve()
                    .bodyToMono(KisHtsConditionResultResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] HTS 조건검색 결과 조회 실패: userId=" + htsUserId + ", seq=" + seq, e))
                    .block();

            validateHtsConditionSaved(response, htsUserId);
            if (response != null && EMPTY_HTS_RESULT_CODE.equals(response.messageCode())) {
                return List.of();
            }

            validateSuccess(response, TR_ID_HTS_CONDITION_RESULT);
            if (response.output() == null) {
                return List.of();
            }
            return response.output();
        });
    }

    // 국내휴장일조회 API (CTCA0903R): 기준일 이후의 영업일/거래일/개장일 정보를 조회합니다.
    public List<KisDomesticHolidayOutput> getDomesticHolidays(LocalDate baseDate) {
        return callWithRetry("국내휴장일조회", formatDate(baseDate), () -> {
            kisRateLimiter.acquire();
            KisDomesticHolidayResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/chk-holiday")
                            .queryParam("BASS_DT", formatDate(baseDate))
                            .queryParam("CTX_AREA_NK", "")
                            .queryParam("CTX_AREA_FK", "")
                            .build())
                    .headers(headers -> setKisHeaders(headers, TR_ID_DOMESTIC_HOLIDAY))
                    .retrieve()
                    .bodyToMono(KisDomesticHolidayResponse.class)
                    .onErrorMap(e -> new KisApiException("[KIS] 국내휴장일조회 실패: baseDate=" + baseDate, e))
                    .block();

            validateSuccess(response, TR_ID_DOMESTIC_HOLIDAY);
            if (response.output() == null) {
                return List.of();
            }
            return response.output();
        });
    }

    // kis api 헤더 설정
    private void setKisHeaders(HttpHeaders headers, String trId) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(kisTokenManager.getAccessToken());
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        headers.set("custtype", CUSTOMER_TYPE_PERSONAL);
    }

    // kis api의 호출 성공 여부 확인
    private void validateSuccess(KisApiResponse response, String trId) {
        if (response == null) {
            throw new KisApiException("[KIS] 응답이 비어있습니다. tr_id=" + trId);
        }
        if (!RESULT_SUCCESS.equals(response.resultCode())) {
            throw new KisApiException("[KIS] API 실패: tr_id=" + trId
                    + ", msg_cd=" + response.messageCode()
                    + ", msg=" + response.message());
        }
    }

    private void validateHtsConditionSaved(KisApiResponse response, String htsUserId) {
        if (response != null && HTS_CONDITION_NOT_SAVED_CODE.equals(response.messageCode())) {
            throw new KisApiException("[KIS] HTS 조건검색 조건이 서버저장되지 않았거나 API에서 조회 가능한 상태가 아닙니다. "
                    + "eFriend Plus [0110] 조건검색 화면에서 조건을 등록한 뒤 왼쪽 하단의 사용자조건 서버저장을 실행하세요. "
                    + "htsUserId=" + htsUserId + ", msg=" + response.message());
        }
    }

    private <T> T callWithRetry(String operationName, String target, Supplier<T> supplier) {
        int attempts = Math.max(1, maxRetryAttempts);
        long delayMillis = Math.max(0, initialRetryDelayMillis);
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt >= attempts || !isRetryable(e)) {
                    throw e;
                }

                sleepBeforeRetry(operationName, target, attempt, attempts, delayMillis, e);
                delayMillis = nextRetryDelay(delayMillis);
            }
        }

        throw lastException;
    }

    private boolean isRetryable(RuntimeException exception) {
        Throwable rootCause = rootCause(exception);
        if (rootCause instanceof WebClientResponseException responseException) {
            HttpStatusCode statusCode = responseException.getStatusCode();
            return statusCode.value() == 429 || statusCode.is5xxServerError();
        }

        String message = exception.getMessage();
        if (message != null && (
                message.contains("응답 output이 비어있습니다")
                        || message.contains("HTS 조건검색 조건이 서버저장되지 않았거나")
                        || message.contains("[KIS] API 실패:")
                        || message.contains(EMPTY_HTS_RESULT_CODE)
                        || message.contains(HTS_CONDITION_NOT_SAVED_CODE)
        )) {
            return false;
        }

        return rootCause instanceof java.io.IOException
                || rootCause instanceof TimeoutException
                || rootCause.getClass().getName().contains("Timeout")
                || message != null && message.contains("Connection prematurely closed");
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void sleepBeforeRetry(String operationName,
                                  String target,
                                  int attempt,
                                  int maxAttempts,
                                  long delayMillis,
                                  RuntimeException exception) {
        log.warn("[KIS Retry] API 호출 실패 후 재시도 대기: operation={}, target={}, attempt={}/{}, delayMillis={}, error={}",
                operationName, target, attempt, maxAttempts, delayMillis, exception.getMessage());
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new KisApiException("[KIS] API 재시도 대기 중 인터럽트가 발생했습니다. operation="
                    + operationName + ", target=" + target, interruptedException);
        }
    }

    private long nextRetryDelay(long currentDelayMillis) {
        long nextDelayMillis = (long) Math.ceil(currentDelayMillis * retryDelayMultiplier);
        return Math.min(Math.max(currentDelayMillis, nextDelayMillis), maxRetryDelayMillis);
    }

    private String formatDate(LocalDate date) {
        return KIS_DATE_FORMATTER.format(date);
    }
}
