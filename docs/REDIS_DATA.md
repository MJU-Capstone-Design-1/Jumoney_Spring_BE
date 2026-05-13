# Redis Data Contract

본 문서는 Node 서버가 Redis에 적재하는 실시간 주식 데이터와 뉴스 분석 데이터의 키 구조, 자료구조, 보존 정책, 페이로드 스키마를 정리한다.

## Common Policy

- Redis 클라이언트: `ioredis`
- 키 네이밍 규칙: `<domain>:<purpose>:<identifier>`
- 시간 기준:
  - 뉴스 TTL은 `nextMidnightKstEpoch()` 기준 KST 자정 만료를 사용한다.
  - 주식 실시간 이력은 TTL 대신 score 기반 슬라이딩 윈도우로 관리한다.
- 본 문서는 Node 쓰기 계약을 기준으로 하며, Spring은 이 계약에 맞춰 읽기 전용으로 연동한다.

## Stock Realtime Data

Node WebSocket 서버는 `src/websocket.ts`에서 주식 실시간 데이터를 Redis에 적재한다.

### `stock:history:{code}` - Sorted Set

| Item | Value |
|---|---|
| Write Location | `src/websocket.ts:92` |
| Redis Command | `ZADD`, `ZREMRANGEBYSCORE` |
| Score | `timestamp` ms epoch |
| Member | 아래 JSON 문자열 |
| Retention | 최근 30분 슬라이딩 윈도우 |

```json
{
  "code": "005930",
  "time": "153000",
  "price": 71000,
  "change": 500,
  "rate": 0.71,
  "vol": 12345678,
  "strength": 105.3,
  "timestamp": 1715511600000
}
```

| Field | Type | Description |
|---|---|---|
| `code` | string | 종목코드 |
| `time` | string | 체결 시각 `HHMMSS` |
| `price` | number | 현재가 |
| `change` | number | 전일 대비 |
| `rate` | number | 등락률(%) |
| `vol` | number | 누적 거래량 |
| `strength` | number | 체결강도 |
| `timestamp` | number | ms epoch |

### `stock:latest:{code}` - String

| Item | Value |
|---|---|
| Write Location | `src/websocket.ts:95` |
| Redis Command | `SET` |
| Value | `stock:history:{code}` member와 동일한 JSON 문자열 |
| TTL | 없음 |
| Current Consumer | `src/app.ts:71` SSE 초기 스냅샷 |

### `stream:stock:ticks` - Stream

| Item | Value |
|---|---|
| Write Location | `src/websocket.ts:98` |
| Redis Command | `XADD stream:stock:ticks MAXLEN ~ 300000 * ...` |
| Retention | 약 300,000 entries |

모든 필드는 문자열로 저장한다.

| Field | Description |
|---|---|
| `code` | 종목코드 |
| `price` | 현재가 |
| `change` | 전일 대비 |
| `rate` | 등락률 |
| `vol` | 누적 거래량 |
| `strength` | 체결강도 |
| `time` | 체결 시각 `HHMMSS` |
| `timestamp` | ms epoch |

## News Collection And Analysis

뉴스 파이프라인은 `src/news/*`에서 Redis를 사용한다.

### `news:dedup:{YYYYMMDD}` - Set

| Item | Value |
|---|---|
| Write Location | `src/news/redis.ts:55` |
| Redis Command | `SADD`, `EXPIREAT` |
| Member | 정규화 URL의 SHA1 hex |
| TTL | 다음 KST 자정 + 1시간 |

정규화 URL은 `protocol://hostname/pathname` 형식이며 query string과 fragment를 제거한다.

### `news:seq` - String Counter

| Item | Value |
|---|---|
| Write Location | `src/news/redis.ts:63` |
| Redis Command | `INCR` |
| Purpose | 개별 뉴스 ID 발급 |
| TTL | 없음 |

### `news:item:{newsId}` - Hash

| Item | Value |
|---|---|
| Write Location | `src/news/redis.ts:71` |
| Redis Command | `HSET`, `EXPIREAT` |
| TTL | 다음 KST 자정 |

| Field | Stored Type | Description |
|---|---|---|
| `newsId` | string | 뉴스 ID |
| `newUrl` | string | 원본 URL, 최대 255자 |
| `title` | string | 제목, HTML 제거, 최대 50자 |
| `content` | string | 본문, HTML 제거 |
| `publishedAt` | string | 발행 시각 ms epoch |
| `keyword` | string | 검색 키워드 |
| `fetchedAt` | string | 수집 시각 ms epoch |

### `news:today` - Sorted Set

| Item | Value |
|---|---|
| Write Location | `src/news/redis.ts:81` |
| Redis Command | `ZADD`, `EXPIREAT` |
| Score | `publishedAt` ms epoch |
| Member | `newsId` |
| TTL | 다음 KST 자정 |
| Purpose | 당일 뉴스 시간순 인덱스 |

### `news:analysis:today` - Hash

| Item | Value |
|---|---|
| Write Location | `src/news/redis.ts:122` |
| Redis Command | `HSET`, `EXPIREAT` |
| TTL | 다음 KST 자정 |

| Field | Stored Type | Description |
|---|---|---|
| `baseTime` | ISO 8601 string | 분석 기준 시각 |
| `analysisResult` | string | 종합 평가 |
| `summary` | string | 핵심 요약 3-5줄 |
| `reasoning` | string | 분석 논리와 근거 |
| `keyword` | string | 핵심 키워드, 최대 50자 |
| `newsCount` | string | 분석 뉴스 수 |
| `newsIds` | JSON array string | `[1, 2, 3, ...]` |
| `goodSectors` | JSON array string | `[{sectorName, reason}, ...]` |
| `badSectors` | JSON array string | `[{sectorName, reason}, ...]` |

### `stream:news:analysis` - Stream

| Item | Value |
|---|---|
| Write Location | `src/news/redis.ts:140` |
| Redis Command | `XADD stream:news:analysis MAXLEN ~ 1000 * ...` |
| Retention | 약 1,000 entries |

| Field | Stored Type |
|---|---|
| `baseTime` | string |
| `newsCount` | string |
| `keyword` | string |

## Daily Reset

뉴스 Redis 데이터는 `src/news/redis.ts:46`의 `registerResetJob`에서 매일 `00:00 KST`에 초기화한다.

```text
DEL news:today news:analysis:today news:dedup:{yesterday YYYYMMDD}
```

## Summary

| Key | Command | Structure | TTL / Limit | Write Location |
|---|---|---|---|---|
| `stock:history:{code}` | `ZADD` | Sorted Set | 최근 30분 | `src/websocket.ts:92` |
| `stock:latest:{code}` | `SET` | String | 없음 | `src/websocket.ts:95` |
| `stream:stock:ticks` | `XADD` | Stream | 약 300,000 entries | `src/websocket.ts:98` |
| `news:dedup:{YYYYMMDD}` | `SADD` | Set | 자정 KST + 1시간 | `src/news/redis.ts:55` |
| `news:seq` | `INCR` | String | 없음 | `src/news/redis.ts:63` |
| `news:item:{newsId}` | `HSET` | Hash | 자정 KST | `src/news/redis.ts:71` |
| `news:today` | `ZADD` | Sorted Set | 자정 KST | `src/news/redis.ts:81` |
| `news:analysis:today` | `HSET` | Hash | 자정 KST | `src/news/redis.ts:122` |
| `stream:news:analysis` | `XADD` | Stream | 약 1,000 entries | `src/news/redis.ts:140` |
