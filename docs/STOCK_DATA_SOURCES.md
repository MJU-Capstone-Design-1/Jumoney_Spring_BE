# Stock Data Sources

본 문서는 종목 관련 API에서 사용하는 데이터를 배치 적재 데이터, 실시간 Redis 데이터, 요청 시 KIS REST 조회 데이터로 구분해 정리한다.

## Summary

| Category           | Source                       | Storage                      | Request-time KIS REST? | Primary Usage                      |
|--------------------|------------------------------|------------------------------|------------------------|------------------------------------|
| 종목 기본 정보           | `data/stock_data.json`       | `stocks`, `sectors`          | No                     | 종목명, 종목코드, 섹터, 대장주 여부              |
| 종목 지표              | KIS REST batch               | `stock_indicators`           | No                     | 추천 필터/정렬, 검색 정렬                    |
| HTS 조건검색 결과        | KIS REST batch               | `hts_stocks`                 | No                     | 위험 성향별 후보군                         |
| 실시간 현재가            | Node WebSocket Redis         | `stock:latest:{code}`        | No                     | 장중 현재가/등락률 표시                      |
| 현재가 fallback/cache | Spring KIS REST, Redis cache | `stock:current-price:{code}` | Yes                    | 주문, 상세 표시 fallback                 |
| 확정 차트 캔들           | KIS REST chart sync          | `stock_candles`              | No                     | 분/30분/일/주 차트                       |
| 미확정 분봉 캔들          | Node WebSocket/Redis/SSE     | Redis, SSE event             | No                     | MINUTE 차트 초기 스냅샷 보강 및 장중 마지막 분봉 표시 |

## Batch Data

배치 데이터는 사용자 요청 시점에 KIS REST API를 호출하지 않고 DB에 저장된 확정값을 사용한다. 검색 정렬이나 추천처럼 결과 수가 많고 반복 호출될 수 있는 기능은 이 데이터를 우선 사용한다.

### `stocks`, `sectors`

| Field            | Entity                 | Source                 | Notes                         |
|------------------|------------------------|------------------------|-------------------------------|
| `stockCode`      | `Stock.stockCode`      | `data/stock_data.json` | KIS 종목코드                      |
| `name`           | `Stock.name`           | `data/stock_data.json` | 검색 이름순 정렬 기준                  |
| `sector`         | `Stock.sector`         | `data/stock_data.json` | 응답 태그는 `SectorType.name()` 사용 |
| `isMarketLeader` | `Stock.isMarketLeader` | `data/stock_data.json` | 대장주 태그/우선 정렬 기준               |
| `marketType`     | `Stock.marketType`     | `data/stock_data.json` | KOSPI/KOSDAQ 등 시장 구분          |
| `description`    | `Stock.description`    | `data/stock_data.json` | 종목 설명                         |

`DataInitializer`는 `stockCode` 기준으로 기존 종목을 갱신한다. 따라서 JSON 수정사항은 재배포 후 애플리케이션 시작 시 기존 DB에도 반영된다.

### `stock_indicators`

`StockIndicatorBatchService`가 전체 종목을 순회하며 KIS REST API 결과를 조립하고 `stock + baseTime(yyyyMM)` 기준으로 upsert한다.

| Field      | Entity Field                    | KIS Source                     | Notes            |
|------------|---------------------------------|--------------------------------|------------------|
| 시가총액       | `marketCap`                     | 주식현재가 시세 `FHKST01010100`       | 검색/추천 시가총액 정렬 기준 |
| 누적 거래대금    | `accumulatedTradeAmount`        | 주식현재가 시세 `FHKST01010100`       | 거래 활발도 정렬 후보     |
| 체결강도       | `executionStrength`             | 주식현재가 체결 `FHKST01010300`       | 배치 실행 시점의 확정 저장값 |
| PER        | `per`                           | 주식현재가 시세 `FHKST01010100`       | 가치 지표            |
| PBR        | `pbr`                           | 주식현재가 시세 `FHKST01010100`       | 가치 지표            |
| 52주 고가 대비율 | `high52WeekRate`                | 주식현재가 시세 `FHKST01010100`       | 모멘텀/리스크 지표       |
| 부채비율       | `debtRatio`                     | 국내주식 재무비율 `FHKST66430300`      | 안정성 지표           |
| 영업이익증가율    | `operatingProfitGrowthRate`     | 국내주식 재무비율 `FHKST66430300`      | 성장성 지표           |
| ROE        | `roe`                           | 국내주식 재무비율 `FHKST66430300`      | 수익성 지표           |
| EPS        | `currentEps`, `lastYearEps`     | 국내주식 재무비율 `FHKST66430300`      | 성장성 계산           |
| 매출액        | `currentSales`, `lastYearSales` | 국내주식 손익계산서 `FHKST66430200`     | 성장성 계산           |
| 영업이익       | `operatingProfit`               | 국내주식 손익계산서 `FHKST66430200`     | 수익성/안정성 필터       |
| DPS        | `dps`                           | 예탁원정보 배당일정 `HHKDB669102C0`     | 최근 1년 DPS 합계     |
| 배당수익률      | `dividendYield`                 | DPS + 현재가                      | 배치에서 계산          |
| 배당성향       | `payoutRatio`                   | DPS + EPS                      | 배치에서 계산          |
| 신용잔고율      | `marginDebtRate`                | 국내주식 신용잔고 일별추이 `FHPST04760000` | 과열/리스크 지표        |
| 기관 순매수     | `instNetBuy20Days`              | 종목별 투자자매매동향 일별 `FHPTJ04160001` | 최근 최대 20개 행 합산   |

체결강도는 이름만 보면 실시간 데이터처럼 보일 수 있지만, API 응답을 배치에서 읽어 DB에 저장하는 값이다. 검색/추천 정렬에서 이 값을 사용하면 사용자 요청 중 KIS REST 호출이 발생하지 않는다.

### `hts_stocks`

| Field        | Source                   | Notes                    |
|--------------|--------------------------|--------------------------|
| `searchType` | HTS 조건검색 정책              | 안정형, 안전추구형, 수익추구형, 공격투자형 |
| `baseDate`   | 배치 기준일                   | 조건검색 실행일                 |
| `stock`      | 종목조건검색조회 `HHKST03900400` | `stocks`에 존재하는 종목만 저장    |

## Realtime Data

실시간 데이터는 장중 표시용이다. 검색 정렬처럼 많은 종목을 다루는 기능에서 직접 KIS REST fallback을 타면 rate limit과 응답 지연 위험이 커진다.

### Node WebSocket Redis

| Redis Key                     | Source                | Spring Usage                        |
|-------------------------------|-----------------------|-------------------------------------|
| `stock:latest:{code}`         | Node 1분봉 집계의 최신 진행 분봉 | 단건 최신 상태 조회, SSE 초기 스냅샷, 현재가/등락률 표시 |
| `stock:minute-candles:{code}` | Node 1분봉 집계           | MINUTE 차트의 최근 미확정 분봉 병합             |

`StockCurrentPriceService`는 `stock:latest:{code}`를 먼저 조회하고, freshness 조건을 만족하는 경우 현재가/등락률로 사용한다.
`stock:minute-candles:{code}`는 차트 전용이며 현재가/등락률 최신 스냅샷 역할을 대체하지 않는다.

최신 Redis 계약 기준으로 두 key의 payload는 동일하며, 구조는 다음 분봉 raw 포맷을 사용한다.

```json
{
  "code": "005930",
  "minuteTs": 1715511600000,
  "open": 70900,
  "high": 71100,
  "low": 70850,
  "close": 71000,
  "volume": 12500,
  "change": 500,
  "rate": 0.71,
  "strength": 105.3
}
```

분봉 차트 구현 시에는 `minuteTs`를 KST 기준 분 시작 시각으로 변환해 API DTO의 `candleTime`에 매핑해야 한다.

### Spring Current Price Cache

| Redis Key                    | Source                      | Notes       |
|------------------------------|-----------------------------|-------------|
| `stock:current-price:{code}` | Spring KIS REST fallback 결과 | 기본 TTL 600초 |

`StockCurrentPriceService`는 실시간 Redis에 값이 없으면 Spring cache Redis를 조회하고, 이마저 없으면 KIS REST 현재가 API를 호출한다. 따라서 검색 정렬처럼 결과
수가 많은 API에서는 이 서비스를 정렬 기준으로 사용하지 않는 것이 원칙이다.

## Search Sort Policy

종목 검색 정렬은 이름, 현재가, 배치 지표 기준을 제공한다. 시가총액과 누적 거래대금은 배치로 적재된 DB 값을 사용한다.

정렬 기준:

| Sort                | Data Source                               | Direction |
|---------------------|-------------------------------------------|-----------|
| `NAME_ASC`          | `stocks.name`                             | 오름차순      |
| `PRICE_DESC`        | `StockCurrentPriceService.currentPrice`   | 내림차순      |
| `PRICE_ASC`         | `StockCurrentPriceService.currentPrice`   | 오름차순      |
| `MARKET_CAP_DESC`   | `stock_indicators.marketCap`              | 내림차순      |
| `TRADE_AMOUNT_DESC` | `stock_indicators.accumulatedTradeAmount` | 내림차순      |

`PRICE_ASC`, `PRICE_DESC`는 현재가 기반이므로 `StockCurrentPriceService` 정책을 따른다. 이 서비스는 실시간 Redis, Spring cache Redis, KIS REST
fallback 순서로 값을 찾는다. 검색에서 KIS REST fallback을 제거하려면 현재가 조회 전용 메서드를 Redis/cache only 정책으로 분리한다.

## Chart Data Policy

차트는 Spring 초기 스냅샷과 Node 실시간 업데이트의 역할을 분리한다.

| Data        | Owner  | Source                                | Usage                                        |
|-------------|--------|---------------------------------------|----------------------------------------------|
| 확정 분봉       | Spring | KIS `FHKST03010200` 30분 주기 동기화        | DB 저장, 차트 API 반환                             |
| 확정 30분봉     | Spring | 확정 1분봉 집계                             | 1주 차트 반환                                     |
| 확정 일/주/월/년봉 | Spring | KIS `FHKST03010100` backfill/schedule | DB 저장, 차트 API 반환                             |
| 미확정 1분봉     | Node   | `stock:minute-candles:{code}`         | Spring `ONE_DAY` 초기 스냅샷에 병합, SSE로 이후 업데이트 전달 |

현재 Spring 단일 차트 API의 `ONE_DAY`는 DB의 `isFinal=true` 확정 분봉에 Redis 기반 `isFinal=false` 미확정 분봉을 병합해 반환한다. `ONE_WEEK`는 DB 확정
30분봉에 Redis 1분봉으로 집계한 마지막 진행 중 30분봉 1개를 병합한다. `THREE_MONTHS`, `ONE_YEAR`, `FIVE_YEARS`는 실시간 보강 없이 DB 확정 일봉/주봉만 반환한다.
Redis 원본은 DB `StockCandle`과 필드명이 다르므로, Spring은 `minuteTs/open/high/low/close/volume`을 API 캔들 모델로 변환해 병합한다.

차트 기간 매핑은 `1일 -> 1분봉`, `1주 -> 30분봉`, `3달 -> 일봉`, `1년 -> 일봉`, `5년 -> 주봉`을 기준으로 한다. `1주` 차트는 확정 30분봉을 기본으로 사용하고, 장중 마지막 진행
중 30분봉 1개만 1분봉 또는 Redis 실시간 분봉에서 보강한다.

KIS 분봉 동기화는 정각/30분 기준 2분 뒤에 실행하고, 요청 시각 기준 최근 2분을 제외한 정각/30분 단위까지만 DB 확정 저장 대상으로 삼는다. 정규 스케줄은 `09:02`, `09:32`,
`10:02` ... `15:32`에 실행되고, `15:40`에 장 마감 보정 스케줄이 한 번 더 실행된다.

차트 기준 수동 동기화는 `POST /api/local/kis/chart/sync?stockCode=005930`를 사용한다. `period`를 생략하면 오늘 또는 직전 개장일 기준으로 `ONE_DAY`,
`ONE_WEEK`, `THREE_MONTHS`, `ONE_YEAR`, `FIVE_YEARS`에 필요한 데이터를 모두 채운다.
차트 기준 DB 적재 범위 확인은 `GET /api/local/kis/chart/sync/status?stockCode=005930`를 사용한다. `period`를 생략하면 전체 차트 기간 상태를 반환한다.
저수준 검증 API는 `POST /api/local/kis/chart/minute/sync?stockCode=005930`, 특정 영업일 보정은
`POST /api/local/kis/chart/minute/sync/trading-day?tradingDate=2026-05-22&stockCode=005930`, 기존 분봉 전용 범위 확인은
`GET /api/local/kis/chart/minute/sync/status?stockCode=005930`를 사용한다.
