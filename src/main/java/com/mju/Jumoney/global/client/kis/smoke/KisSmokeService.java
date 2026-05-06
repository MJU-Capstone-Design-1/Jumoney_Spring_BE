package com.mju.Jumoney.global.client.kis.smoke;

import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeApiResult;
import com.mju.Jumoney.global.client.kis.smoke.dto.KisSmokeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
@Profile("local")
@RequiredArgsConstructor
public class KisSmokeService {

    private static final String TR_ID_CURRENT_PRICE = "FHKST01010100";
    private static final String TR_ID_EXECUTION_STRENGTH = "FHKST01010300";
    private static final String TR_ID_FINANCIAL_RATIO = "FHKST66430300";
    private static final String TR_ID_INCOME_STATEMENT = "FHKST66430200";
    private static final String TR_ID_DIVIDEND = "HHKDB669102C0";
    private static final String TR_ID_CREDIT_BALANCE = "FHPST04760000";
    private static final String TR_ID_INVESTOR_TRADE_DAILY = "FHPTJ04160001";

    private final KisApiClient kisApiClient;

    public KisSmokeResponse smoke(String stockCode, LocalDate baseDate, LocalDate dividendFrom, LocalDate dividendTo) {
        List<KisSmokeApiResult> results = new ArrayList<>();

        results.add(callSingle(1, "주식현재가 시세", TR_ID_CURRENT_PRICE,
                () -> kisApiClient.getCurrentPrice(stockCode)));
        results.add(callSingle(2, "주식현재가 체결", TR_ID_EXECUTION_STRENGTH,
                () -> kisApiClient.getExecutionStrength(stockCode)));
        results.add(callList(3, "국내주식 재무비율", TR_ID_FINANCIAL_RATIO,
                () -> kisApiClient.getFinancialRatios(stockCode)));
        results.add(callList(4, "국내주식 손익계산서", TR_ID_INCOME_STATEMENT,
                () -> kisApiClient.getIncomeStatements(stockCode)));
        results.add(callList(5, "예탁원정보 배당일정", TR_ID_DIVIDEND,
                () -> kisApiClient.getDividends(stockCode, dividendFrom, dividendTo)));
        results.add(callList(6, "국내주식 신용잔고 일별추이", TR_ID_CREDIT_BALANCE,
                () -> kisApiClient.getDailyCreditBalances(stockCode, baseDate)));
        results.add(callList(7, "종목별 투자자매매동향 일별", TR_ID_INVESTOR_TRADE_DAILY,
                () -> kisApiClient.getInvestorTradesDaily(stockCode, baseDate)));

        int successCount = (int) results.stream()
                .filter(KisSmokeApiResult::success)
                .count();

        return new KisSmokeResponse(
                stockCode,
                baseDate,
                dividendFrom,
                dividendTo,
                results.size(),
                successCount,
                results.size() - successCount,
                results
        );
    }

    private KisSmokeApiResult callSingle(int step, String name, String trId, Supplier<?> supplier) {
        try {
            Object sample = supplier.get();
            return KisSmokeApiResult.success(step, name, trId, sample == null ? 0 : 1, sample);
        } catch (Exception e) {
            return KisSmokeApiResult.failure(step, name, trId, e.getMessage());
        }
    }

    private <T> KisSmokeApiResult callList(int step, String name, String trId, Supplier<List<T>> supplier) {
        try {
            List<T> items = supplier.get();
            Object sample = items.isEmpty() ? null : items.get(0);
            return KisSmokeApiResult.success(step, name, trId, items.size(), sample);
        } catch (Exception e) {
            return KisSmokeApiResult.failure(step, name, trId, e.getMessage());
        }
    }
}
