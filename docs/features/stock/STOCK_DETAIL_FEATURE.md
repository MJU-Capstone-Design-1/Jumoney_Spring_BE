# Stock Detail & Chart Specification

본 문서는 종목 상세 화면의 차트 데이터 제공 정책을 정의한다.

상세 지표 조회는 모의투자 API 문서(`docs/features/mockInvestment/MOCK_INVESTMENT_FEATURE.md`)의
`GET /api/mock-investments/stocks/{stockCode}`를 기준으로 한다.

---

## 1. 차트 API 방향

프론트 차트는 `1일`, `1주`, `3달`, `1년`, `5년` 기간 기준으로 제공한다.

### Endpoint

```http
GET /api/mock-investments/stocks/{stockCode}/chart?period=ONE_DAY
GET /api/mock-investments/stocks/{stockCode}/chart?period=ONE_DAY&date=2026-05-21
```

단일 차트 API가 `ONE_DAY`, `ONE_WEEK`, `THREE_MONTHS`, `ONE_YEAR`, `FIVE_YEARS`를 모두 제공한다.

### 기간별 봉 매핑

| period         | 사용할 봉 | 비고                                 |
|----------------|-------|------------------------------------|
| `ONE_DAY`      | 1분봉   | DB 확정 1분봉 + 오늘 기준 Redis 미확정 1분봉 병합 |
| `ONE_WEEK`     | 30분봉  | 확정 30분봉 + 장중 마지막 진행 중 30분봉 1개 병합   |
| `THREE_MONTHS` | 일봉    | DB 확정 일봉                           |
| `ONE_YEAR`     | 일봉    | DB 확정 일봉                           |
| `FIVE_YEARS`   | 주봉    | DB 확정 주봉                           |

### 봉 데이터 소스

| candle type | source                                        | notes       |
|-------------|-----------------------------------------------|-------------|
| 1분봉         | KIS `FHKST03010200`                           | 당일 확정 분봉 저장 |
| 30분봉        | 1분봉 집계 저장                                     | 1주 차트용      |
| 일봉          | KIS `FHKST03010100` + `FID_PERIOD_DIV_CODE=D` | 3달, 1년 차트용  |
| 주봉          | KIS `FHKST03010100` + `FID_PERIOD_DIV_CODE=W` | 5년 차트용      |

---

## 2. 응답 구조

Spring 차트 API는 호출 시점까지의 초기 차트 스냅샷을 반환한다.

- `ONE_DAY`: DB 확정 1분봉 + 오늘 기준 Redis 미확정 1분봉 병합을 지원한다.
- `ONE_WEEK`: DB 확정 30분봉 + 오늘 기준 Redis 1분봉으로 집계한 마지막 진행 중 30분봉 1개 병합을 지원한다.
- `THREE_MONTHS`, `ONE_YEAR`, `FIVE_YEARS`: 실시간 보강 없이 DB 확정 일봉/주봉만 반환한다.
- `date` 생략 시 기준일은 KST 기준 오늘이 개장일이면 오늘, 아니면 직전 개장일이다.

```json
{
  "stockCode": "005930",
  "stockName": "삼성전자",
  "period": "ONE_DAY",
  "intervalType": "MINUTE",
  "date": "2026-05-21",
  "includesRealtime": true,
  "lastFinalCandleTime": "2026-05-21T14:00:00",
  "candles": [
    {
      "candleTime": "2026-05-21T14:00:00",
      "openPrice": 73500,
      "highPrice": 73800,
      "lowPrice": 73400,
      "closePrice": 73700,
      "volume": 120340,
      "tradeAmount": 8840000000,
      "isFinal": true
    },
    {
      "candleTime": "2026-05-21T14:21:00",
      "openPrice": 73700,
      "highPrice": 73900,
      "lowPrice": 73600,
      "closePrice": 73800,
      "volume": 32000,
      "tradeAmount": 2361600000,
      "isFinal": false
    }
  ]
}
```

### Candle Fields

| Field         | Description                                       |
|---------------|---------------------------------------------------|
| `candleTime`  | 캔들 기준 시각. KST 기준, minute는 `yyyy-MM-dd'T'HH:mm:00` |
| `openPrice`   | 시가                                                |
| `highPrice`   | 고가                                                |
| `lowPrice`    | 저가                                                |
| `closePrice`  | 종가                                                |
| `volume`      | 거래량                                               |
| `tradeAmount` | 거래대금. Redis/KIS 원천에 값이 없던 과거 데이터는 `null` 허용          |
| `isFinal`     | DB/KIS 확정 캔들은 `true`, Redis 기반 미확정 분봉은 `false`    |

---

## 3. 저장 모델

차트 저장은 캔들 전용 테이블을 사용한다. 기존 문서의 `StockPrice` 개념은 `stock_candles`로 구체화한다.

### `stock_candles`

| Column          | Type        | Notes                                    |
|-----------------|-------------|------------------------------------------|
| `candle_id`     | BIGINT      | PK                                       |
| `stock_id`      | BIGINT      | FK to `stocks.stock_id`                  |
| `stock_code`    | VARCHAR(10) | 조회/운영 편의를 위한 종목코드 중복 저장                  |
| `interval_type` | VARCHAR(20) | `MINUTE`, `THIRTY_MINUTE`, `DAY`, `WEEK` |
| `candle_time`   | TIMESTAMP   | KST 기준 캔들 시각                             |
| `open_price`    | NUMERIC     | 시가                                       |
| `high_price`    | NUMERIC     | 고가                                       |
| `low_price`     | NUMERIC     | 저가                                       |
| `close_price`   | NUMERIC     | 종가                                       |
| `volume`        | BIGINT      | 거래량                                      |
| `trade_amount`  | BIGINT      | 거래대금, nullable                           |
| `is_final`      | BOOLEAN     | 확정 여부                                    |
| `created_at`    | TIMESTAMP   | 생성 시각                                    |
| `updated_at`    | TIMESTAMP   | 수정 시각                                    |

Unique key:

```text
unique(stock_id, interval_type, candle_time)
```

KIS REST로 다시 받은 같은 캔들은 upsert한다. KIS 값을 확정 데이터의 기준으로 둔다.

---

## 4. 분봉 정책

차트 저장은 “확정 캔들 저장 + 장중 마지막 진행 중 봉 보강” 구조를 기준으로 한다.

### Spring 역할

- `09:02`, `09:32`, `10:02` ... `15:32`에 KIS `FHKST03010200`을 호출한다.
- `15:40`에 장 마감 보정 동기화를 한 번 더 실행한다.
- KIS가 제공하는 당일 분봉을 `stock_candles(interval_type=MINUTE)`에 upsert한다.
- 요청 시각 기준 최근 2분은 확정 저장 대상에서 제외하고, 그 값을 정각/30분 단위로 내린 시각까지만 저장한다.
- 종목별로 오늘 저장된 마지막 분봉 다음 시각부터만 증분 동기화한다.
- KRX 장마감 동시호가 구간인 `15:20~15:29`는 체결이 없으므로, DB 확정 분봉 저장 시 `15:19` 종가를 기준으로 `volume=0` 1분봉을 보강한다.
- `15:30`은 장마감 단일가 체결 봉으로 별도 저장한다.
- `ONE_DAY` 차트 API는 DB 확정 분봉에 오늘 날짜일 때만 Redis 미확정 분봉을 병합한다.
- `1주` 차트를 위해 확정 30분봉을 1분봉 동기화 시 함께 집계 저장한다.
- 장중 `1주` 차트의 마지막 진행 중 30분봉은 Redis 실시간 분봉에서 즉석 집계해 응답에 병합한다.

### Node 역할

- KIS WebSocket `H0STCNT0` tick을 수신한다.
- tick을 같은 `minuteTs` 기준으로 1분봉 미확정 캔들로 집계한다.
- `stock:minute-candles:{code}` ZSET에 최근 40분 미확정 분봉을 저장한다.
- `stock:latest:{code}`에는 현재 진행 중인 최신 분봉 1개를 String으로 저장한다. 현재가/등락률 표시와 단건 최신 상태 조회는 이 key를 사용한다.
- SSE로 Spring 초기 응답 이후의 `MINUTE_CANDLE_UPDATE` 이벤트를 보낸다.

```json
{
  "type": "MINUTE_CANDLE_UPDATE",
  "stockCode": "005930",
  "candle": {
    "candleTime": "2026-05-21T14:21:00",
    "openPrice": 73700,
    "highPrice": 73900,
    "lowPrice": 73600,
    "closePrice": 73800,
    "volume": 32000,
    "tradeAmount": 2361600000,
    "isFinal": false
  }
}
```

### Frontend 역할

- 현재는 Spring 차트 API로 DB 확정 분봉 스냅샷만 로드한다.
- Redis/SSE 연동 이후에는 Node SSE의 `MINUTE_CANDLE_UPDATE`를 추가로 반영한다.
- 기간 선택 UI는 `1일`, `1주`, `3달`, `1년`, `5년` 기준으로 구성한다.

### Redis 분봉 계약

`stock:latest:{code}`와 `stock:minute-candles:{code}`는 같은 raw 분봉 payload를 사용한다. 전자는 "현재 진행 중인 최신 분봉 1개", 후자는 "최근 40분 분봉
배열"이다.
현재가/등락률은 `stock:latest:{code}`에서 읽고, 차트는 `stock:minute-candles:{code}`를 병합한다.

`stock:minute-candles:{code}`:

| Item       | Value                |
|------------|----------------------|
| Redis type | Sorted Set           |
| Score      | `minuteTs` ms epoch  |
| Member     | 아래 JSON 문자열          |
| Retention  | 최근 40분 / key TTL 1시간 |

```json
{
  "code": "005930",
  "minuteTs": 1715511600000,
  "open": 70900,
  "high": 71100,
  "low": 70850,
  "close": 71000,
  "volume": 12500,
  "tradeAmount": 8875000000,
  "change": 500,
  "rate": 0.71,
  "strength": 105.3
}
```

주의:

- Redis raw payload는 DB/API 캔들 DTO의 `candleTime`, `openPrice`, `closePrice`, `isFinal` 구조와 다르다.
- Spring 차트 병합 시 `minuteTs -> candleTime`, `open -> openPrice`, `close -> closePrice` 등 별도 매핑이 필요하다.
- Redis raw `tradeAmount`는 Spring 차트 응답의 `tradeAmount`로 전달한다. `ONE_WEEK`의 장중 진행 30분봉은 Redis 1분봉들의 `tradeAmount`를 합산한다.
- Redis raw `strength`는 오늘의 호주머니 초단기 추천 정렬에서 freshness 조건을 만족할 때 우선 사용한다. 모의투자 상세의 공개 체결강도(`investmentMetrics.executionStrength`)는 DB `StockIndicator.executionStrength`를 사용한다.
- Redis 분봉은 미확정 실시간용이므로 `isFinal=false`는 저장값으로 존재하지 않고, 병합 단계에서 애플리케이션이 부여하는 개념으로 본다.

---

## 5. 적재 정책

### 과거 데이터 backfill

분봉 과거 데이터는 특정 영업일 수동 동기화 API로 보정할 수 있다. 30분봉은 1분봉 동기화 과정에서 확정 1분봉을 집계해 함께 저장한다.

차트 기간 기준 수동 적재 API:

```http
POST /api/smoke/kis/chart/sync
POST /api/smoke/kis/chart/sync?period=ONE_DAY
POST /api/smoke/kis/chart/sync?period=ONE_WEEK
POST /api/smoke/kis/chart/sync?period=THREE_MONTHS
POST /api/smoke/kis/chart/sync?period=ONE_YEAR
POST /api/smoke/kis/chart/sync?period=FIVE_YEARS
POST /api/smoke/kis/chart/sync?stockCode=005930
GET /api/smoke/kis/chart/sync/status?stockCode=005930
GET /api/smoke/kis/chart/sync/status?stockCode=005930&period=ONE_WEEK
```

`period`를 생략하면 오늘 또는 직전 개장일 기준으로 1일/1주/3달/1년/5년 차트에 필요한 데이터를 모두 동기화한다. `ONE_YEAR`는 `THREE_MONTHS`의 일봉 범위를 포함하므로 전체
동기화에서는 `DAY` 기간봉을 최근 1년 범위로 한 번만 호출한다.

상태 확인 API는 `period` 생략 시 전체 차트 기간의 DB 캔들 범위와 건수를 반환한다.

### 분봉 smoke/backfill

분봉은 당일 API와 특정 영업일 API를 모두 지원한다. 특정 영업일 수동 동기화도 1분봉 저장 후 완성된 30분 버킷을 `THIRTY_MINUTE`로 함께 집계 저장한다.

수동 API:

```http
POST /api/smoke/kis/chart/minute/sync
POST /api/smoke/kis/chart/minute/sync?stockCode=005930
POST /api/smoke/kis/chart/minute/sync/trading-day?tradingDate=2026-05-22
POST /api/smoke/kis/chart/minute/sync/trading-day?tradingDate=2026-05-22&stockCode=005930
```

용도:

- 구현 직후 특정 종목의 당일 분봉 저장 검증
- 장중 30분 스케줄러와 동일한 저장 로직 수동 실행
- 장중 장애 또는 배치 실패 후 당일/특정 영업일 분봉 보정

정책:

- `stockCode`를 지정하면 해당 종목만 실행한다.
- `stockCode`를 생략하면 등록된 전체 종목을 대상으로 실행한다.
- KIS `FHKST03010200`은 한 번에 최대 30건만 반환하므로, 수동 동기화는 필요한 정각/30분 입력 시각만 여러 번 호출한다.
- 과거 영업일은 KIS `FHKST03010230`을 사용하며, 응답에 다른 날짜 raw가 섞여도 요청한 `tradingDate` 분봉만 저장한다. 휴장일/주말/미래일은 동기화하지 않는다.
- 종목별로 오늘 저장된 마지막 분봉 다음 시각부터만 증분 동기화한다.
- 수동 동기화도 스케줄러와 동일하게 정각/30분 단위까지만 `isFinal=true`로 저장한다.
- 예: `14:20` 실행이면 DB 확정 저장 대상은 `09:00~14:00`이다.
- 응답의 `kisRequestCount`로 실제 KIS 호출 횟수를 확인한다.
- 차트 상태 확인 API는 Redis 미확정 분봉이 아니라 DB 확정 캔들의 범위와 건수를 검증한다.

### 장중 분봉 동기화

- KOSPI 200 전체 종목을 대상으로 30분마다 실행한다.
- KIS REST rate limiter를 적용한다.
- 실패 종목은 로그와 결과 응답에 남기고 다음 종목을 계속 처리한다.
- 같은 캔들은 unique key 기준 upsert한다.
- 스케줄 실행 시각은 정각/30분 정각보다 정확히 2분 늦게 둔다. 예: `09:32`, `10:02`, `10:32`.
- 장 마감 보정 스케줄은 `15:40`에 한 번 더 실행한다.
- 저장 cutoff도 요청 시각 기준 최근 2분을 제외한다. 예: `14:32` 실행이면 `14:30`까지 확정 저장한다.
- 종목별로 오늘 저장된 마지막 분봉 다음 시각부터만 증분 동기화한다.

부하 추정:

```text
30분마다 200 requests
1시간 400 requests
장중 6.5시간 기준 하루 약 2,600 requests
250ms 간격 제한 시 200종목 한 바퀴 최소 약 50초
```

30분 주기 안에 처리 가능한 수준을 목표로 한다.

### 30분봉/일봉/주봉 확정 동기화

1주 차트용 30분봉은 1분봉 확정 데이터로부터 집계 저장한다. 3달/1년/5년 차트용 일봉/주봉은 KIS 국내주식기간별시세 `FHKST03010100`을 사용한다.

KIS 요청:

| interval        | source                  | 저장 기준 `candleTime`         |
|-----------------|-------------------------|----------------------------|
| `THIRTY_MINUTE` | 1분봉 집계                  | KST 기준 30분 버킷 시작 시각        |
| `DAY`           | `FID_PERIOD_DIV_CODE=D` | 영업일 `00:00:00`             |
| `WEEK`          | `FID_PERIOD_DIV_CODE=W` | KIS가 반환한 주봉 기준일 `00:00:00` |

저장 매핑:

| source        | mapping                                                                                                                                                                                           |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 30분봉 집계       | 같은 30분 버킷의 1분봉으로 OHLCV 집계. `15:00` 버킷은 synthetic `15:20~15:29` 분봉을 포함하고, `15:30`은 별도 단일가 체결 봉으로 저장                                                                                                  |
| KIS `output2` | `stck_bsop_date -> candle_time`, `stck_oprc -> open_price`, `stck_hgpr -> high_price`, `stck_lwpr -> low_price`, `stck_clpr -> close_price`, `acml_vol -> volume`, `acml_tr_pbmn -> trade_amount` |

초기 backfill:

- `POST /api/smoke/kis/chart/sync`로 전체 차트 기간 또는 특정 차트 기간을 기준으로 전체 종목/단일 종목을 적재한다.
- `THIRTY_MINUTE`: 1주 차트 제공 범위를 커버할 만큼 최근 영업일 데이터를 집계 저장한다.
- `DAY`: 최근 1년 이상을 우선 적재한다.
- `WEEK`: 최근 5년 이상을 우선 적재한다.
- 같은 캔들은 `unique(stock_id, interval_type, candle_time)` 기준으로 upsert한다.

수동 API:

```http
POST /api/smoke/kis/chart/sync
POST /api/smoke/kis/chart/sync?period=ONE_WEEK
POST /api/smoke/kis/chart/minute/sync/trading-day?tradingDate=2026-05-22
GET /api/smoke/kis/chart/sync/status?stockCode=005930
```

정기 동기화:

- `THIRTY_MINUTE`: 1분봉 확정 저장 이후 집계해 upsert한다.
- `DAY`: 장 마감 후 당일 일봉을 upsert한다.
- `WEEK`: 장 마감 후 최신 주봉을 upsert한다.

차트 조회:

- `1일`: DB 확정 1분봉에 오늘 기준 Redis 미확정 1분봉을 병합한다.
- `1주`: DB 확정 30분봉에 장중 마지막 진행 중 30분봉 1개를 병합한다.
- `3달`, `1년`: DB 확정 일봉만 반환한다.
- `5년`: DB 확정 주봉만 반환한다.

---

## 6. 정합성 정책

- DB 확정 캔들의 기준은 KIS REST 응답이다.
- `15:20~15:29` 장마감 동시호가 구간은 체결이 없으므로, Spring이 `15:19` 종가 기준 synthetic 확정 1분봉으로 보강한다.
- Redis/SSE 미확정 캔들은 표시용이며 DB 확정 캔들을 대체하지 않는다.
- WebSocket 장애로 Redis/SSE 구간이 비어도 다음 KIS 분봉 동기화에서 DB 확정 캔들이 복구된다.
- Spring 차트 API와 Node SSE는 `candleTime` 기준을 동일하게 사용한다.
- 같은 `candleTime`이 충돌하면 `isFinal=true`인 Spring DB 값을 우선한다.
- 14:25에 `MINUTE` 차트 API를 호출하면 Spring은 DB 확정 캔들에 Redis 미확정 캔들을 병합해 14:25까지의 스냅샷을 반환하고, 14:25 이후 변화는 Node SSE가 전달한다.
- 14:32에 스케줄러가 실행 중일 때 사용자가 `MINUTE` 차트 API를 호출하면, Spring은 그 시점에 DB에 커밋된 확정 캔들까지만 읽고 Redis 미확정 캔들을 병합한다. 스케줄러가 아직 해당 종목의
  `14:30` 확정 캔들을 저장하지 않았다면 Redis의 `14:30` 미확정 캔들을 반환할 수 있고, 저장이 끝난 뒤 재조회하면 DB의 `isFinal=true` 캔들이 우선된다.
- 스케줄러는 종목별 upsert를 짧은 트랜잭션으로 커밋한다. 전체 200종목 동기화가 끝날 때까지 차트 조회를 막지 않는다.
