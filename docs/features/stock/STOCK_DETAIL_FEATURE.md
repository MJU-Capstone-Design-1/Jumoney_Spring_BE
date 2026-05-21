# Stock Detail & Chart Specification

본 문서는 종목 상세 화면의 차트 데이터 제공 정책을 정의한다.

상세 지표 조회는 모의투자 API 문서(`docs/features/mockInvestment/MOCK_INVESTMENT_FEATURE.md`)의
`GET /api/mock-investments/stocks/{stockCode}`를 기준으로 한다.

---

## 1. 차트 API 방향

차트는 프론트 기간 버튼이 아니라 KIS가 제공하는 봉 단위를 그대로 노출한다. 초기 구현의 복잡도를 낮추고, KIS REST 응답과 DB 저장 단위를 1:1로 맞추기 위함이다.

### Endpoint

```http
GET /api/mock-investments/stocks/{stockCode}/chart?interval=MINUTE
GET /api/mock-investments/stocks/{stockCode}/chart?interval=DAY
GET /api/mock-investments/stocks/{stockCode}/chart?interval=WEEK
GET /api/mock-investments/stocks/{stockCode}/chart?interval=MONTH
GET /api/mock-investments/stocks/{stockCode}/chart?interval=YEAR
```

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

- `MINUTE`: DB 확정 분봉과 Redis 최근 미확정 분봉을 병합해 반환한다.
- `DAY`, `WEEK`, `MONTH`, `YEAR`: DB 확정 캔들만 반환한다.
- Node SSE는 Spring 응답 이후의 실시간 미확정 분봉 업데이트를 전달한다.

```json
{
  "stockCode": "005930",
  "interval": "MINUTE",
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
      "candleTime": "2026-05-21T14:25:00",
      "openPrice": 73800,
      "highPrice": 73900,
      "lowPrice": 73700,
      "closePrice": 73850,
      "volume": 32000,
      "tradeAmount": null,
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
| `tradeAmount` | 거래대금. KIS 응답에 없거나 초기 구현에서 제외하면 `null` 허용          |
| `isFinal`     | DB/KIS 확정 캔들은 `true`, Redis 기반 미확정 분봉은 `false`       |

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

분봉 DB 데이터는 KIS REST를 확정 기준으로 삼는다. Redis 최근 실시간 데이터는 Spring의 초기 스냅샷과 Node SSE의 실시간 업데이트에 사용한다.

### Spring 역할

- 장중 30분마다 KIS `FHKST03010200`을 호출한다.
- KIS가 제공하는 최근 당일 분봉을 `stock_candles(interval_type=MINUTE)`에 upsert한다.
- 요청 시각 기준 최근 2분은 확정 저장 대상에서 제외한다. KIS 응답의 가장 최근 분봉은 첫 체결 전 이전 분 거래량이 보일 수 있고, 호출 지연 때문에 아직 완성되지 않은 봉일 수 있기 때문이다.
- `MINUTE` 차트 API는 DB 확정 분봉과 Redis 최근 미확정 분봉을 병합해 반환한다.
- Redis 미확정 분봉은 `isFinal=false`로 반환한다.
- 같은 `candleTime`에 DB 확정 분봉과 Redis 미확정 분봉이 모두 있으면 DB 확정 분봉을 우선한다.

### Node 역할

- KIS WebSocket `H0STCNT0` tick을 수신한다.
- tick을 같은 `candleTime` 기준으로 1분봉 미확정 캔들로 집계한다.
- Redis에 최근 미확정 분봉을 저장하거나, Spring이 집계할 수 있는 최근 tick 이력을 유지한다.
- SSE로 Spring 초기 응답 이후의 캔들 업데이트 이벤트를 보낸다.

프론트가 tick 원본으로 직접 1분봉을 만드는 것도 가능하지만, 권장하지 않는다. 이유는 다음과 같다.

- 여러 화면/플랫폼에서 동일한 OHLC 계산 규칙을 중복 구현하게 된다.
- 거래량 계산, 같은 분 업데이트, 순서 역전, 중복 이벤트 처리를 프론트마다 맞춰야 한다.
- Node가 이미 실시간 스트림의 소유자이므로, 1분봉 미확정 캔들 생성 책임도 Node에 두는 편이 일관적이다.

따라서 SSE 이벤트는 가능하면 아래처럼 캔들 업데이트 형태로 제공한다.

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

- 최초 진입 시 Spring 차트 API로 현재 시점까지의 차트 스냅샷을 로드한다.
- Node SSE의 `MINUTE_CANDLE_UPDATE`로 마지막 미확정 캔들을 append/update한다.
- 같은 `candleTime`의 Spring 확정 캔들이 재조회되면 Spring 값을 우선한다.
- `isFinal=false` 캔들은 표시용 임시 데이터로만 다룬다.

---

## 5. 적재 정책

### 과거 데이터 backfill

과거 데이터는 수동 배치 API로 적재한다.

대상:

- `DAY`
- `WEEK`
- `MONTH`
- `YEAR`

분봉 과거 데이터는 KIS 제공 범위가 제한적이므로 초기 구현에서는 당일 분봉만 저장한다.

예상 API:

```http
POST /api/admin/chart/backfill?interval=DAY
POST /api/admin/chart/backfill?interval=WEEK
POST /api/admin/chart/backfill?interval=MONTH
POST /api/admin/chart/backfill?interval=YEAR
POST /api/admin/chart/backfill?stockCode=005930&interval=DAY
```

### 당일 분봉 smoke/backfill

분봉은 당일 데이터만 우선 지원한다. 구현 당일이 장 마감 이후여도 KIS `FHKST03010200`이 당일 분봉을 제공하는 시간대라면 수동 API로 DB를 채울 수 있어야 한다.

예상 API:

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
- KIS `FHKST03010200`은 한 번에 최대 30건만 반환하므로, 수동 동기화는 `15:30`, `15:00`, `14:30`처럼 정각/30분 입력 시각만 내려가며 여러 번 호출한다.
- 수동 동기화도 스케줄러와 동일하게 정각/30분 단위까지만 `isFinal=true`로 저장한다.
- 예: `14:20` 실행이면 DB 확정 저장 대상은 `09:00~14:00`이고, `14:01~14:20`은 Redis/Node SSE의 `isFinal=false` 미확정 캔들 검증 대상이다.
- 응답의 `kisRequestCount`로 실제 KIS 호출 횟수를 확인한다.
- 상태 확인 API의 `firstCandleTime`, `lastCandleTime`, `dbExpectedCandleCount`, `candleCount`, `hasExpectedCandleCount`, `coversExpectedRange`로 DB 적재 범위를 검증한다.
- 상태 확인 API는 `realtimeExpectedStartTime`, `realtimeExpectedEndTime`, `realtimeCheckRequired`로 Redis 미확정 구간도 알려준다. 단, Spring에서 실제 Redis 분봉 검증까지 하려면 Node가 저장하는 미확정 분봉 Redis key 규격을 먼저 확정해야 한다.

장 마감 이후 KIS가 당일 분봉을 더 이상 제공하지 않으면 수동 API는 실패 결과를 반환하고, 다음 개장일 장중에 다시 검증한다.

### 장중 분봉 동기화

- KOSPI 200 전체 종목을 대상으로 30분마다 실행한다.
- KIS REST rate limiter를 적용한다.
- 실패 종목은 로그와 결과 응답에 남기고 다음 종목을 계속 처리한다.
- 같은 캔들은 unique key 기준 upsert한다.
- 스케줄 실행 시각은 정각/30분 정각보다 정확히 2분 늦게 둔다. 예: `09:32`, `10:02`, `10:32`.
- 저장 cutoff도 요청 시각 기준 최근 2분을 제외한다. 예: `14:32` 실행이면 `14:30`까지 확정 저장한다.
- 수동 smoke 동기화도 같은 기준을 사용한다. 따라서 `14:20` 수동 실행은 `14:00`까지만 DB에 저장하고, `14:01` 이후는 Redis 미확정 구간으로 둔다.

부하 추정:

```text
30분마다 200 requests
1시간 400 requests
장중 6.5시간 기준 하루 약 2,600 requests
250ms 간격 제한 시 200종목 한 바퀴 최소 약 50초
```

30분 주기 안에 처리 가능한 수준으로 본다. 실제 구현에서는 API 응답 시간, DB upsert 시간, 실패 재시도 정책을 포함해 2분 내 완료를 목표로 한다.

### 일/주/월/년 확정 동기화

- 일봉은 장 마감 후 스케줄러로 당일 확정 데이터를 저장한다.
- 주봉/월봉/연봉은 매일 또는 주기적으로 최신 데이터를 upsert해도 된다.
- 과거 구간은 수동 backfill로 먼저 채운다.

---

## 6. 정합성 정책

- DB 확정 캔들의 기준은 KIS REST 응답이다.
- Redis/SSE 미확정 캔들은 표시용이며 DB 확정 캔들을 대체하지 않는다.
- WebSocket 장애로 Redis/SSE 구간이 비어도 다음 KIS 분봉 동기화에서 DB 확정 캔들이 복구된다.
- Spring 차트 API와 Node SSE는 `candleTime` 기준을 동일하게 사용한다.
- 같은 `candleTime`이 충돌하면 `isFinal=true`인 Spring DB 값을 우선한다.
- 14:25에 `MINUTE` 차트 API를 호출하면 Spring은 DB 확정 캔들에 Redis 미확정 캔들을 병합해 14:25까지의 스냅샷을 반환하고, 14:25 이후 변화는 Node SSE가 전달한다.
- 14:32에 스케줄러가 실행 중일 때 사용자가 `MINUTE` 차트 API를 호출하면, Spring은 그 시점에 DB에 커밋된 확정 캔들까지만 읽고 Redis 미확정 캔들을 병합한다. 스케줄러가 아직 해당 종목의 `14:30` 확정 캔들을 저장하지 않았다면 Redis의 `14:30` 미확정 캔들을 반환할 수 있고, 저장이 끝난 뒤 재조회하면 DB의 `isFinal=true` 캔들이 우선된다.
- 스케줄러는 종목별 upsert를 짧은 트랜잭션으로 커밋한다. 전체 200종목 동기화가 끝날 때까지 차트 조회를 막지 않는다.
