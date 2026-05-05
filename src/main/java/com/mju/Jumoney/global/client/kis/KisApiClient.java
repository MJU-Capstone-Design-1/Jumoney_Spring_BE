package com.mju.Jumoney.global.client.kis;

import com.mju.Jumoney.global.client.kis.dto.KisApiResponse;
import com.mju.Jumoney.global.client.kis.dto.KisCurrentPriceMetrics;
import com.mju.Jumoney.global.client.kis.dto.KisCurrentPriceResponse;
import com.mju.Jumoney.global.client.kis.dto.KisFinancialPeriod;
import com.mju.Jumoney.global.client.kis.dto.KisFinancialRatioMetrics;
import com.mju.Jumoney.global.client.kis.dto.KisFinancialRatioResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

// KIS REST API 호출을 담당 (토큰 발급/캐싱은 KisTokenManager에 위임)
@Component
public class KisApiClient {

    private static final String RESULT_SUCCESS = "0";
    private static final String CUSTOMER_TYPE_PERSONAL = "P";
    private static final String MARKET_DIV_CODE_KRX = "J";

    private static final String TR_ID_CURRENT_PRICE = "FHKST01010100";
    private static final String TR_ID_FINANCIAL_RATIO = "FHKST66430300";

    private final WebClient webClient;
    private final KisTokenManager kisTokenManager;
    private final KisMetricMapper kisMetricMapper;

    @Value("${kis.appkey}")
    private String appKey;

    @Value("${kis.appsecret}")
    private String appSecret;

    public KisApiClient(@Qualifier("kisWebClient") WebClient webClient,
                        KisTokenManager kisTokenManager,
                        KisMetricMapper kisMetricMapper) {
        this.webClient = webClient;
        this.kisTokenManager = kisTokenManager;
        this.kisMetricMapper = kisMetricMapper;
    }

    // 주식현재가 시세: PER/PBR/시가총액/거래대금/현재가 fallback을 한 번에 가져옵니다.
    // 반환 타입은 필요한 데이터만 모아둔 DTO, 파라메터는 종목 코드
    public KisCurrentPriceMetrics getCurrentPrice(String stockCode) {
        // webClient로 GET 요청
        KisCurrentPriceResponse response = webClient.get()
                // 요청 주소 세팅
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("fid_cond_mrkt_div_code", MARKET_DIV_CODE_KRX) // 시장 분류 코드
                        .queryParam("fid_input_iscd", stockCode) // 종목 코드
                        .build())
                // http 헤더 세팅 (setKisHeaders메서드 호출)
                .headers(headers -> setKisHeaders(headers, TR_ID_CURRENT_PRICE))
                // http 요청을 kis 서버로 보내고 응답받기
                .retrieve()
                // JSON 응답 바디를 dto로 역직렬화
                .bodyToMono(KisCurrentPriceResponse.class)
                // 에러 처리
                .onErrorMap(e -> new KisApiException("[KIS] 현재가 시세 조회 실패: stockCode=" + stockCode, e))
                // 동기 처리
                .block();

        //  KIS API 내부 에러 처리
        validateSuccess(response, TR_ID_CURRENT_PRICE);
        if (response.output() == null) {
            throw new KisApiException("[KIS] 현재가 시세 응답 output이 비어있습니다. stockCode=" + stockCode);
        }
        // kisMetricMapper로 BigDecimal로만 이루어진 객체로 변환
        return kisMetricMapper.toCurrentPriceMetrics(response.output());
    }

    // 국내주식 재무비율: 분기별 배열을 받아 EPS 성장률/ROE/부채비율 계산의 원천 데이터로 사용합니다.
    public List<KisFinancialRatioMetrics> getFinancialRatios(String stockCode) {
        return getFinancialRatios(stockCode, KisFinancialPeriod.QUARTER);
    }

    // YEAR/QUARTER를 선택할 수 있게 열어두어 배치 정책 변경 시 API 클라이언트를 다시 고치지 않도록 합니다.
    public List<KisFinancialRatioMetrics> getFinancialRatios(String stockCode, KisFinancialPeriod period) {
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
}
