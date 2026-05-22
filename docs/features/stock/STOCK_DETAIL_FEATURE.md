# Stock Detail & Chart Specification

본 문서는 종목 상세 화면의 차트 데이터 제공 정책을 정의한다.

상세 지표 조회는 모의투자 API 문서(`docs/features/mockInvestment/MOCK_INVESTMENT_FEATURE.md`)의
`GET /api/mock-investments/stocks/{stockCode}`를 기준으로 한다.

---

## 1. 차트 API 방향

차트는 KIS가 제공하는 봉 단위를 그대로 노출한다.

### Endpoint

```http
GET /api/mock-investments/stocks/{stockCode}/charts/minute
GET /api/mock-investments/stocks/{stockCode}/charts/minute?date=2026-05-21
```

현재 구현은 `MINUTE`만 제공한다. `DAY`, `WEEK`, `MONTH`, `YEAR`는 아직 미구현이다.

### Interval

| interval | 의미     | KIS API                   | KIS 파라미터                |
|----------|--------|---------------------------|-------------------------|
| `MINUTE` | 당일 1분봉 | 주식당일분봉조회 `FHKST03010200`  | 당일 분봉                   |
| `DAY`    | 일봉     | 국내주식기간별시세 `FHKST03010100` | `FID_PERIOD_DIV_CODE=D` |
| `WEEK`   | 주봉     | 국내주식기간별시세 `FHKST03010100` | `FID_PERIOD_DIV_CODE=W` |
| `MONTH`  | 월봉     | 국내주식기간별시세 `FHKST03010100` | `FID_PERIOD_DIV_CODE=M` |
| `YEAR`   | 연봉     | 국내주식기간별시세 `FHKST03010100` | `FID_PERIOD_DIV_CODE=Y` |

프론트가 `1일`, `1주일`, `1개월`, `1년`, `5년` UX를 원하면 프론트 내부에서 위 interval로 매핑한다. 백엔드 API는 우선 봉 단위만 책임진다.

---

## 2. 응답 구조

Spring 차트 API는 호출 시점까지의 초기 차트 스냅샷을 반환한다.

- 현재 `MINUTE`: DB 확정 분봉만 반환한다. 응답의 `includesRealtime=false`가 이 상태를 뜻한다.
- Redis 미확정 분봉 병합은 아직 미구현이다.
- `DAY`, `WEEK`, `MONTH`, `YEAR`는 아직 미구현이다.

```json
{
  "stockCode": "005930",
  "stockName": "삼성전자",
  "intervalType": "MINUTE",
  "date": "2026-05-21",
  "includesRealtime": false,
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
| `tradeAmount` | 거래대금. KIS 응답에 없거나 초기 구현에서 제외하면 `null` 허용          |
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
| `interval_type` | VARCHAR(20) | `MINUTE`, `DAY`, `WEEK`, `MONTH`, `YEAR` |
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

분봉 DB 데이터는 KIS REST를 확정 기준으로 삼는다.

### Spring 역할

- `09:02`, `09:32`, `10:02` ... `15:32`에 KIS `FHKST03010200`을 호출한다.
- `15:40`에 장 마감 보정 동기화를 한 번 더 실행한다.
- KIS가 제공하는 당일 분봉을 `stock_candles(interval_type=MINUTE)`에 upsert한다.
- 요청 시각 기준 최근 2분은 확정 저장 대상에서 제외하고, 그 값을 정각/30분 단위로 내린 시각까지만 저장한다.
- 종목별로 오늘 저장된 마지막 분봉 다음 시각부터만 증분 동기화한다.
- `MINUTE` 차트 API는 현재 DB 확정 분봉만 반환한다.

### Node 역할

- KIS WebSocket `H0STCNT0` tick을 수신한다.
- `stock:latest:{code}`에 최신 tick 스냅샷을 저장한다. 현재가/등락률/누적 거래량/체결강도 최신 표시용이며 차트 분봉과 분리한다.
- tick을 같은 `candleTime` 기준으로 1분봉 미확정 캔들로 집계한다.
- `stock:minute-candles:{code}` ZSET에 최근 30~40분 미확정 분봉을 저장한다.
- `stock:history:{code}`와 `stream:stock:ticks`는 Spring 비즈니스 로직 필수 의존성이 아니므로 디버깅/장애 분석/후처리 확장 목적일 때만 유지한다.
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
    "tradeAmount": null,
    "isFinal": false
  }
}
```

### Frontend 역할

- 현재는 Spring 차트 API로 DB 확정 분봉 스냅샷만 로드한다.
- Redis/SSE 연동 이후에는 Node SSE의 `MINUTE_CANDLE_UPDATE`를 추가로 반영한다.

### Redis 분봉 계약

`stock:latest:{code}`는 최신 상태 스냅샷이고, `stock:minute-candles:{code}`는 차트용 분봉 배열이다. 현재가/등락률은 `stock:latest:{code}`에서 읽고, 차트는
`stock:minute-candles:{code}`를 병합한다.

`stock:minute-candles:{code}`:

| Item       | Value                                     |
|------------|-------------------------------------------|
| Redis type | Sorted Set                                |
| Score      | `candleTime` epoch millis 또는 epoch minute |
| Member     | 아래 JSON 문자열                               |
| Retention  | 최근 30~40분                                 |

```json
{
  "stockCode": "005930",
  "candleTime": "2026-05-21T14:20:00",
  "openPrice": 71000,
  "highPrice": 71200,
  "lowPrice": 70900,
  "closePrice": 71100,
  "volume": 32000,
  "tradeAmount": 2275000000,
  "isFinal": false,
  "updatedAt": "2026-05-21T14:20:31"
}
```

---

## 5. 적재 정책

### 과거 데이터 backfill

분봉 과거 데이터는 저장하지 않는다. 현재 분봉 저장 범위는 당일 데이터만이다.

일/주/월/년봉 수동 적재 API는 아직 미구현이다.

예정 경로:

```http
POST /api/admin/chart/backfill?interval=DAY
POST /api/admin/chart/backfill?interval=WEEK
POST /api/admin/chart/backfill?interval=MONTH
POST /api/admin/chart/backfill?interval=YEAR
POST /api/admin/chart/backfill?stockCode=005930&interval=DAY
```

### 당일 분봉 smoke/backfill

분봉은 당일 데이터만 지원한다.

수동 API:

```http
POST /api/local/kis/chart/minute/sync
POST /api/local/kis/chart/minute/sync?stockCode=005930
GET /api/local/kis/chart/minute/sync/status?stockCode=005930
```

용도:

- 구현 직후 특정 종목의 당일 분봉 저장 검증
- 장중 30분 스케줄러와 동일한 저장 로직 수동 실행
- 장중 장애 또는 배치 실패 후 당일 분봉 보정

정책:

- `stockCode`를 지정하면 해당 종목만 실행한다.
- `stockCode`를 생략하면 등록된 전체 종목을 대상으로 실행한다.
- KIS `FHKST03010200`은 한 번에 최대 30건만 반환하므로, 수동 동기화는 필요한 정각/30분 입력 시각만 여러 번 호출한다.
- 종목별로 오늘 저장된 마지막 분봉 다음 시각부터만 증분 동기화한다.
- 수동 동기화도 스케줄러와 동일하게 정각/30분 단위까지만 `isFinal=true`로 저장한다.
- 예: `14:20` 실행이면 DB 확정 저장 대상은 `09:00~14:00`이다.
- 응답의 `kisRequestCount`로 실제 KIS 호출 횟수를 확인한다.
- 상태 확인 API의 `firstCandleTime`, `lastCandleTime`, `dbExpectedCandleCount`, `candleCount`, `hasExpectedCandleCount`,
  `coversExpectedRange`로 DB 적재 범위를 검증한다.
- 상태 확인 API는 `realtimeExpectedStartTime`, `realtimeExpectedEndTime`, `realtimeCheckRequired`를 내려주지만, 현재 Spring은 Redis
  미확정 분봉을 실제 검증하지 않는다.

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

### 일/주/월/년 확정 동기화

KIS 국내주식기간별시세 `FHKST03010100`을 사용한다.

KIS 요청:

| interval | `FID_PERIOD_DIV_CODE` | 1회 최대 건수 | 저장 기준 `candleTime`         |
|----------|-----------------------|----------|----------------------------|
| `DAY`    | `D`                   | 100건     | 영업일 `00:00:00`             |
| `WEEK`   | `W`                   | 100건     | KIS가 반환한 주봉 기준일 `00:00:00` |
| `MONTH`  | `M`                   | 100건     | KIS가 반환한 월봉 기준일 `00:00:00` |
| `YEAR`   | `Y`                   | 100건     | KIS가 반환한 연봉 기준일 `00:00:00` |

저장 매핑:

| KIS output2      | `stock_candles` |
|------------------|-----------------|
| `stck_bsop_date` | `candle_time`   |
| `stck_oprc`      | `open_price`    |
| `stck_hgpr`      | `high_price`    |
| `stck_lwpr`      | `low_price`     |
| `stck_clpr`      | `close_price`   |
| `acml_vol`       | `volume`        |
| `acml_tr_pbmn`   | `trade_amount`  |

초기 backfill:

- 수동 API로 전체 종목 또는 단일 종목을 적재한다.
- `DAY`: 최근 1~2년을 우선 적재한다. KIS 1회 최대 100건이므로 여러 구간으로 나눠 호출한다.
- `WEEK`: 최근 3~5년을 우선 적재한다.
- `MONTH`: 최근 5~10년을 우선 적재한다.
- `YEAR`: 가능한 전체 기간을 적재한다.
- 같은 캔들은 `unique(stock_id, interval_type, candle_time)` 기준으로 upsert한다.

예상 수동 API:

```http
POST /api/local/kis/chart/period/sync?interval=DAY
POST /api/local/kis/chart/period/sync?interval=WEEK
POST /api/local/kis/chart/period/sync?interval=MONTH
POST /api/local/kis/chart/period/sync?interval=YEAR
POST /api/local/kis/chart/period/sync?stockCode=005930&interval=DAY
```

정기 동기화:

- `DAY`: 장 마감 후 충분히 지연된 시각에 당일 일봉을 upsert한다. 예: `16:10` 이후.
- `WEEK`: 매일 장 마감 후 최신 주봉을 upsert해도 되고, 금요일 장 마감 후 실행해도 된다. 단순성을 위해 초기에는 매일 최신 100건 upsert를 허용한다.
- `MONTH`: 매일 장 마감 후 최신 100건 upsert를 허용한다. 호출량을 줄이려면 월말 또는 월초 보정 스케줄로 축소한다.
- `YEAR`: 매일 실행할 필요가 낮다. 월 1회 또는 수동 backfill 중심으로 둔다.

차트 조회:

- `DAY`, `WEEK`, `MONTH`, `YEAR`는 Redis 병합 없이 DB 확정 캔들만 반환한다.
- 프론트 표시 기간이 `1주일`, `1개월`, `1년`, `5년`처럼 UX 기간인 경우에도 백엔드는 interval과 조회 기간만 받는다.

---

## 6. 정합성 정책

- DB 확정 캔들의 기준은 KIS REST 응답이다.
- Redis/SSE 미확정 캔들은 표시용이며 DB 확정 캔들을 대체하지 않는다.
- WebSocket 장애로 Redis/SSE 구간이 비어도 다음 KIS 분봉 동기화에서 DB 확정 캔들이 복구된다.
- Spring 차트 API와 Node SSE는 `candleTime` 기준을 동일하게 사용한다.
- 같은 `candleTime`이 충돌하면 `isFinal=true`인 Spring DB 값을 우선한다.
- 14:25에 `MINUTE` 차트 API를 호출하면 Spring은 DB 확정 캔들에 Redis 미확정 캔들을 병합해 14:25까지의 스냅샷을 반환하고, 14:25 이후 변화는 Node SSE가 전달한다.
- 14:32에 스케줄러가 실행 중일 때 사용자가 `MINUTE` 차트 API를 호출하면, Spring은 그 시점에 DB에 커밋된 확정 캔들까지만 읽고 Redis 미확정 캔들을 병합한다. 스케줄러가 아직 해당 종목의
  `14:30` 확정 캔들을 저장하지 않았다면 Redis의 `14:30` 미확정 캔들을 반환할 수 있고, 저장이 끝난 뒤 재조회하면 DB의 `isFinal=true` 캔들이 우선된다.
- 스케줄러는 종목별 upsert를 짧은 트랜잭션으로 커밋한다. 전체 200종목 동기화가 끝날 때까지 차트 조회를 막지 않는다.
