package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.exception.StockErrorCode;
import com.mju.Jumoney.domain.stock.exception.StockIndicatorBatchException;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.dividend.KisDividendMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisIncomeStatementMetrics;
import com.mju.Jumoney.global.client.kis.dto.price.KisCurrentPriceMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisCreditBalanceMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisInvestorTradeDailyMetrics;
import com.mju.Jumoney.global.client.kis.enums.KisFinancialPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndicatorBatchService {

    private static final DateTimeFormatter BASE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);
    private static final int RATIO_SCALE = 4;
    private static final int INVESTOR_TRADE_DAYS = 20;

    private final StockRepository stockRepository;
    private final StockIndicatorRepository stockIndicatorRepository;
    private final KisApiClient kisApiClient;

    // Stock 테이블의 전체 종목을 순회하며 StockIndicator를 적재합니다.
    // 한 종목이 실패해도 전체 배치를 중단하지 않고 다음 종목을 계속 처리합니다.
    public StockIndicatorBatchResult syncAll(LocalDate baseDate) {
        String baseTime = toBaseTime(baseDate);
        int successCount = 0; // 성공 개수
        int failureCount = 0; // 실패 개수

        List<Stock> stocks = stockRepository.findAll();
        for (Stock stock : stocks) {
            try {
                sync(stock, baseDate, baseTime);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.warn("[StockIndicatorBatch] 종목 지표 적재 실패: stockCode={}, stockName={}, error={}",
                        stock.getStockCode(), stock.getName(), e.getMessage());
            }
        }

        log.info("[StockIndicatorBatch] 전체 지표 적재 완료: baseDate={}, baseTime={}, totalCount={}, successCount={}, failureCount={}",
                baseDate, baseTime, stocks.size(), successCount, failureCount);
        return new StockIndicatorBatchResult(baseDate, baseTime, stocks.size(), successCount, failureCount);
    }

    // 운영 배치 전 특정 종목만 수동 검증할 때 사용하는 단건 동기화 메서드입니다.
    public void syncOne(Long stockId, LocalDate baseDate) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));
        sync(stock, baseDate, toBaseTime(baseDate));
    }

    // 한 종목에 대해 KIS API를 호출합니다.
    // KIS REST API 여러 개를 호출해 StockIndicator 한 행에 필요한 지표를 조립하고 upsert합니다.
    private void sync(Stock stock, LocalDate baseDate, String baseTime) {
        KisCurrentPriceMetrics currentPrice = kisApiClient.getCurrentPrice(stock.getStockCode());
        List<KisFinancialRatioMetrics> financialRatios = kisApiClient.getFinancialRatios(stock.getStockCode(), KisFinancialPeriod.YEAR);
        List<KisIncomeStatementMetrics> incomeStatements = kisApiClient.getIncomeStatements(stock.getStockCode(), KisFinancialPeriod.YEAR);
        List<KisDividendMetrics> dividends = kisApiClient.getDividends(stock.getStockCode(), baseDate.minusYears(1), baseDate);
        List<KisCreditBalanceMetrics> creditBalances = kisApiClient.getDailyCreditBalances(stock.getStockCode(), baseDate);
        List<KisInvestorTradeDailyMetrics> investorTrades = kisApiClient.getInvestorTradesDaily(stock.getStockCode(), baseDate);

        KisFinancialRatioMetrics currentFinancialRatio = latestFinancialRatio(financialRatios);
        KisFinancialRatioMetrics lastYearFinancialRatio = previousFinancialRatio(financialRatios);
        KisIncomeStatementMetrics currentIncomeStatement = latestIncomeStatement(incomeStatements);
        KisIncomeStatementMetrics lastYearIncomeStatement = previousIncomeStatement(incomeStatements);

        // 배당 관련 값은 최근 1년 DPS 합계 기준으로 계산합니다.
        BigDecimal dps = sumDps(dividends);
        BigDecimal dividendYield = calculateDividendYield(dps, currentPrice.currentPrice());
        BigDecimal payoutRatio = calculatePayoutRatio(dividends, currentFinancialRatio);
        BigDecimal marginDebtRate = latestCreditBalanceRate(creditBalances);
        Long instNetBuy20Days = sumInstitutionNetBuy(investorTrades);
        BigDecimal high52WeekRate = firstNonNull(
                currentPrice.fiftyTwoWeekHighPriceRate(),
                currentPrice.twoHundredFiftyDayHighPriceRate()
        );

        Long marketCap = requiredLong(currentPrice.marketCap(), "marketCap");
        BigDecimal debtRatio = required(currentFinancialRatio.debtRatio(), "debtRatio");
        Long operatingProfit = requiredLong(currentIncomeStatement.operatingProfit(), "operatingProfit");
        BigDecimal operatingProfitGrowthRate = required(currentFinancialRatio.operatingProfitGrowthRate(), "operatingProfitGrowthRate");
        BigDecimal roe = required(currentFinancialRatio.roe(), "roe");
        BigDecimal per = required(currentPrice.per(), "per");
        BigDecimal pbr = required(currentPrice.pbr(), "pbr");
        BigDecimal currentEps = required(currentFinancialRatio.eps(), "currentEps");
        BigDecimal lastYearEps = lastYearFinancialRatio == null ? null : lastYearFinancialRatio.eps();
        Long currentSales = requiredLong(currentIncomeStatement.sales(), "currentSales");
        Long lastYearSales = lastYearIncomeStatement == null ? null : optionalLong(lastYearIncomeStatement.sales());
        BigDecimal requiredMarginDebtRate = required(marginDebtRate, "marginDebtRate");
        BigDecimal requiredHigh52WeekRate = required(high52WeekRate, "high52WeekRate");

        // stock + baseTime은 unique key입니다. 기존 행이 있으면 갱신하고 없으면 새로 생성합니다.
        StockIndicator stockIndicator = stockIndicatorRepository.findByStockAndBaseTime(stock, baseTime)
                .orElseGet(() -> StockIndicator.create(
                        stock,
                        baseTime,
                        marketCap,
                        debtRatio,
                        operatingProfit,
                        operatingProfitGrowthRate,
                        dps,
                        dividendYield,
                        payoutRatio,
                        roe,
                        per,
                        pbr,
                        currentEps,
                        lastYearEps,
                        currentSales,
                        lastYearSales,
                        requiredMarginDebtRate,
                        requiredHigh52WeekRate,
                        instNetBuy20Days
                ));

        stockIndicator.updateMetrics(
                marketCap,
                debtRatio,
                operatingProfit,
                operatingProfitGrowthRate,
                dps,
                dividendYield,
                payoutRatio,
                roe,
                per,
                pbr,
                currentEps,
                lastYearEps,
                currentSales,
                lastYearSales,
                requiredMarginDebtRate,
                requiredHigh52WeekRate,
                instNetBuy20Days
        );

        stockIndicatorRepository.save(stockIndicator);
    }

    // 연간 재무비율 응답 중 가장 최신 결산년월 데이터를 현재 지표로 사용합니다.
    private KisFinancialRatioMetrics latestFinancialRatio(List<KisFinancialRatioMetrics> financialRatios) {
        return sortedFinancialRatios(financialRatios).stream()
                .findFirst()
                .orElseThrow(() -> new StockIndicatorBatchException(StockErrorCode.STOCK_INDICATOR_CURRENT_FINANCIAL_RATIO_MISSING));
    }

    // 최신 결산년월 바로 이전 데이터를 전년 지표로 사용합니다. 신규 상장주는 전년 지표가 없을 수 있습니다.
    private KisFinancialRatioMetrics previousFinancialRatio(List<KisFinancialRatioMetrics> financialRatios) {
        return sortedFinancialRatios(financialRatios).stream()
                .skip(1)
                .findFirst()
                .orElse(null);
    }

    // KIS 결산년월 문자열(yyyyMM)을 기준으로 최신순 정렬합니다.
    private List<KisFinancialRatioMetrics> sortedFinancialRatios(List<KisFinancialRatioMetrics> financialRatios) {
        return financialRatios.stream()
                .filter(metrics -> metrics.settlementYearMonth() != null)
                .sorted(Comparator.comparing(KisFinancialRatioMetrics::settlementYearMonth).reversed())
                .toList();
    }

    // 연간 손익계산서 응답 중 가장 최신 결산년월 데이터를 현재 매출/영업이익으로 사용합니다.
    private KisIncomeStatementMetrics latestIncomeStatement(List<KisIncomeStatementMetrics> incomeStatements) {
        return sortedIncomeStatements(incomeStatements).stream()
                .findFirst()
                .orElseThrow(() -> new StockIndicatorBatchException(StockErrorCode.STOCK_INDICATOR_CURRENT_INCOME_STATEMENT_MISSING));
    }

    // 최신 결산년월 바로 이전 데이터를 전년 매출로 사용합니다. 신규 상장주는 전년 매출이 없을 수 있습니다.
    private KisIncomeStatementMetrics previousIncomeStatement(List<KisIncomeStatementMetrics> incomeStatements) {
        return sortedIncomeStatements(incomeStatements).stream()
                .skip(1)
                .findFirst()
                .orElse(null);
    }

    // KIS 결산년월 문자열(yyyyMM)을 기준으로 최신순 정렬합니다.
    private List<KisIncomeStatementMetrics> sortedIncomeStatements(List<KisIncomeStatementMetrics> incomeStatements) {
        return incomeStatements.stream()
                .filter(metrics -> metrics.settlementYearMonth() != null)
                .sorted(Comparator.comparing(KisIncomeStatementMetrics::settlementYearMonth).reversed())
                .toList();
    }

    // 최근 1년 배당 이벤트의 주당배당금(DPS)을 합산합니다. 배당이 없으면 0으로 저장합니다.
    private BigDecimal sumDps(List<KisDividendMetrics> dividends) {
        return dividends.stream()
                .map(KisDividendMetrics::cashDividendPerShare)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 시가배당률 = 최근 1년 DPS 합계 / 현재가 * 100
    private BigDecimal calculateDividendYield(BigDecimal dps, BigDecimal currentPrice) {
        if (dps == null || currentPrice == null || currentPrice.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return dps.multiply(PERCENT).divide(currentPrice, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    // 배당성향 = 최근 1년 DPS 합계 / EPS * 100. 배당 또는 EPS가 없으면 계산하지 않습니다.
    private BigDecimal calculatePayoutRatio(List<KisDividendMetrics> dividends,
                                            KisFinancialRatioMetrics financialRatio) {
        if (financialRatio == null || dividends == null || dividends.isEmpty()) {
            return null;
        }

        List<BigDecimal> dpsValues = dividends.stream()
                .map(KisDividendMetrics::cashDividendPerShare)
                .filter(Objects::nonNull)
                .toList();

        if (dpsValues.isEmpty()) {
            return null;
        }

        BigDecimal totalDps = dpsValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal eps = financialRatio.eps();

        if (eps == null || eps.signum() <= 0) {
            return null;
        }

        return totalDps.multiply(PERCENT)
                .divide(eps, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    // 신용잔고 API 응답은 최신 데이터가 앞에 온다는 전제로 첫 유효 값을 사용합니다.
    private BigDecimal latestCreditBalanceRate(List<KisCreditBalanceMetrics> creditBalances) {
        return creditBalances.stream()
                .map(KisCreditBalanceMetrics::totalLoanBalanceRate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // 투자자매매동향 응답의 최근 최대 20개 행을 합산해 기관 순매수 수량으로 저장합니다.
    private Long sumInstitutionNetBuy(List<KisInvestorTradeDailyMetrics> investorTrades) {
        BigDecimal total = investorTrades.stream()
                .limit(INVESTOR_TRADE_DAYS)
                .map(KisInvestorTradeDailyMetrics::institutionNetBuyQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    // StockIndicator의 월별 기준값입니다. 예: 2026년 5월 기준 배치면 202605.
    private String toBaseTime(LocalDate baseDate) {
        return YearMonth.from(baseDate).format(BASE_TIME_FORMATTER);
    }

    // nullable=false 컬럼에 잘못된 0을 저장하지 않기 위해 필수 지표 누락은 종목 단위 실패로 처리합니다.
    private BigDecimal required(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new StockIndicatorBatchException(
                    StockErrorCode.STOCK_INDICATOR_REQUIRED_METRIC_MISSING,
                    "field=" + fieldName
            );
        }
        return value;
    }

    // KIS 숫자 응답을 정수 컬럼에 저장하기 위한 변환입니다.
    private Long requiredLong(BigDecimal value, String fieldName) {
        return required(value, fieldName).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private Long optionalLong(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    // 52주 고가 대비율이 없으면 250일 고가 대비율을 fallback으로 사용합니다.
    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    public record StockIndicatorBatchResult(
            LocalDate baseDate,
            String baseTime,
            int totalCount,
            int successCount,
            int failureCount
    ) {
    }
}
