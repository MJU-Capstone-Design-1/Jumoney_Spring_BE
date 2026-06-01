# Master Choice Backtest Feature

## Summary

거장의 선택 백테스팅 검증 기능은 사용자 요청 시점에 KIS REST API를 호출하지 않는다. 필요한 과거 원천 데이터는 배치로 미리 DB에 적재하고, 백테스트 API는 DB 데이터만 조회해 선택 종목의 최근 1년 일봉과 추천 조건 만족 구간을 반환한다.

이 기능은 현재 운영 추천 로직을 검증하는 목적이므로, 운영 추천과 동일하게 연간 재무 데이터 기준으로 평가한다. 운영 추천 저장 구조(`stock_indicators`)는 월별 upsert 테이블이므로 과거 일별 백테스트 원천으로 재사용하지 않는다.

## Goals

- 거장의 선택 4명 전체에 대해 선택 종목이 과거 어느 거래일/구간에서 선택 조건을 만족했는지 검증한다.
- 백테스트 조회 API는 빠르고 안정적이어야 하며, 외부 KIS API 장애나 rate limit에 영향을 받지 않아야 한다.
- 운영 추천 로직은 시연 전 변경하지 않는다.
- 기존 차트 일봉 데이터는 재사용하고, 백테스트에 필요한 보조 지표만 별도 적재한다.

## Non-Goals

- 오늘의 호주머니 추천 백테스팅은 포함하지 않는다.
- DART Open API 연동과 실제 종목별 공시 접수일 반영은 포함하지 않는다.
- 과거 섹터 변경 이력, 과거 대장주 여부 변경 이력은 반영하지 않는다.
- 백테스트 결과를 실제 과거 운영 추천 저장 이력으로 간주하지 않는다. 이 기능은 과거 원천 데이터에 현재 거장 조건을 재적용하는 검증 기능이다.

## Data Policy

### Existing Data Reuse

- `stock_candles`
  - 기존 일봉(`interval_type=DAY`)을 그대로 사용한다.
  - 백테스트 배치에서 일봉을 다시 적재하지 않는다.
  - 52주 고가 대비율 계산을 위해 백테스트 시작일보다 1년 앞선 일봉도 필요하다.
- `stocks`
  - 종목 코드, 종목명, 섹터, 대장주 여부를 사용한다.
  - 과거 섹터 변경 이력은 반영하지 않고 현재 메타데이터를 사용한다.
- `master_options`
  - 사용자가 선택한 조건 ID를 `MasterOptionLogicCode`로 변환하는 데 사용한다.
  - 조건 미선택 시 현재 추천 API와 동일하게 해당 거장의 모든 조건을 적용한다.

### New Backtest Data

백테스트 요청 중 KIS 호출을 피하기 위해 다음 원천 데이터를 별도 테이블에 저장한다.

- 연간 재무 스냅샷
  - 원천: KIS 국내주식 재무비율 `FHKST66430300`, 국내주식 손익계산서 `FHKST66430200`
  - 기준: `KisFinancialPeriod.YEAR`
  - 주요 필드: `settlementYearMonth`, `availableDate`, `roe`, `currentEps`, `lastYearEps`, `debtRatio`, `currentSales`, `lastYearSales`, `operatingProfit`
  - `availableDate = 결산월 말일 + 90일`
- 일별 신용잔고율
  - 원천: KIS 국내주식 신용잔고 일별추이 `FHPST04760000`
  - 주요 필드: `stockId`, `tradeDate`, `marginDebtRate`
  - 레이 달리오 `DALIO_MARGIN_DEBT` 조건에 사용한다.
- 일별 기관 순매수
  - 원천: KIS 종목별 투자자매매동향 일별 `FHPTJ04160001`
  - 주요 필드: `stockId`, `tradeDate`, `institutionNetBuyQuantity`
  - 윌리엄 오닐 `ONEIL_INST_NET_BUY` 조건에서 최근 20거래일 롤링 합산에 사용한다.

### Data Dependency by Master Condition

| Master | Logic Code | Required DB Source | Derived Metric |
|---|---|---|---|
| Warren Buffett | `BUFFETT_ROE` | `master_choice_backtest_financials.roe` | - |
| Warren Buffett | `BUFFETT_PER` | `stock_candles.close_price`, `master_choice_backtest_financials.current_eps` | `close / EPS` |
| Warren Buffett | `BUFFETT_EPS_GROWTH` | `current_eps`, `last_year_eps` | EPS 성장률 |
| Warren Buffett | `BUFFETT_DEBT_RATIO` | `debt_ratio` | - |
| Warren Buffett | `BUFFETT_OPERATING_MARGIN` | `operating_profit`, `current_sales` | 영업이익률 |
| Peter Lynch | `LYNCH_PEG` | `close_price`, `current_eps`, `last_year_eps` | PER / EPS 성장률 |
| Peter Lynch | `LYNCH_EPS_GROWTH` | `current_eps`, `last_year_eps` | EPS 성장률 |
| Peter Lynch | `LYNCH_DEBT_RATIO` | `debt_ratio` | - |
| Peter Lynch | `LYNCH_SALES_GROWTH` | `current_sales`, `last_year_sales` | 매출 성장률 |
| Peter Lynch | `LYNCH_SECTOR` | `stocks.sector` | 현재 섹터 매칭 |
| Ray Dalio | `DALIO_ALL_WEATHER` | `stocks.sector` | 현재 섹터 매칭 |
| Ray Dalio | `DALIO_PER` | `close_price`, `current_eps` | `close / EPS` |
| Ray Dalio | `DALIO_MARGIN_DEBT` | `master_choice_backtest_daily_indicators.margin_debt_rate` | - |
| Ray Dalio | `DALIO_DEBT_RATIO` | `debt_ratio` | - |
| Ray Dalio | `DALIO_EARNINGS_YIELD` | `close_price`, `current_eps` | `100 / PER` |
| William O'Neil | `ONEIL_EPS_GROWTH` | `current_eps`, `last_year_eps` | EPS 성장률 |
| William O'Neil | `ONEIL_ROE` | `roe` | - |
| William O'Neil | `ONEIL_HIGH_52_WEEK` | `stock_candles` | 52주 고가 대비율 |
| William O'Neil | `ONEIL_MARKET_LEADER` | `stocks.is_market_leader` | 현재 대장주 여부 |
| William O'Neil | `ONEIL_INST_NET_BUY` | `institution_net_buy_quantity` | 최근 20거래일 합산 |

## Proposed Tables

### `master_choice_backtest_financials`

`stock_id + settlement_year_month` 기준으로 upsert한다.

| Column | Type | Notes |
|---|---|---|
| `backtest_financial_id` | BIGINT PK | |
| `stock_id` | BIGINT FK | |
| `settlement_year_month` | VARCHAR(6) | KIS `stac_yymm` |
| `available_date` | DATE | `settlement_year_month` 말일 + 90일 |
| `roe` | NUMERIC(19,4) | |
| `current_eps` | NUMERIC(19,4) | |
| `last_year_eps` | NUMERIC(19,4) NULL | 직전 연간 결산 EPS |
| `debt_ratio` | NUMERIC(19,4) | |
| `current_sales` | NUMERIC(19,4) | |
| `last_year_sales` | NUMERIC(19,4) NULL | 직전 연간 결산 매출 |
| `operating_profit` | NUMERIC(19,4) | |

Constraints and indexes:

- Unique: `(stock_id, settlement_year_month)`
- Index: `(stock_id, available_date)`
- FK: `stock_id -> stocks.stock_id`

### `master_choice_backtest_daily_indicators`

`stock_id + trade_date` 기준으로 upsert한다.

| Column | Type | Notes |
|---|---|---|
| `daily_indicator_id` | BIGINT PK | |
| `stock_id` | BIGINT FK | |
| `trade_date` | DATE | KRX 거래일 |
| `margin_debt_rate` | NUMERIC(19,4) NULL | 신용잔고율 |
| `institution_net_buy_quantity` | NUMERIC(19,4) NULL | 기관 순매수 수량 |

일별 테이블은 신용잔고율과 기관 순매수를 한 테이블에 합쳐 저장한다. 둘 중 하나만 적재된 날짜도 허용한다.

Constraints and indexes:

- Unique: `(stock_id, trade_date)`
- Index: `(stock_id, trade_date)`
- FK: `stock_id -> stocks.stock_id`

### Optional Batch Run Status Table

시연 전 수동 실행만 필요하면 필수는 아니지만, 운영 안정성을 위해 배치 실행 상태 테이블을 둘 수 있다.

`master_choice_backtest_batch_runs`

| Column | Type | Notes |
|---|---|---|
| `batch_run_id` | BIGINT PK | |
| `batch_type` | VARCHAR(40) | `FINANCIAL`, `DAILY_INDICATOR`, `ALL` |
| `started_at` | TIMESTAMP | |
| `finished_at` | TIMESTAMP NULL | |
| `target_stock_count` | INTEGER | |
| `success_count` | INTEGER | |
| `failure_count` | INTEGER | |
| `status` | VARCHAR(20) | `RUNNING`, `COMPLETED`, `FAILED` |
| `message` | TEXT NULL | |

## Batch Design

### Batch Scope

초기 구현은 다음 두 가지 실행 범위를 모두 지원한다.

- 전체 종목 backfill
  - `stocks` 전체를 대상으로 한다.
  - 운영/검증 데이터 구축에 사용한다.
- 특정 종목 backfill
  - `stockCode` 목록을 입력받아 삼성전자, SK하이닉스 등 시연 대상만 빠르게 적재한다.
  - 시연 전 장애 범위와 실행 시간을 줄이는 용도로 사용한다.

### Financial Backfill Batch

- 대상 종목: 기본은 `stocks` 전체, 시연 전에는 대표 종목 목록으로 제한 가능
- 대상 기간: 최근 2년 이상의 연간 재무 데이터
  - 최근 1년 백테스트에서 전년 EPS/매출 비교가 필요하므로 최소 2개 이상의 결산년월이 필요하다.
- 처리:
  - 종목별 재무비율 YEAR 조회
  - 종목별 손익계산서 YEAR 조회
  - 같은 `settlementYearMonth`끼리 조인
  - 최신순 정렬 후 바로 이전 연간 데이터를 `lastYear*`로 매핑
  - `master_choice_backtest_financials`에 upsert
- 실패 정책:
  - 종목 단위 실패는 로그로 남기고 다음 종목 처리
  - 전체 Job은 실패 건수가 있으면 실패 상태로 끝내 재실행 가능하게 한다.

### Daily Indicator Backfill Batch

- 대상 종목: 기본은 `stocks` 전체, 시연 전에는 대표 종목 목록으로 제한 가능
- 대상 기간: 최근 1년 + 20거래일 여유분
- 처리:
  - 신용잔고 일별추이 API를 기간 커버될 때까지 반복 조회
  - 투자자매매동향 일별 API를 기간 커버될 때까지 반복 조회
  - `tradeDate` 기준으로 `master_choice_backtest_daily_indicators`에 upsert
- 실패 정책:
  - 일부 일자 누락은 허용한다.
  - 백테스트 API는 누락된 일자의 해당 조건을 불만족으로 평가하고, 필요 시 응답에 데이터 부족 경고를 포함한다.

### KIS Call Estimate

전체 종목을 한 번에 backfill하면 KIS 호출량이 크므로 실행 범위를 통제해야 한다.

- Financial batch
  - 종목당 재무비율 1회 + 손익계산서 1회
  - 200종목 기준 약 400회
- Daily indicator batch
  - 신용잔고: 1회 최대 30건 응답 기준, 최근 1년은 종목당 약 9회 이상
  - 투자자매매동향: 응답 범위에 따라 종목당 여러 회 필요
  - 200종목 전체 실행은 오래 걸릴 수 있으므로 시연 전에는 대표 종목 제한 실행을 우선한다.

KIS REST 호출은 기존 `KisRateLimiter` 정책을 따른다. 배치 중 사용자 요청 API에서 KIS fallback이 발생하지 않도록, 백테스트 API는 KIS client를 의존하지 않는다.

### Manual Admin API

운영 smoke/admin 패턴에 맞춰 수동 실행 API를 추가한다.

- `POST /api/smoke/kis/master-choice/backtest/financials/sync`
  - query: `stockCodes` optional comma-separated
  - 재무 백테스트 데이터를 적재한다.
- `POST /api/smoke/kis/master-choice/backtest/daily-indicators/sync`
  - query: `stockCodes` optional comma-separated
  - query: `fromDate`, `toDate` optional
  - 일별 신용잔고/기관순매수 데이터를 적재한다.
- `GET /api/smoke/kis/master-choice/backtest/status`
  - query: `stockCode`
  - 해당 종목의 재무/일별 데이터 적재 범위와 누락 상태를 조회한다.

정기 스케줄은 시연 MVP 범위에서는 필수로 두지 않는다. 필요 시 추후 `masterChoiceBacktestDataBatchJob`으로 분리한다.

## API Design

### Request

`POST /api/master-choice/masters/{masterId}/backtests/stocks/{stockCode}`

기존 `MasterChoiceRequest`를 재사용한다.

```json
{
  "selectedOptionIds": [1, 2, 3],
  "sectorTypes": ["IT_SEMICONDUCTOR"]
}
```

### Response

- `candles`: 최근 1년 일봉
- `matchedRanges`: 모든 선택 조건을 만족한 연속 거래일 구간
- `dailyEvaluations`: 거래일별 조건 충족 결과
- `dataWarnings`: 선택 조건 평가에 필요한 원천 데이터가 부족한 경우 경고

백테스트 API는 KIS를 호출하지 않고 다음 DB 데이터만 읽는다.

- `stock_candles`
- `stocks`
- `master_choice_backtest_financials`
- `master_choice_backtest_daily_indicators`

Suggested response shape:

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏",
  "stockId": 1,
  "stockCode": "005930",
  "stockName": "삼성전자",
  "fromDate": "2025-06-01",
  "toDate": "2026-06-01",
  "selectedLogicCodes": ["BUFFETT_ROE", "BUFFETT_PER"],
  "dataWarnings": [],
  "candles": [
    {
      "date": "2026-01-02",
      "openPrice": 71000,
      "highPrice": 72000,
      "lowPrice": 70500,
      "closePrice": 71800,
      "volume": 1000000,
      "tradeAmount": 71800000000
    }
  ],
  "matchedRanges": [
    {
      "startDate": "2026-01-02",
      "endDate": "2026-01-17"
    }
  ],
  "dailyEvaluations": [
    {
      "date": "2026-01-02",
      "matched": true,
      "matchedLogicCodes": ["BUFFETT_ROE", "BUFFETT_PER"],
      "matchedConditionCount": 2,
      "totalConditionCount": 2,
      "financialBaseYearMonth": "202412",
      "metrics": {
        "per": 9.42,
        "roe": 15.2,
        "epsGrowthRate": 11.8
      }
    }
  ]
}
```

`metrics`는 프론트가 조건별 상세 근거를 표시할 수 있도록 포함하는 것을 권장한다. 최소 MVP에서 제외할 수 있지만, 시연 설명력은 크게 좋아진다.

## Evaluation Rules

- 연간 재무 데이터는 `availableDate <= tradingDate`인 최신 스냅샷을 적용한다.
- PER은 `해당 거래일 종가 / currentEps`로 계산한다.
- EPS 성장률은 `(currentEps - lastYearEps) / lastYearEps * 100`으로 계산한다.
- 매출 성장률은 `(currentSales - lastYearSales) / lastYearSales * 100`으로 계산한다.
- 영업이익률은 `operatingProfit / currentSales * 100`으로 계산한다.
- PEG는 `PER / EPS 성장률`로 계산한다.
- 이익수익률은 `100 / PER`로 계산한다.
- 52주 고가 대비율은 `해당 거래일 종가 / 직전 1년 고가 * 100`으로 계산한다.
- 신용잔고율은 `master_choice_backtest_daily_indicators.marginDebtRate`를 사용한다.
- 기관 순매수 조건은 `institutionNetBuyQuantity`의 최근 20거래일 합산이 0 이상인지 평가한다.
- 섹터와 대장주 여부는 현재 `stocks` 메타데이터를 사용한다.

### Missing Data Policy

- 일봉이 최근 1년 구간에 하나도 없으면 `BACKTEST_CANDLE_DATA_NOT_FOUND`로 실패한다.
- 특정 거래일에 적용 가능한 재무 스냅샷이 없으면 해당 거래일의 재무 기반 조건은 불만족 처리한다.
- 전체 기간에 재무 스냅샷이 전혀 없으면 `BACKTEST_FINANCIAL_DATA_NOT_FOUND`로 실패한다.
- 신용잔고율 조건을 선택했는데 해당 날짜의 `marginDebtRate`가 없으면 해당 조건은 불만족 처리하고 `dataWarnings`에 누락 범위를 포함한다.
- 기관 순매수 조건을 선택했는데 최근 20거래일 합산에 필요한 일별 데이터가 부족하면 해당 조건은 불만족 처리하고 `dataWarnings`에 누락 범위를 포함한다.
- 52주 고가 계산을 위한 선행 일봉이 부족하면 가능한 데이터만으로 계산하지 않는다. 해당 거래일의 52주 조건은 불만족 처리하고 `dataWarnings`에 선행 일봉 부족을 포함한다.

### Range Merge Policy

- `dailyEvaluations.matched = true`인 거래일을 연속 구간으로 묶는다.
- 주말/휴장일은 거래일 목록에 없으므로 구간 단절로 보지 않는다.
- 중간 거래일이 데이터 부족 또는 조건 불만족이면 구간을 끊는다.

## Implementation Notes

- 현재 구현된 요청 시 KIS 조회 방식은 제거한다.
- `MasterChoiceBacktestService`는 repository 조회와 조건 평가만 담당한다.
- KIS 호출은 전부 batch/backfill service로 이동한다.
- 기존 `stock_indicators`는 운영 추천용 월별 upsert 테이블이므로 백테스트 원천으로 사용하지 않는다.
- 운영 추천 로직은 시연 전 변경하지 않는다.

### Suggested Package Structure

- `domain.masterchoice.domain`
  - `MasterChoiceBacktestFinancial`
  - `MasterChoiceBacktestDailyIndicator`
- `domain.masterchoice.repository`
  - `MasterChoiceBacktestFinancialRepository`
  - `MasterChoiceBacktestDailyIndicatorRepository`
- `domain.masterchoice.service`
  - `MasterChoiceBacktestDataSyncService`
  - `MasterChoiceBacktestService`
  - `MasterChoiceBacktestEvaluator`
- `domain.masterchoice.dto`
  - `MasterChoiceBacktestResponse`
  - `MasterChoiceBacktestDataSyncResponse`

### Migration Plan

1. Add Flyway migration for the two backtest tables.
2. Add domain entities and repositories.
3. Add data sync service that calls KIS and upserts DB rows.
4. Add smoke/admin sync/status endpoints.
5. Replace current request-time KIS implementation in `MasterChoiceBacktestService` with repository-only queries.
6. Add evaluation unit tests with fixed candle/financial/daily indicator fixtures.
7. Add compile/test verification.

### Current Implementation Gap

현재 코드에 추가된 `MasterChoiceBacktestService`는 요청 시점에 KIS를 직접 호출한다. 이 구현은 이 문서의 최종 설계와 맞지 않으므로 다음 구현 단계에서 제거하거나 재작성해야 한다.

## Test Scenarios

- 워런 버핏: 재무 스냅샷 적용일 전후로 ROE/EPS/PER 조건 평가가 달라지는지 검증한다.
- 피터 린치: PEG와 매출 성장률 계산이 운영 추천 기준과 같은 수식인지 검증한다.
- 레이 달리오: 신용잔고율 누락일은 조건 불만족 또는 경고로 처리되는지 검증한다.
- 윌리엄 오닐: 20거래일 기관 순매수 롤링 합산과 52주 고가 대비율을 검증한다.
- 공통: KIS API가 백테스트 요청 경로에서 호출되지 않는지 테스트한다.
- 공통: 만족 거래일이 연속 구간으로 병합되는지 테스트한다.

## Acceptance Criteria

- 백테스트 API 호출 중 `KisApiClient`가 호출되지 않는다.
- 백테스트 API는 대표 종목 1개 기준 DB 조회만으로 1초 내외 응답을 목표로 한다.
- 워런 버핏, 피터 린치, 레이 달리오, 윌리엄 오닐의 모든 `MasterOptionLogicCode`를 평가할 수 있다.
- 선택 조건이 섹터를 요구하면 기존 추천 API와 동일하게 섹터 미입력 에러를 반환한다.
- 응답에는 차트 표시용 일봉과 하이라이트 표시용 `matchedRanges`가 모두 포함된다.
- 배치 데이터가 부족한 경우 API가 조용히 빈 결과만 반환하지 않고 `dataWarnings` 또는 명확한 에러를 제공한다.
