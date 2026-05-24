# Redis Data Contract

본 문서는 Node 서버가 Redis에 적재하는 최신 실시간 주식/뉴스 데이터 계약을 정리한다.

## Common Policy

- Redis 클라이언트: `ioredis`
- 키 네이밍 규칙: `<domain>:<purpose>:<identifier>`
- TTL 기준 시각: 뉴스 데이터는 KST 자정(`nextMidnightKstEpoch()`) 기준 만료
- 주식 실시간 데이터는 분봉 sliding window와 key TTL을 함께 사용
- 본 문서는 Node 쓰기 계약 기준이며, Spring은 이 계약에 맞춰 읽기 전용으로 연동한다

## Stock Realtime Data

Node WebSocket 서버는 `src/websocket.ts`에서 실시간 체결을 받아 1분 분봉으로 집계한 뒤 Redis에 적재한다.

### Required Keys

| Key                           | Structure  | Purpose               |
|-------------------------------|------------|-----------------------|
| `stock:minute-candles:{code}` | Sorted Set | 최근 40분 미확정 1분봉 저장     |
| `stock:latest:{code}`         | String     | 현재 진행 중인 최신 분봉 스냅샷 저장 |

`stock:latest:{code}`와 `stock:minute-candles:{code}`의 payload는 동일하다. 전자는 단건 조회용 최신 스냅샷이고, 후자는 차트 병합용 최근 분봉 배열이다.

### `stock:minute-candles:{code}` - Sorted Set

| Item           | Value                                             |
|----------------|---------------------------------------------------|
| Write Location | `src/websocket.ts:149` (`recordToRedis`)          |
| Redis Command  | `ZREM` + `ZADD` + `ZREMRANGEBYSCORE` + `EXPIRE`   |
| Score          | `minuteTs` ms epoch, `floor(now / 60000) * 60000` |
| Member         | 분봉 JSON 문자열                                       |
| Sliding Window | 최근 40분 (`CANDLE_WINDOW_MS`)                       |
| Key TTL        | 1시간 (`EXPIRE 3600`)                               |

동작 정책:

- KIS WebSocket 틱(`H0STCNT0`)을 같은 분(`minuteTs`) 기준으로 1분 OHLCV로 집계한다.
- 동일 분 데이터는 member를 `ZREM` 후 `ZADD`로 교체해 최신 OHLC/volume으로 갱신한다.
- `volume`은 누적거래량(`vol`)의 분 내 델타 합계다.

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

| Field      | Type   | Description      |
|------------|--------|------------------|
| `code`     | string | 종목코드             |
| `minuteTs` | number | 분 시작 시각 ms epoch |
| `open`     | number | 해당 분 첫 체결가       |
| `high`     | number | 해당 분 최고가         |
| `low`      | number | 해당 분 최저가         |
| `close`    | number | 해당 분 최신 체결가      |
| `volume`   | number | 해당 분 체결량         |
| `change`   | number | 전일 대비            |
| `rate`     | number | 등락률(%)           |
| `strength` | number | 체결강도             |

### `stock:latest:{code}` - String

| Item           | Value                                                          |
|----------------|----------------------------------------------------------------|
| Write Location | `src/websocket.ts:158`                                         |
| Redis Command  | `SET ... EX`                                                   |
| Value          | 현재 진행 중인 분봉 JSON (`stock:minute-candles:{code}` member와 동일 구조) |
| TTL            | 3일 (`EX 259200`)                                               |
| Current Usage  | SSE 연결 직후 초기 스냅샷, 실시간 최신 상태 조회                                 |

주의:

- `stock:latest:{code}`는 더 이상 틱 원본 스냅샷 전용 계약이 아니라 "현재 진행 중인 최신 분봉 1개" 계약이다.
- 현재가/등락률만 필요해도 payload는 분봉 구조 그대로 읽는다.
- 분봉 차트 구현 시에는 `stock:minute-candles:{code}`를 기준으로 병합하고, `stock:latest:{code}`는 단건 최신 상태 fallback으로 본다.

## News Collection And Analysis

뉴스 파이프라인은 `src/news/redis.ts`에서 Redis를 사용한다.

### `news:dedup:{YYYYMMDD}` - Set

| Item           | Value                  |
|----------------|------------------------|
| Write Location | `src/news/redis.ts:55` |
| Redis Command  | `SADD`, `EXPIREAT`     |
| Member         | 정규화 URL의 SHA1 hex      |
| TTL            | 다음 KST 자정 + 1시간        |

정규화 URL은 `protocol://hostname/pathname` 형식이며 query string과 fragment를 제거한다.

### `news:seq` - String Counter

| Item           | Value                  |
|----------------|------------------------|
| Write Location | `src/news/redis.ts:63` |
| Redis Command  | `INCR`                 |
| Purpose        | 개별 뉴스 ID 발급            |
| TTL            | 없음                     |

### `news:item:{newsId}` - Hash

| Item           | Value                  |
|----------------|------------------------|
| Write Location | `src/news/redis.ts:71` |
| Redis Command  | `HSET`, `EXPIREAT`     |
| TTL            | 다음 KST 자정              |

모든 필드는 문자열로 저장한다.

| Field         | Description         |
|---------------|---------------------|
| `newsId`      | 뉴스 ID               |
| `newUrl`      | 원본 URL, 최대 255자     |
| `title`       | 제목, HTML 제거, 최대 50자 |
| `content`     | 본문, HTML 제거         |
| `publishedAt` | 발행 시각 ms epoch      |
| `keyword`     | 검색 키워드              |
| `fetchedAt`   | 수집 시각 ms epoch      |

### `news:today` - Sorted Set

| Item           | Value                  |
|----------------|------------------------|
| Write Location | `src/news/redis.ts:81` |
| Redis Command  | `ZADD`, `EXPIREAT`     |
| Score          | `publishedAt` ms epoch |
| Member         | `newsId`               |
| TTL            | 다음 KST 자정              |
| Purpose        | 당일 뉴스 시간순 인덱스          |

### `news:analysis:today` - Hash

| Item           | Value                   |
|----------------|-------------------------|
| Write Location | `src/news/redis.ts:122` |
| Redis Command  | `HSET`, `EXPIREAT`      |
| TTL            | 다음 KST 자정               |

모든 필드는 문자열로 저장한다.

| Field            | Logical Type | Description                   |
|------------------|--------------|-------------------------------|
| `baseTime`       | ISO 8601     | 분석 기준 시각                      |
| `analysisResult` | string       | 종합 평가                         |
| `summary`        | string       | 핵심 요약 3-5줄                    |
| `reasoning`      | string       | 분석 논리와 근거                     |
| `keyword`        | string       | 핵심 키워드, 최대 50자                |
| `newsCount`      | integer      | 분석 뉴스 수                       |
| `newsIds`        | JSON array   | `[1,2,3,...]`                 |
| `goodSectors`    | JSON array   | `[{sectorName, reason}, ...]` |
| `badSectors`     | JSON array   | `[{sectorName, reason}, ...]` |

### `stream:news:analysis` - Stream

| Item           | Value                                           |
|----------------|-------------------------------------------------|
| Write Location | `src/news/redis.ts:140`                         |
| Redis Command  | `XADD stream:news:analysis MAXLEN ~ 1000 * ...` |
| Retention      | 약 1,000 entries                                 |

모든 필드는 문자열로 저장한다.

| Field       | Description       |
|-------------|-------------------|
| `baseTime`  | ISO 8601 분석 기준 시각 |
| `newsCount` | 분석 뉴스 수           |
| `keyword`   | 핵심 키워드            |

## Daily Reset

뉴스 Redis 데이터는 `src/news/redis.ts:46`의 `registerResetJob`에서 매일 `00:00 KST`에 초기화한다.

```text
DEL news:today news:analysis:today news:dedup:{yesterday YYYYMMDD}
```

## Chart Integration Note

분봉 차트 구현 시 주의할 점:

- Redis raw 분봉 key는 `stock:minute-candles:{code}`다.
- Redis payload 시간 필드는 `candleTime` 문자열이 아니라 `minuteTs` 숫자 epoch millis다.
- Spring/API 응답 DTO가 `candleTime`, `openPrice`, `closePrice` 같은 도메인 필드를 사용하더라도, Redis 읽기 계층에서 `minuteTs -> candleTime`,
  `open -> openPrice` 식의 매핑이 필요하다.
- DB 확정 캔들(`isFinal=true`)과 Redis 미확정 캔들(`isFinal=false`)은 동일 모델이 아니라 "저장 계층별 표현이 다를 수 있다"는 전제로 설계해야 한다.

## Summary

| Key                           | Command         | Structure  | TTL / Limit           | Write Location          |
|-------------------------------|-----------------|------------|-----------------------|-------------------------|
| `stock:minute-candles:{code}` | `ZREM` + `ZADD` | Sorted Set | 40분 슬라이딩 / key TTL 1h | `src/websocket.ts:149`  |
| `stock:latest:{code}`         | `SET ... EX`    | String     | 3일, 매 틱 갱신            | `src/websocket.ts:158`  |
| `news:dedup:{YYYYMMDD}`       | `SADD`          | Set        | 자정 KST + 1시간          | `src/news/redis.ts:55`  |
| `news:seq`                    | `INCR`          | String     | 없음                    | `src/news/redis.ts:63`  |
| `news:item:{newsId}`          | `HSET`          | Hash       | 자정 KST                | `src/news/redis.ts:71`  |
| `news:today`                  | `ZADD`          | Sorted Set | 자정 KST                | `src/news/redis.ts:81`  |
| `news:analysis:today`         | `HSET`          | Hash       | 자정 KST                | `src/news/redis.ts:122` |
| `stream:news:analysis`        | `XADD`          | Stream     | 약 1,000 entries       | `src/news/redis.ts:140` |
