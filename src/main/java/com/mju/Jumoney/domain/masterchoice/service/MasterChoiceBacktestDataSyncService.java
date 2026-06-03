package com.mju.Jumoney.domain.masterchoice.service;

import com.mju.Jumoney.domain.masterchoice.domain.MasterChoiceBacktestDailyIndicator;
import com.mju.Jumoney.domain.masterchoice.domain.MasterChoiceBacktestFinancial;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceBacktestDataStatusResponse;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceBacktestDataSyncResponse;
import com.mju.Jumoney.domain.masterchoice.repository.MasterChoiceBacktestDailyIndicatorRepository;
import com.mju.Jumoney.domain.masterchoice.repository.MasterChoiceBacktestFinancialRepository;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.exception.StockErrorCode;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisIncomeStatementMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisCreditBalanceMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisInvestorTradeDailyMetrics;
import com.mju.Jumoney.global.client.kis.enums.KisFinancialPeriod;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MasterChoiceBacktestDataSyncService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter KIS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int ANNUAL_DISCLOSURE_DELAY_DAYS = 90;
    private static final int INTERIM_DISCLOSURE_DELAY_DAYS = 45;
    private static final int MAX_DAILY_API_WINDOWS = 30;

    private final StockRepository stockRepository;
    private final MasterChoiceBacktestFinancialRepository financialRepository;
    private final MasterChoiceBacktestDailyIndicatorRepository dailyIndicatorRepository;
    private final KisApiClient kisApiClient;
    private final TransactionTemplate transactionTemplate;

    public MasterChoiceBacktestDataSyncResponse syncFinancials(List<String> stockCodes) {
        List<Stock> stocks = resolveStocks(stockCodes);
        List<MasterChoiceBacktestDataSyncResponse.Item> items = new ArrayList<>();

        for (Stock stock : stocks) {
            try {
                int savedCount = syncFinancials(stock);
                items.add(MasterChoiceBacktestDataSyncResponse.Item.success(stock.getStockCode(), stock.getName(), savedCount));
            } catch (Exception e) {
                log.warn("[MasterChoiceBacktestDataSync] 재무 적재 실패: stockCode={}, stockName={}, error={}",
                        stock.getStockCode(), stock.getName(), e.getMessage());
                items.add(MasterChoiceBacktestDataSyncResponse.Item.failure(stock.getStockCode(), stock.getName(), e.getMessage()));
            }
        }

        return toResponse("FINANCIALS", null, null, items);
    }

    public MasterChoiceBacktestDataSyncResponse syncDailyIndicators(List<String> stockCodes,
                                                                    LocalDate fromDate,
                                                                    LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        List<Stock> stocks = resolveStocks(stockCodes);
        List<MasterChoiceBacktestDataSyncResponse.Item> items = new ArrayList<>();

        for (Stock stock : stocks) {
            try {
                int savedCount = syncDailyIndicators(stock, fromDate, toDate);
                items.add(MasterChoiceBacktestDataSyncResponse.Item.success(stock.getStockCode(), stock.getName(), savedCount));
            } catch (Exception e) {
                log.warn("[MasterChoiceBacktestDataSync] 일별 보조지표 적재 실패: stockCode={}, stockName={}, fromDate={}, toDate={}, error={}",
                        stock.getStockCode(), stock.getName(), fromDate, toDate, e.getMessage());
                items.add(MasterChoiceBacktestDataSyncResponse.Item.failure(stock.getStockCode(), stock.getName(), e.getMessage()));
            }
        }

        return toResponse("DAILY_INDICATORS", fromDate, toDate, items);
    }

    @Transactional(readOnly = true)
    public MasterChoiceBacktestDataStatusResponse getStatus(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));

        Optional<MasterChoiceBacktestFinancial> latestFinancial = financialRepository.findTopByStockOrderBySettlementYearMonthDesc(stock);
        Optional<MasterChoiceBacktestDailyIndicator> latestDailyIndicator = dailyIndicatorRepository.findTopByStockOrderByTradeDateDesc(stock);

        return new MasterChoiceBacktestDataStatusResponse(
                stock.getStockCode(),
                stock.getName(),
                financialRepository.countByStock(stock),
                latestFinancial.map(MasterChoiceBacktestFinancial::getSettlementYearMonth).orElse(null),
                latestFinancial.map(MasterChoiceBacktestFinancial::getAvailableDate).orElse(null),
                dailyIndicatorRepository.countByStock(stock),
                latestDailyIndicator.map(MasterChoiceBacktestDailyIndicator::getTradeDate).orElse(null)
        );
    }

    protected int syncFinancials(Stock stock) {
        List<KisFinancialRatioMetrics> ratios = sortedFinancialRatios(
                kisApiClient.getFinancialRatios(stock.getStockCode(), KisFinancialPeriod.QUARTER)
        );
        Map<String, KisIncomeStatementMetrics> incomeStatementBySettlement = kisApiClient
                .getIncomeStatements(stock.getStockCode(), KisFinancialPeriod.QUARTER)
                .stream()
                .filter(metrics -> StringUtils.hasText(metrics.settlementYearMonth()))
                .collect(Collectors.toMap(
                        KisIncomeStatementMetrics::settlementYearMonth,
                        Function.identity(),
                        (left, right) -> left
                ));
        Map<String, KisFinancialRatioMetrics> ratioBySettlement = ratios.stream()
                .collect(Collectors.toMap(
                        KisFinancialRatioMetrics::settlementYearMonth,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<MasterChoiceBacktestFinancial.BacktestFinancialMetrics> backtestMetrics = new ArrayList<>();
        for (KisFinancialRatioMetrics currentRatio : ratios) {
            String lastYearSettlementYearMonth = previousYearSettlementYearMonth(currentRatio.settlementYearMonth());
            KisFinancialRatioMetrics previousRatio = ratioBySettlement.get(lastYearSettlementYearMonth);
            KisIncomeStatementMetrics currentStatement = incomeStatementBySettlement.get(currentRatio.settlementYearMonth());
            KisIncomeStatementMetrics previousStatement = incomeStatementBySettlement.get(lastYearSettlementYearMonth);

            MasterChoiceBacktestFinancial.BacktestFinancialMetrics metrics =
                    new MasterChoiceBacktestFinancial.BacktestFinancialMetrics(
                            stock,
                            currentRatio.settlementYearMonth(),
                            calculateAvailableDate(currentRatio.settlementYearMonth()),
                            currentRatio.roe(),
                            currentRatio.eps(),
                            previousRatio == null ? null : previousRatio.eps(),
                            currentRatio.debtRatio(),
                            toLong(currentStatement == null ? null : currentStatement.sales()),
                            toLong(previousStatement == null ? null : previousStatement.sales()),
                            currentRatio.salesGrowthRate(),
                            toLong(currentStatement == null ? null : currentStatement.operatingProfit())
                    );
            backtestMetrics.add(metrics);
        }

        return transactionTemplate.execute(status -> {
            backtestMetrics.forEach(this::upsertFinancial);
            return backtestMetrics.size();
        });
    }

    protected int syncDailyIndicators(Stock stock, LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, BigDecimal> marginDebtRates = getMarginDebtRates(stock.getStockCode(), fromDate, toDate);
        Map<LocalDate, Long> institutionNetBuyQuantities = getInstitutionNetBuyQuantities(stock.getStockCode(), fromDate, toDate);

        Set<LocalDate> tradeDates = new TreeSet<>();
        tradeDates.addAll(marginDebtRates.keySet());
        tradeDates.addAll(institutionNetBuyQuantities.keySet());

        return transactionTemplate.execute(status -> {
            for (LocalDate tradeDate : tradeDates) {
                upsertDailyIndicator(
                        stock,
                        tradeDate,
                        marginDebtRates.get(tradeDate),
                        institutionNetBuyQuantities.get(tradeDate)
                );
            }
            return tradeDates.size();
        });
    }

    private void upsertFinancial(MasterChoiceBacktestFinancial.BacktestFinancialMetrics metrics) {
        MasterChoiceBacktestFinancial existing = financialRepository
                .findByStockAndSettlementYearMonth(metrics.stock(), metrics.settlementYearMonth())
                .orElse(null);
        if (existing == null) {
            financialRepository.save(MasterChoiceBacktestFinancial.create(metrics));
            return;
        }

        existing.update(metrics);
    }

    private void upsertDailyIndicator(Stock stock,
                                      LocalDate tradeDate,
                                      BigDecimal marginDebtRate,
                                      Long institutionNetBuyQuantity) {
        MasterChoiceBacktestDailyIndicator existing = dailyIndicatorRepository
                .findByStockAndTradeDate(stock, tradeDate)
                .orElse(null);
        if (existing == null) {
            dailyIndicatorRepository.save(MasterChoiceBacktestDailyIndicator.create(
                    stock,
                    tradeDate,
                    marginDebtRate,
                    institutionNetBuyQuantity
            ));
            return;
        }

        existing.update(marginDebtRate, institutionNetBuyQuantity);
    }

    private List<KisFinancialRatioMetrics> sortedFinancialRatios(List<KisFinancialRatioMetrics> ratios) {
        return ratios.stream()
                .filter(metrics -> StringUtils.hasText(metrics.settlementYearMonth()))
                .sorted(Comparator.comparing(KisFinancialRatioMetrics::settlementYearMonth).reversed())
                .toList();
    }

    static LocalDate calculateAvailableDate(String settlementYearMonth) {
        YearMonth yearMonth = YearMonth.parse(settlementYearMonth, YEAR_MONTH_FORMATTER);
        int disclosureDelayDays = isInterimSettlementMonth(yearMonth)
                ? INTERIM_DISCLOSURE_DELAY_DAYS
                : ANNUAL_DISCLOSURE_DELAY_DAYS;
        return yearMonth.atEndOfMonth().plusDays(disclosureDelayDays);
    }

    static String previousYearSettlementYearMonth(String settlementYearMonth) {
        return YearMonth.parse(settlementYearMonth, YEAR_MONTH_FORMATTER)
                .minusYears(1)
                .format(YEAR_MONTH_FORMATTER);
    }

    private static boolean isInterimSettlementMonth(YearMonth yearMonth) {
        int month = yearMonth.getMonthValue();
        return month == 3 || month == 6 || month == 9;
    }

    private Map<LocalDate, BigDecimal> getMarginDebtRates(String stockCode, LocalDate fromDate, LocalDate toDate) {
        return collectDailyWindow(
                toDate,
                fromDate,
                cursor -> kisApiClient.getDailyCreditBalances(stockCode, cursor),
                KisCreditBalanceMetrics::tradeDate,
                KisCreditBalanceMetrics::totalLoanBalanceRate
        );
    }

    private Map<LocalDate, Long> getInstitutionNetBuyQuantities(String stockCode, LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, BigDecimal> quantities = collectDailyWindow(
                toDate,
                fromDate,
                cursor -> kisApiClient.getInvestorTradesDaily(stockCode, cursor),
                KisInvestorTradeDailyMetrics::businessDate,
                KisInvestorTradeDailyMetrics::institutionNetBuyQuantity
        );

        return quantities.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toLong(entry.getValue()),
                        (left, right) -> left,
                        TreeMap::new
                ));
    }

    private <T, V> Map<LocalDate, V> collectDailyWindow(LocalDate toDate,
                                                        LocalDate fromDate,
                                                        Function<LocalDate, List<T>> fetcher,
                                                        Function<T, String> dateExtractor,
                                                        Function<T, V> valueExtractor) {
        Map<LocalDate, V> values = new TreeMap<>();
        LocalDate cursor = toDate;

        for (int i = 0; i < MAX_DAILY_API_WINDOWS && !cursor.isBefore(fromDate); i++) {
            List<T> rows = fetcher.apply(cursor);
            if (rows.isEmpty()) {
                break;
            }

            LocalDate oldestDate = null;
            for (T row : rows) {
                LocalDate tradeDate = parseKisDate(dateExtractor.apply(row)).orElse(null);
                if (tradeDate == null) {
                    continue;
                }

                if (oldestDate == null || tradeDate.isBefore(oldestDate)) {
                    oldestDate = tradeDate;
                }

                if (!tradeDate.isBefore(fromDate) && !tradeDate.isAfter(toDate)) {
                    V value = valueExtractor.apply(row);
                    if (value != null) {
                        values.putIfAbsent(tradeDate, value);
                    }
                }
            }

            if (oldestDate == null || !oldestDate.isBefore(cursor)) {
                break;
            }
            cursor = oldestDate.minusDays(1);
        }

        return values;
    }

    private Optional<LocalDate> parseKisDate(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }

        String normalized = value.trim();
        try {
            if (normalized.length() == 8 && normalized.chars().allMatch(Character::isDigit)) {
                return Optional.of(LocalDate.parse(normalized, KIS_DATE_FORMATTER));
            }
            return Optional.of(LocalDate.parse(normalized));
        } catch (DateTimeParseException e) {
            log.warn("[MasterChoiceBacktestDataSync] KIS 날짜 파싱 실패: value={}", value);
            return Optional.empty();
        }
    }

    private Long toLong(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private List<Stock> resolveStocks(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return stockRepository.findAll();
        }

        List<String> normalizedStockCodes = stockCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedStockCodes.isEmpty()) {
            return stockRepository.findAll();
        }

        List<Stock> stocks = stockRepository.findByStockCodeIn(normalizedStockCodes);
        Set<String> foundStockCodes = stocks.stream()
                .map(Stock::getStockCode)
                .collect(Collectors.toSet());
        List<String> missingStockCodes = normalizedStockCodes.stream()
                .filter(stockCode -> !foundStockCodes.contains(stockCode))
                .toList();
        if (!missingStockCodes.isEmpty()) {
            throw new CustomException(StockErrorCode.STOCK_NOT_FOUND);
        }

        return stocks;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate와 toDate가 필요합니다.");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate는 toDate보다 늦을 수 없습니다.");
        }
    }

    private MasterChoiceBacktestDataSyncResponse toResponse(String syncType,
                                                            LocalDate fromDate,
                                                            LocalDate toDate,
                                                            List<MasterChoiceBacktestDataSyncResponse.Item> items) {
        int successCount = (int) items.stream()
                .filter(MasterChoiceBacktestDataSyncResponse.Item::success)
                .count();
        return new MasterChoiceBacktestDataSyncResponse(
                syncType,
                fromDate,
                toDate,
                items.size(),
                successCount,
                items.size() - successCount,
                items
        );
    }
}
