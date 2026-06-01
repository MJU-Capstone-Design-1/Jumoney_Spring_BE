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
import com.mju.Jumoney.global.batch.MarketCalendarService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private static final int HIGH_52_WEEK_MIN_LOOKBACK_DAYS = 200;
    private static final int INSTITUTION_NET_BUY_LOOKBACK_CALENDAR_DAYS = 90;

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
    private final MarketCalendarService marketCalendarService;

    @Value("${kis.batch.zone-id:Asia/Seoul}")
    private String zoneId;

    @Value("${kis.batch.opening-day-lookback-days:14}")
    private int openingDayLookbackDays;

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

        ZoneId batchZoneId = ZoneId.of(zoneId);
        LocalDate previousOpenDate = marketCalendarService.resolvePreviousOpenDay(
                LocalDate.now(batchZoneId),
                openingDayLookbackDays,
                batchZoneId
        );
        LocalDate toDate = resolveBacktestToDate(stock, logicCodes, previousOpenDate);
        LocalDate fromDate = toDate.minusYears(BACKTEST_YEARS);
        List<StockCandle> candles = getDailyCandles(stock, fromDate, toDate);
        List<StockCandle> high52WeekCandles = getDailyCandles(stock, fromDate.minusYears(1), toDate);
        List<MasterChoiceBacktestFinancial> financials = needsFinancialData(logicCodes)
                ? getFinancials(stock)
                : List.of();
        NavigableMap<LocalDate, MasterChoiceBacktestFinancial> financialByAvailableDate = toFinancialByAvailableDate(financials);
        Map<LocalDate, MasterChoiceBacktestDailyIndicator> dailyIndicatorByDate = getDailyIndicatorByDate(
                stock,
                fromDate.minusDays(INSTITUTION_NET_BUY_LOOKBACK_CALENDAR_DAYS),
                toDate
        );
        Map<LocalDate, Long> institutionNetBuy20DaysByDate = calculateInstitutionNetBuy20Days(
                dailyIndicatorByDate,
                fromDate,
                toDate,
                high52WeekCandles
        );
        Map<LocalDate, BigDecimal> high52WeekHighByDate = calculateHigh52WeekHighByDate(high52WeekCandles);

        List<MasterChoiceBacktestResponse.DailyResult> dailyResults = new ArrayList<>();
        List<MasterChoiceBacktestResponse.DataWarning> dataWarnings = new ArrayList<>();
        for (StockCandle candle : candles) {
            LocalDate tradingDate = candle.getCandleTime().toLocalDate();
            MasterChoiceBacktestFinancial financial = latestAvailableFinancial(financialByAvailableDate, tradingDate).orElse(null);
            MasterChoiceBacktestDailyIndicator dailyIndicator = dailyIndicatorByDate.get(tradingDate);
            BacktestIndicator indicator = toIndicator(
                    stock,
                    candle,
                    high52WeekHighByDate,
                    financial,
                    dailyIndicator,
                    institutionNetBuy20DaysByDate.get(tradingDate)
            );

            boolean matched = logicCodes.stream()
                    .allMatch(logicCode -> matches(indicator, logicCode, sectorTypes));
            dataWarnings.addAll(dataWarnings(tradingDate, logicCodes, financial, dailyIndicator, indicator));
            dailyResults.add(new MasterChoiceBacktestResponse.DailyResult(
                    tradingDate,
                    matched
            ));
        }

        return new MasterChoiceBacktestResponse(
                master.getId(),
                master.getMasterCode(),
                stock.getStockCode(),
                fromDate,
                toDate,
                logicCodes,
                dailyResults,
                dataWarnings
        );
    }

    private LocalDate resolveBacktestToDate(Stock stock,
                                            List<MasterOptionLogicCode> logicCodes,
                                            LocalDate previousOpenDate) {
        if (!needsDailyIndicatorData(logicCodes)) {
            return previousOpenDate;
        }

        LocalDate latestDailyIndicatorDate = dailyIndicatorRepository.findTopByStockOrderByTradeDateDesc(stock)
                .map(MasterChoiceBacktestDailyIndicator::getTradeDate)
                .orElseThrow(() -> new CustomException(MasterChoiceErrorCode.BACKTEST_DAILY_INDICATOR_DATA_NOT_FOUND));
        if (latestDailyIndicatorDate.isBefore(previousOpenDate)) {
            return latestDailyIndicatorDate;
        }
        return previousOpenDate;
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

    private NavigableMap<LocalDate, MasterChoiceBacktestFinancial> toFinancialByAvailableDate(
            List<MasterChoiceBacktestFinancial> financials
    ) {
        NavigableMap<LocalDate, MasterChoiceBacktestFinancial> financialByAvailableDate = new TreeMap<>();
        for (MasterChoiceBacktestFinancial financial : financials) {
            financialByAvailableDate.merge(
                    financial.getAvailableDate(),
                    financial,
                    this::latestSettlementFinancial
            );
        }
        return financialByAvailableDate;
    }

    private MasterChoiceBacktestFinancial latestSettlementFinancial(MasterChoiceBacktestFinancial left,
                                                                    MasterChoiceBacktestFinancial right) {
        return left.getSettlementYearMonth().compareTo(right.getSettlementYearMonth()) >= 0 ? left : right;
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

    private Map<LocalDate, Long> calculateInstitutionNetBuy20Days(Map<LocalDate, MasterChoiceBacktestDailyIndicator> indicatorByDate,
                                                                  LocalDate fromDate,
                                                                  LocalDate toDate,
                                                                  List<StockCandle> allCandles) {
        List<LocalDate> tradingDates = allCandles.stream()
                .map(candle -> candle.getCandleTime().toLocalDate())
                .filter(date -> !date.isAfter(toDate))
                .sorted()
                .toList();

        Map<LocalDate, Long> result = new HashMap<>();
        for (int i = 0; i < tradingDates.size(); i++) {
            LocalDate tradingDate = tradingDates.get(i);
            if (tradingDate.isBefore(fromDate)) {
                continue;
            }
            if (i + 1 < INVESTOR_TRADE_DAYS) {
                continue;
            }

            long total = 0L;
            boolean complete = true;
            for (int j = i - INVESTOR_TRADE_DAYS + 1; j <= i; j++) {
                MasterChoiceBacktestDailyIndicator indicator = indicatorByDate.get(tradingDates.get(j));
                if (indicator == null || indicator.getInstitutionNetBuyQuantity() == null) {
                    complete = false;
                    break;
                }
                total += indicator.getInstitutionNetBuyQuantity();
            }
            if (complete) {
                result.put(tradingDate, total);
            }
        }

        return result;
    }

    private Optional<MasterChoiceBacktestFinancial> latestAvailableFinancial(
            NavigableMap<LocalDate, MasterChoiceBacktestFinancial> financialByAvailableDate,
            LocalDate tradingDate
    ) {
        Map.Entry<LocalDate, MasterChoiceBacktestFinancial> entry = financialByAvailableDate.floorEntry(tradingDate);
        return entry == null ? Optional.empty() : Optional.of(entry.getValue());
    }

    private BacktestIndicator toIndicator(Stock stock,
                                          StockCandle candle,
                                          Map<LocalDate, BigDecimal> high52WeekHighByDate,
                                          MasterChoiceBacktestFinancial financial,
                                          MasterChoiceBacktestDailyIndicator dailyIndicator,
                                          Long institutionNetBuy20Days) {
        BigDecimal per = financial == null ? null : per(candle.getClosePrice(), financial.getCurrentEps());
        BigDecimal epsGrowthRate = financial == null ? null : epsGrowthRate(financial.getCurrentEps(), financial.getLastYearEps());
        BigDecimal earningsYield = earningsYield(per);
        BigDecimal peg = peg(per, epsGrowthRate);
        return new BacktestIndicator(
                stock,
                financial == null ? null : financial.getRoe(),
                per,
                epsGrowthRate,
                financial == null ? null : financial.getDebtRatio(),
                financial == null ? null : operatingMargin(financial.getOperatingProfit(), financial.getCurrentSales()),
                financial == null ? null : salesGrowthRate(financial.getCurrentSales(), financial.getLastYearSales()),
                dailyIndicator == null ? null : dailyIndicator.getMarginDebtRate(),
                high52WeekRate(high52WeekHighByDate, candle),
                institutionNetBuy20Days,
                peg,
                earningsYield
        );
    }

    private Map<LocalDate, BigDecimal> calculateHigh52WeekHighByDate(List<StockCandle> candles) {
        List<StockCandle> sortedCandles = candles.stream()
                .sorted(Comparator.comparing(StockCandle::getCandleTime))
                .toList();
        Deque<StockCandle> window = new ArrayDeque<>();
        Deque<StockCandle> maxWindow = new ArrayDeque<>();
        Map<LocalDate, BigDecimal> highByDate = new HashMap<>();

        for (StockCandle candle : sortedCandles) {
            LocalDate targetDate = candle.getCandleTime().toLocalDate();
            LocalDate startDate = targetDate.minusYears(1);
            while (!window.isEmpty() && window.peekFirst().getCandleTime().toLocalDate().isBefore(startDate)) {
                StockCandle removed = window.removeFirst();
                if (!maxWindow.isEmpty() && maxWindow.peekFirst() == removed) {
                    maxWindow.removeFirst();
                }
            }

            window.addLast(candle);
            if (candle.getHighPrice() != null) {
                while (!maxWindow.isEmpty()
                        && maxWindow.peekLast().getHighPrice() != null
                        && maxWindow.peekLast().getHighPrice().compareTo(candle.getHighPrice()) <= 0) {
                    maxWindow.removeLast();
                }
                maxWindow.addLast(candle);
            }

            if (window.size() >= HIGH_52_WEEK_MIN_LOOKBACK_DAYS && !maxWindow.isEmpty()) {
                highByDate.put(targetDate, maxWindow.peekFirst().getHighPrice());
            }
        }

        return highByDate;
    }

    private BigDecimal high52WeekRate(Map<LocalDate, BigDecimal> high52WeekHighByDate, StockCandle target) {
        BigDecimal high = high52WeekHighByDate.get(target.getCandleTime().toLocalDate());
        if (high == null || high.signum() <= 0) {
            return null;
        }
        return target.getClosePrice().multiply(HUNDRED).divide(high, RATIO_SCALE, RoundingMode.HALF_UP);
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

    private BigDecimal peg(BigDecimal per, BigDecimal epsGrowthRate) {
        if (per == null || per.signum() <= 0 || epsGrowthRate == null || epsGrowthRate.signum() <= 0) {
            return null;
        }
        return per.divide(epsGrowthRate, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal earningsYield(BigDecimal per) {
        if (per == null || per.signum() <= 0) {
            return null;
        }
        return HUNDRED.divide(per, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private boolean matches(BacktestIndicator indicator, MasterOptionLogicCode logicCode, Set<SectorType> selectedSectorTypes) {
        return switch (logicCode) {
            case BUFFETT_ROE -> greaterThanOrEqual(indicator.roe(), BUFFETT_MIN_ROE);
            case BUFFETT_PER -> positive(indicator.per()) && lessThanOrEqual(indicator.per(), BUFFETT_MAX_PER);
            case BUFFETT_EPS_GROWTH -> greaterThanOrEqual(indicator.epsGrowthRate(), BUFFETT_MIN_EPS_GROWTH_RATE);
            case BUFFETT_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), BUFFETT_MAX_DEBT_RATIO);
            case BUFFETT_OPERATING_MARGIN ->
                    greaterThanOrEqual(indicator.operatingMargin(), BUFFETT_MIN_OPERATING_MARGIN);

            case LYNCH_PEG -> lessThanOrEqual(indicator.peg(), LYNCH_MAX_PEG);
            case LYNCH_EPS_GROWTH ->
                    between(indicator.epsGrowthRate(), LYNCH_MIN_EPS_GROWTH_RATE, LYNCH_MAX_EPS_GROWTH_RATE);
            case LYNCH_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), LYNCH_MAX_DEBT_RATIO);
            case LYNCH_SALES_GROWTH -> greaterThanOrEqual(indicator.salesGrowthRate(), LYNCH_MIN_SALES_GROWTH_RATE);
            case LYNCH_SECTOR -> matchesSelectedSector(indicator.stock(), selectedSectorTypes);

            case DALIO_ALL_WEATHER -> matchesSelectedSector(indicator.stock(), selectedSectorTypes);
            case DALIO_PER -> positive(indicator.per()) && lessThanOrEqual(indicator.per(), DALIO_MAX_PER);
            case DALIO_MARGIN_DEBT -> lessThanOrEqual(indicator.marginDebtRate(), DALIO_MAX_MARGIN_DEBT_RATE);
            case DALIO_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), DALIO_MAX_DEBT_RATIO);
            case DALIO_EARNINGS_YIELD -> greaterThanOrEqual(indicator.earningsYield(), DALIO_MIN_EARNINGS_YIELD);

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

    private List<MasterChoiceBacktestResponse.DataWarning> dataWarnings(LocalDate tradingDate,
                                                                        List<MasterOptionLogicCode> logicCodes,
                                                                        MasterChoiceBacktestFinancial financial,
                                                                        MasterChoiceBacktestDailyIndicator dailyIndicator,
                                                                        BacktestIndicator indicator) {
        List<MasterChoiceBacktestResponse.DataWarning> warnings = new ArrayList<>();
        if (financial == null && needsFinancialData(logicCodes)) {
            warnings.add(new MasterChoiceBacktestResponse.DataWarning(
                    tradingDate,
                    "FINANCIAL_MISSING",
                    "해당 거래일에 적용 가능한 연간 재무 스냅샷이 없습니다."
            ));
        }
        if (logicCodes.contains(MasterOptionLogicCode.DALIO_MARGIN_DEBT)
                && (dailyIndicator == null || dailyIndicator.getMarginDebtRate() == null)) {
            warnings.add(new MasterChoiceBacktestResponse.DataWarning(
                    tradingDate,
                    "MARGIN_DEBT_MISSING",
                    "해당 거래일의 신용잔고율 데이터가 없습니다."
            ));
        }
        if (logicCodes.contains(MasterOptionLogicCode.ONEIL_INST_NET_BUY)
                && indicator.instNetBuy20Days() == null) {
            warnings.add(new MasterChoiceBacktestResponse.DataWarning(
                    tradingDate,
                    "INSTITUTION_NET_BUY_20D_MISSING",
                    "최근 20거래일 기관 순매수 합산에 필요한 데이터가 부족합니다."
            ));
        }
        if (logicCodes.contains(MasterOptionLogicCode.ONEIL_HIGH_52_WEEK)
                && indicator.high52WeekRate() == null) {
            warnings.add(new MasterChoiceBacktestResponse.DataWarning(
                    tradingDate,
                    "HIGH_52_WEEK_LOOKBACK_MISSING",
                    "52주 고가 대비율 계산에 필요한 선행 일봉 데이터가 부족합니다."
            ));
        }
        return warnings;
    }

    private boolean needsFinancialData(List<MasterOptionLogicCode> logicCodes) {
        return logicCodes.stream()
                .anyMatch(logicCode -> switch (logicCode) {
                    case BUFFETT_ROE,
                         BUFFETT_PER,
                         BUFFETT_EPS_GROWTH,
                         BUFFETT_DEBT_RATIO,
                         BUFFETT_OPERATING_MARGIN,
                         LYNCH_PEG,
                         LYNCH_EPS_GROWTH,
                         LYNCH_DEBT_RATIO,
                         LYNCH_SALES_GROWTH,
                         DALIO_PER,
                         DALIO_DEBT_RATIO,
                         DALIO_EARNINGS_YIELD,
                         ONEIL_EPS_GROWTH,
                         ONEIL_ROE -> true;
                    case LYNCH_SECTOR,
                         DALIO_ALL_WEATHER,
                         DALIO_MARGIN_DEBT,
                         ONEIL_HIGH_52_WEEK,
                         ONEIL_MARKET_LEADER,
                         ONEIL_INST_NET_BUY -> false;
                });
    }

    private boolean needsDailyIndicatorData(List<MasterOptionLogicCode> logicCodes) {
        return logicCodes.stream()
                .anyMatch(logicCode -> logicCode == MasterOptionLogicCode.DALIO_MARGIN_DEBT
                        || logicCode == MasterOptionLogicCode.ONEIL_INST_NET_BUY);
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
            Long instNetBuy20Days,
            BigDecimal peg,
            BigDecimal earningsYield
    ) {
    }
}
