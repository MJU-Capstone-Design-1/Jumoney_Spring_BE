package com.mju.Jumoney.domain.masterchoice.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.domain.MasterOption;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.master.exception.MasterErrorCode;
import com.mju.Jumoney.domain.master.repository.MasterOptionRepository;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.masterchoice.domain.MasterChoiceBacktestDailyIndicator;
import com.mju.Jumoney.domain.masterchoice.domain.MasterChoiceBacktestFinancial;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceBacktestResponse;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceRequest;
import com.mju.Jumoney.domain.masterchoice.exception.MasterChoiceErrorCode;
import com.mju.Jumoney.domain.masterchoice.repository.MasterChoiceBacktestDailyIndicatorRepository;
import com.mju.Jumoney.domain.masterchoice.repository.MasterChoiceBacktestFinancialRepository;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import com.mju.Jumoney.domain.stock.exception.StockErrorCode;
import com.mju.Jumoney.domain.stock.repository.StockCandleRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterChoiceBacktestService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RATIO_SCALE = 4;
    private static final int BACKTEST_YEARS = 1;
    private static final int INVESTOR_TRADE_DAYS = 20;

    private static final BigDecimal BUFFETT_MIN_ROE = BigDecimal.valueOf(15);
    private static final BigDecimal BUFFETT_MAX_PER = BigDecimal.valueOf(15);
    private static final BigDecimal BUFFETT_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(10);
    private static final BigDecimal BUFFETT_MAX_DEBT_RATIO = BigDecimal.valueOf(100);
    private static final BigDecimal BUFFETT_MIN_OPERATING_MARGIN = BigDecimal.valueOf(20);

    private static final BigDecimal LYNCH_MAX_PEG = BigDecimal.ONE;
    private static final BigDecimal LYNCH_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(20);
    private static final BigDecimal LYNCH_MAX_EPS_GROWTH_RATE = BigDecimal.valueOf(50);
    private static final BigDecimal LYNCH_MAX_DEBT_RATIO = BigDecimal.valueOf(100);
    private static final BigDecimal LYNCH_MIN_SALES_GROWTH_RATE = BigDecimal.valueOf(10);

    private static final BigDecimal DALIO_MAX_PER = BigDecimal.valueOf(20);
    private static final BigDecimal DALIO_MAX_MARGIN_DEBT_RATE = BigDecimal.valueOf(5);
    private static final BigDecimal DALIO_MAX_DEBT_RATIO = BigDecimal.valueOf(50);
    private static final BigDecimal DALIO_MIN_EARNINGS_YIELD = BigDecimal.valueOf(3.38);

    private static final BigDecimal ONEIL_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(25);
    private static final BigDecimal ONEIL_MIN_ROE = BigDecimal.valueOf(17);
    private static final BigDecimal ONEIL_MIN_HIGH_52_WEEK_RATE = BigDecimal.valueOf(90);

    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;
    private final StockRepository stockRepository;
    private final StockCandleRepository stockCandleRepository;
    private final MasterChoiceBacktestFinancialRepository financialRepository;
    private final MasterChoiceBacktestDailyIndicatorRepository dailyIndicatorRepository;

    public MasterChoiceBacktestResponse backtest(Long masterId, String stockCode, MasterChoiceRequest request) {
        Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(MasterErrorCode.MASTER_NOT_FOUND));
        Stock stock = stockRepository.findWithSectorByStockCode(stockCode)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));

        List<MasterOption> selectedOptions = resolveSelectedOptions(master, request.selectedOptionIds());
        validateSectorSelection(selectedOptions, request.sectorTypes());
        List<MasterOptionLogicCode> logicCodes = selectedOptions.stream()
                .map(MasterOption::getLogicCode)
                .toList();
        Set<SectorType> sectorTypes = request.sectorTypes() == null || request.sectorTypes().isEmpty()
                ? Set.of()
                : EnumSet.copyOf(request.sectorTypes());

        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusYears(BACKTEST_YEARS);
        List<StockCandle> candles = getDailyCandles(stock, fromDate, toDate);
        List<StockCandle> high52WeekCandles = getDailyCandles(stock, fromDate.minusYears(1), toDate);
        List<MasterChoiceBacktestFinancial> financials = getFinancials(stock);
        Map<LocalDate, MasterChoiceBacktestDailyIndicator> dailyIndicatorByDate = getDailyIndicatorByDate(stock, fromDate, toDate);
        Map<LocalDate, Long> institutionNetBuy20DaysByDate = calculateInstitutionNetBuy20Days(stock, fromDate, toDate);

        List<MasterChoiceBacktestResponse.DailyEvaluation> dailyEvaluations = new ArrayList<>();
        for (StockCandle candle : candles) {
            LocalDate tradingDate = candle.getCandleTime().toLocalDate();
            MasterChoiceBacktestFinancial financial = latestAvailableFinancial(financials, tradingDate).orElse(null);
            MasterChoiceBacktestDailyIndicator dailyIndicator = dailyIndicatorByDate.get(tradingDate);
            BacktestIndicator indicator = toIndicator(
                    stock,
                    candle,
                    high52WeekCandles,
                    financial,
                    dailyIndicator,
                    institutionNetBuy20DaysByDate.get(tradingDate)
            );

            List<MasterOptionLogicCode> matchedCodes = logicCodes.stream()
                    .filter(logicCode -> matches(indicator, logicCode, sectorTypes))
                    .toList();
            dailyEvaluations.add(new MasterChoiceBacktestResponse.DailyEvaluation(
                    tradingDate,
                    matchedCodes.size() == logicCodes.size(),
                    matchedCodes,
                    matchedCodes.size(),
                    logicCodes.size(),
                    financial == null ? null : financial.getSettlementYearMonth()
            ));
        }

        return new MasterChoiceBacktestResponse(
                master.getId(),
                master.getMasterCode(),
                master.getMasterName(),
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                fromDate,
                toDate,
                logicCodes,
                candles.stream().map(this::toCandleResponse).toList(),
                toMatchedRanges(dailyEvaluations),
                dailyEvaluations
        );
    }

    private List<StockCandle> getDailyCandles(Stock stock, LocalDate fromDate, LocalDate toDate) {
        List<StockCandle> candles = stockCandleRepository.findByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                stock.getId(),
                StockCandleIntervalType.DAY,
                fromDate.atStartOfDay(),
                toDate.atTime(23, 59, 59)
        );
        if (candles.isEmpty()) {
            throw new CustomException(MasterChoiceErrorCode.BACKTEST_CANDLE_DATA_NOT_FOUND);
        }
        return candles;
    }

    private List<MasterChoiceBacktestFinancial> getFinancials(Stock stock) {
        List<MasterChoiceBacktestFinancial> financials = financialRepository.findByStockOrderByAvailableDateAsc(stock);
        if (financials.isEmpty()) {
            throw new CustomException(MasterChoiceErrorCode.BACKTEST_FINANCIAL_DATA_NOT_FOUND);
        }
        return financials;
    }

    private Map<LocalDate, MasterChoiceBacktestDailyIndicator> getDailyIndicatorByDate(Stock stock,
                                                                                       LocalDate fromDate,
                                                                                       LocalDate toDate) {
        return dailyIndicatorRepository.findByStockAndTradeDateBetweenOrderByTradeDateAsc(stock, fromDate, toDate)
                .stream()
                .collect(Collectors.toMap(
                        MasterChoiceBacktestDailyIndicator::getTradeDate,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private Map<LocalDate, Long> calculateInstitutionNetBuy20Days(Stock stock, LocalDate fromDate, LocalDate toDate) {
        List<MasterChoiceBacktestDailyIndicator> indicators = dailyIndicatorRepository
                .findByStockAndTradeDateBetweenOrderByTradeDateAsc(stock, fromDate.minusDays(60), toDate)
                .stream()
                .filter(indicator -> indicator.getInstitutionNetBuyQuantity() != null)
                .toList();

        ArrayDeque<MasterChoiceBacktestDailyIndicator> window = new ArrayDeque<>();
        return indicators.stream()
                .map(indicator -> {
                    window.addLast(indicator);
                    while (window.size() > INVESTOR_TRADE_DAYS) {
                        window.removeFirst();
                    }
                    if (window.size() < INVESTOR_TRADE_DAYS || indicator.getTradeDate().isBefore(fromDate)) {
                        return null;
                    }
                    long total = window.stream()
                            .map(MasterChoiceBacktestDailyIndicator::getInstitutionNetBuyQuantity)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .sum();
                    return Map.entry(indicator.getTradeDate(), total);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    private Optional<MasterChoiceBacktestFinancial> latestAvailableFinancial(List<MasterChoiceBacktestFinancial> financials,
                                                                             LocalDate tradingDate) {
        return financials.stream()
                .filter(financial -> !financial.getAvailableDate().isAfter(tradingDate))
                .max(Comparator.comparing(MasterChoiceBacktestFinancial::getSettlementYearMonth));
    }

    private BacktestIndicator toIndicator(Stock stock,
                                          StockCandle candle,
                                          List<StockCandle> high52WeekCandles,
                                          MasterChoiceBacktestFinancial financial,
                                          MasterChoiceBacktestDailyIndicator dailyIndicator,
                                          Long institutionNetBuy20Days) {
        BigDecimal per = financial == null ? null : per(candle.getClosePrice(), financial.getCurrentEps());
        return new BacktestIndicator(
                stock,
                financial == null ? null : financial.getRoe(),
                per,
                financial == null ? null : epsGrowthRate(financial.getCurrentEps(), financial.getLastYearEps()),
                financial == null ? null : financial.getDebtRatio(),
                financial == null ? null : operatingMargin(financial.getOperatingProfit(), financial.getCurrentSales()),
                financial == null ? null : salesGrowthRate(financial.getCurrentSales(), financial.getLastYearSales()),
                dailyIndicator == null ? null : dailyIndicator.getMarginDebtRate(),
                high52WeekRate(high52WeekCandles, candle),
                institutionNetBuy20Days
        );
    }

    private BigDecimal per(BigDecimal closePrice, BigDecimal eps) {
        if (closePrice == null || eps == null || eps.signum() <= 0) {
            return null;
        }
        return closePrice.divide(eps, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal epsGrowthRate(BigDecimal currentEps, BigDecimal lastYearEps) {
        if (currentEps == null || lastYearEps == null || lastYearEps.signum() <= 0) {
            return null;
        }
        return currentEps.subtract(lastYearEps)
                .multiply(HUNDRED)
                .divide(lastYearEps, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal operatingMargin(Long operatingProfit, Long currentSales) {
        if (operatingProfit == null || currentSales == null || currentSales <= 0) {
            return null;
        }
        return BigDecimal.valueOf(operatingProfit)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(currentSales), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal salesGrowthRate(Long currentSales, Long lastYearSales) {
        if (currentSales == null || lastYearSales == null || lastYearSales <= 0) {
            return null;
        }
        return BigDecimal.valueOf(currentSales - lastYearSales)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(lastYearSales), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal peg(BacktestIndicator indicator) {
        if (indicator.per() == null || indicator.per().signum() <= 0
                || indicator.epsGrowthRate() == null || indicator.epsGrowthRate().signum() <= 0) {
            return null;
        }
        return indicator.per().divide(indicator.epsGrowthRate(), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal earningsYield(BacktestIndicator indicator) {
        if (indicator.per() == null || indicator.per().signum() <= 0) {
            return null;
        }
        return HUNDRED.divide(indicator.per(), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal high52WeekRate(List<StockCandle> candles, StockCandle target) {
        LocalDate targetDate = target.getCandleTime().toLocalDate();
        LocalDate startDate = targetDate.minusYears(1);
        BigDecimal high = candles.stream()
                .filter(candle -> {
                    LocalDate date = candle.getCandleTime().toLocalDate();
                    return !date.isBefore(startDate) && !date.isAfter(targetDate);
                })
                .map(StockCandle::getHighPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
        if (high == null || high.signum() <= 0) {
            return null;
        }
        return target.getClosePrice().multiply(HUNDRED).divide(high, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private boolean matches(BacktestIndicator indicator, MasterOptionLogicCode logicCode, Set<SectorType> selectedSectorTypes) {
        return switch (logicCode) {
            case BUFFETT_ROE -> greaterThanOrEqual(indicator.roe(), BUFFETT_MIN_ROE);
            case BUFFETT_PER -> positive(indicator.per()) && lessThanOrEqual(indicator.per(), BUFFETT_MAX_PER);
            case BUFFETT_EPS_GROWTH -> greaterThanOrEqual(indicator.epsGrowthRate(), BUFFETT_MIN_EPS_GROWTH_RATE);
            case BUFFETT_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), BUFFETT_MAX_DEBT_RATIO);
            case BUFFETT_OPERATING_MARGIN ->
                    greaterThanOrEqual(indicator.operatingMargin(), BUFFETT_MIN_OPERATING_MARGIN);

            case LYNCH_PEG -> lessThanOrEqual(peg(indicator), LYNCH_MAX_PEG);
            case LYNCH_EPS_GROWTH ->
                    between(indicator.epsGrowthRate(), LYNCH_MIN_EPS_GROWTH_RATE, LYNCH_MAX_EPS_GROWTH_RATE);
            case LYNCH_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), LYNCH_MAX_DEBT_RATIO);
            case LYNCH_SALES_GROWTH -> greaterThanOrEqual(indicator.salesGrowthRate(), LYNCH_MIN_SALES_GROWTH_RATE);
            case LYNCH_SECTOR -> matchesSelectedSector(indicator.stock(), selectedSectorTypes);

            case DALIO_ALL_WEATHER -> matchesSelectedSector(indicator.stock(), selectedSectorTypes);
            case DALIO_PER -> positive(indicator.per()) && lessThanOrEqual(indicator.per(), DALIO_MAX_PER);
            case DALIO_MARGIN_DEBT -> lessThanOrEqual(indicator.marginDebtRate(), DALIO_MAX_MARGIN_DEBT_RATE);
            case DALIO_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), DALIO_MAX_DEBT_RATIO);
            case DALIO_EARNINGS_YIELD -> greaterThanOrEqual(earningsYield(indicator), DALIO_MIN_EARNINGS_YIELD);

            case ONEIL_EPS_GROWTH -> greaterThanOrEqual(indicator.epsGrowthRate(), ONEIL_MIN_EPS_GROWTH_RATE);
            case ONEIL_ROE -> greaterThanOrEqual(indicator.roe(), ONEIL_MIN_ROE);
            case ONEIL_HIGH_52_WEEK -> greaterThanOrEqual(indicator.high52WeekRate(), ONEIL_MIN_HIGH_52_WEEK_RATE);
            case ONEIL_MARKET_LEADER -> indicator.stock().isMarketLeader();
            case ONEIL_INST_NET_BUY -> indicator.instNetBuy20Days() != null && indicator.instNetBuy20Days() >= 0;
        };
    }

    private boolean matchesSelectedSector(Stock stock, Set<SectorType> selectedSectorTypes) {
        return !selectedSectorTypes.isEmpty()
                && selectedSectorTypes.contains(stock.getSector().getSectorName());
    }

    private boolean greaterThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) >= 0;
    }

    private boolean lessThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) <= 0;
    }

    private boolean between(BigDecimal value, BigDecimal min, BigDecimal max) {
        return greaterThanOrEqual(value, min) && lessThanOrEqual(value, max);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private List<MasterChoiceBacktestResponse.MatchedRange> toMatchedRanges(List<MasterChoiceBacktestResponse.DailyEvaluation> evaluations) {
        List<MasterChoiceBacktestResponse.MatchedRange> ranges = new ArrayList<>();
        LocalDate start = null;
        LocalDate end = null;
        for (MasterChoiceBacktestResponse.DailyEvaluation evaluation : evaluations) {
            if (evaluation.matched()) {
                if (start == null) {
                    start = evaluation.date();
                }
                end = evaluation.date();
                continue;
            }
            if (start != null) {
                ranges.add(new MasterChoiceBacktestResponse.MatchedRange(start, end));
                start = null;
                end = null;
            }
        }
        if (start != null) {
            ranges.add(new MasterChoiceBacktestResponse.MatchedRange(start, end));
        }
        return ranges;
    }

    private MasterChoiceBacktestResponse.Candle toCandleResponse(StockCandle candle) {
        return new MasterChoiceBacktestResponse.Candle(
                candle.getCandleTime().toLocalDate(),
                candle.getOpenPrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getClosePrice(),
                candle.getVolume(),
                candle.getTradeAmount()
        );
    }

    private List<MasterOption> resolveSelectedOptions(Master master, List<Long> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return masterOptionRepository.findByMasterIdOrderByDisplayOrderAsc(master.getId());
        }
        List<Long> distinctIds = selectedOptionIds.stream().distinct().toList();
        List<MasterOption> options = masterOptionRepository.findByIdIn(distinctIds);
        if (options.size() != distinctIds.size()) {
            throw new CustomException(MasterChoiceErrorCode.INVALID_MASTER_OPTION_SELECTION);
        }
        boolean hasInvalidOption = options.stream()
                .anyMatch(option -> !option.getMaster().getId().equals(master.getId()));
        if (hasInvalidOption) {
            throw new CustomException(MasterChoiceErrorCode.INVALID_MASTER_OPTION_SELECTION);
        }
        Map<Long, MasterOption> optionById = options.stream()
                .collect(Collectors.toMap(MasterOption::getId, Function.identity()));
        return distinctIds.stream().map(optionById::get).toList();
    }

    private void validateSectorSelection(List<MasterOption> selectedOptions, List<?> sectorTypes) {
        boolean needsSectorSelection = selectedOptions.stream()
                .map(MasterOption::getLogicCode)
                .anyMatch(this::requiresSectorSelection);
        if (needsSectorSelection && (sectorTypes == null || sectorTypes.isEmpty())) {
            throw new CustomException(MasterChoiceErrorCode.MISSING_MASTER_SECTOR_SELECTION);
        }
        if (!needsSectorSelection && sectorTypes != null && !sectorTypes.isEmpty()) {
            throw new CustomException(MasterChoiceErrorCode.UNSUPPORTED_MASTER_SECTOR_SELECTION);
        }
    }

    private boolean requiresSectorSelection(MasterOptionLogicCode logicCode) {
        return logicCode == MasterOptionLogicCode.LYNCH_SECTOR
                || logicCode == MasterOptionLogicCode.DALIO_ALL_WEATHER;
    }

    private record BacktestIndicator(
            Stock stock,
            BigDecimal roe,
            BigDecimal per,
            BigDecimal epsGrowthRate,
            BigDecimal debtRatio,
            BigDecimal operatingMargin,
            BigDecimal salesGrowthRate,
            BigDecimal marginDebtRate,
            BigDecimal high52WeekRate,
            Long instNetBuy20Days
    ) {
    }
}
