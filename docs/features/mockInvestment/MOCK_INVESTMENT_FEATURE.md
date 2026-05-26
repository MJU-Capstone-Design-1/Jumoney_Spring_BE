# Mock Investment Feature Specification

본 문서는 모의투자 기능의 비즈니스 로직을 정의합니다.
---

## 1. 시드머니 지급

- **기능**: 유저가 모의투자 탭에 처음 진입할 경우 초기 자본금(10,000,000원)이 담긴 계좌 생성.
- **사용 테이블**: `Account`
- **로직**:
    1. 현재 로그인한 `userId`로 `Account` 테이블 조회.
    2. 계좌가 존재하지 않을 경우, 신규 `Account` 엔티티 생성.
        - `seedMoney` (초기 자본금) = 10,000,000
        - `cashBalance` (예수금) = 10,000,000
        - `totalPurchaseAmount`, `totalAsset` 등 초기화.
    3. `Order` 테이블에 "초기 자산 지급" 로깅.
        - `stockId` = NULL
        - `orderType` = DEPOSIT
        - `totalExecutionAmount` = 10,000,000
    3. `@Transactional`을 적용하여 무결성 보장.

---

## 2. 관심 분야의 대표 기업 조회

- **기능**: 모의투자 탭 첫 진입 시, 사용자가 선택한 관심 분야(섹터)의 대장주를 1개 추천.
- **사용 테이블**: `Sector`, `Stock`
- **로직**:
    1. 선택된 섹터 ID(`sectorId`)와 대장주 여부(`isMarketLeader = true`) 조건으로 `Stock` 테이블을 조회.
    2. 해당 종목의 기업명(`name`), 종목 코드를 DTO로 묶어 반환.

---

## 3. 모의투자 메인 페이지 (대시보드)

### 3.1. 사용자 계좌 정보 요약

- **기능**: 총 잔고, 총 매입 금액, 총 수익률 등 계좌의 전체 실시간 평가 상태 조회.
- **사용 테이블**: `Account`, `Portfolio`
- **로직**:
    1. `Account` 테이블에서 `cashBalance`(예수금), `totalPurchaseAmount`(총 매입 금액) 조회.
    2. `Portfolio` 테이블에서 보유 종목 리스트와 `quantity`(보유 수량) 조회.
    3. 각 종목의 실시간 현재가를 Redis에서 가져옴 (실패 시 DB(`StockPrice`)에서 해당 종목의 가장 최근 `closePrice` 조회)
    4. **실시간 수익률 연산 (Java Application Level)**:
        - 평가 금액 합계 = ∑ (실시간 현재가 * 보유 수량)
        - 실시간 총 자산 = 평가 금액 합계 + `cashBalance`(예수금)
        - 총 수익률 = ((평가 금액 합계 / `totalPurchaseAmount`) - 1) * 100

### 3.2. 내 보유 종목 리스트

- **기능**: 유저가 현재 보유 중인 개별 종목들의 상세 손익 현황 조회.
- **사용 테이블**: `Portfolio`, `Stock`
- **로직**:
    1. `Portfolio` 테이블에서 보유 종목 리스트, 수량(`quantity`), 평균 매입 단가(`averagePurchasePrice`) 조회.
    2. `Stock` 테이블과 JOIN하여 기업명(`name`), 종목 코드, 태그 등 기본 정보 조회.
    3. 실시간 현재가를 Redis에서 조회. (실패 시 DB(`StockPrice`)에서 해당 종목의 가장 최근 `closePrice` 조회)
    4. **개별 종목 실시간 손익 연산**:
        - 평가 손익 = (실시간 현재가 - `averagePurchasePrice`) * `quantity`
        - 수익률 = ((실시간 현재가 / `averagePurchasePrice`) - 1) * 100

### 3.3. 분야 별 종목 리스트

- **기능**: 특정 분야(섹터)에 속한 종목들의 실시간 시세 및 등락률 리스트 조회.
- **사용 테이블**: `Stock`, `Sector`
- **로직**:
    1. `Stock` 테이블에서 해당 `sectorId`로 종목 리스트, 기업명 등 조회.
    2. 각 종목의 실시간 현재가, 전일 대비 상승/하락 금액, 전일 대비율(등락률)을 Redis에서 조회하여 병합 후 반환.
    3. 실패 시 DB에서 최신 데이터 조회

### 3.4. 종목 상세 조회

- **기능**: 차트를 제외한 종목 기본 정보, 현재 시세, 최신 지표를 종합 조회.
- **Endpoint**: `GET /api/mock-investments/stocks/{stockCode}`
- **사용 테이블**: `Stock`, `Sector`, `StockIndicator`
- **로직**:
    1. `Stock` 테이블에서 `stockCode` 기준으로 종목, 섹터, 설명을 조회한다.
    2. `StockCurrentPriceService`로 `currentPrice`, `changeRate`를 조회한다.
    3. `StockIndicator` 테이블에서 최신 `baseTime` 기준 지표 1건을 조회한다.
    4. 태그는 `sector`, `isMarketLeader` 기준으로 구성한다.
    5. 최신 지표가 없으면 지표 필드는 `null`로 반환한다.

#### Response Data

```json
{
  "stockId": 1,
  "stockCode": "005930",
  "stockName": "삼성전자",
  "sector": "IT_SEMICONDUCTOR",
  "isMarketLeader": true,
  "tags": [
    "IT_SEMICONDUCTOR",
    "MARKET_LEADER"
  ],
  "price": {
    "currentPrice": 73500,
    "changeRate": 1.66,
    "marketCap": 438000000000000,
    "accumulatedTradeAmount": 845000000000
  },
  "investmentMetrics": {
    "pbr": 1.45,
    "per": 18.2,
    "roe": 12.8,
    "dividendYield": 2.15,
    "payoutRatio": 35.9,
    "executionStrength": 121.4,
    "instNetBuy20Days": 1523000
  },
  "financialMetrics": {
    "sales": 279600000000000,
    "operatingProfit": 6540000000000,
    "debtRatio": 24.1
  },
  "description": [
    "메모리 반도체와 스마트폰 사업을 하는 대표 기업이에요."
  ]
}
```

### 3.5. 종목 차트 조회

- **기능**: 종목의 차트 데이터를 기간 기준으로 조회.
- **현재 Endpoint**: `GET /api/mock-investments/stocks/{stockCode}/chart?period={period}&date={yyyy-MM-dd}`
- **기간 UX**: `1일`, `1주`, `3달`, `1년`, `5년`
- **period enum**: `ONE_DAY`, `ONE_WEEK`, `THREE_MONTHS`, `ONE_YEAR`, `FIVE_YEARS`
- **사용 테이블**: `Stock`, `StockCandle`
- **로직**:
    1. 단일 차트 API가 기간별 차트 조회를 담당한다.
    2. 기간별 봉 매핑은 `ONE_DAY -> 1분봉`, `ONE_WEEK -> 30분봉`, `THREE_MONTHS -> 일봉`, `ONE_YEAR -> 일봉`, `FIVE_YEARS -> 주봉`을 기준으로
       한다.
    3. `date`를 생략하면 KST 기준 오늘이 개장일이면 오늘, 아니면 직전 개장일로 보정한다.
    4. `ONE_DAY`는 DB 확정 1분봉을 조회하고, 조회 기준일이 오늘이면 Redis 미확정 1분봉을 추가 병합한다.
    5. `ONE_WEEK`는 DB 확정 30분봉을 조회하고, 조회 기준일이 오늘이면 Redis 1분봉으로 마지막 진행 중 30분봉 1개를 집계해 병합한다.
    6. `THREE_MONTHS`, `ONE_YEAR`, `FIVE_YEARS`는 실시간 보강 없이 DB 확정 봉만 반환한다.

#### 수동 동기화 API

```http
POST /api/local/kis/chart/sync
POST /api/local/kis/chart/sync?period=ONE_WEEK
POST /api/local/kis/chart/sync?stockCode=005930
POST /api/local/kis/chart/sync/range?period=ONE_WEEK&fromDate=2026-05-18&toDate=2026-05-22&stockCode=005930
GET /api/local/kis/chart/sync/status?stockCode=005930
GET /api/local/kis/chart/sync/status?stockCode=005930&period=ONE_WEEK
```

- `period` 생략 시 `ONE_DAY`, `ONE_WEEK`, `THREE_MONTHS`, `ONE_YEAR`, `FIVE_YEARS`에 필요한 원천 캔들을 한 번에 동기화한다.
- `ONE_DAY`/`ONE_WEEK`는 1분봉 동기화를 실행하고, 완성된 1분봉 버킷으로 30분봉을 함께 집계한다.
- `THREE_MONTHS`/`ONE_YEAR`는 `DAY`, `FIVE_YEARS`는 `WEEK` 기간봉을 동기화한다.
- 특정 기간만 보정할 때는 `/chart/sync/range`에 `period`, `fromDate`, `toDate`, `stockCode`를 지정한다.
- `date` 생략 시 KST 기준 오늘이 개장일이면 오늘, 아니면 직전 개장일을 기준으로 한다.
- 상태 확인 API는 `period`를 생략하면 전체 차트 기간의 DB 캔들 범위와 건수를 반환한다.

#### Response Data

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

---

## 4. 시장가 매매 주문 (BUY / SELL)

### 4.1. 시장가 매수 (BUY)

- **기능**: 호출 시점의 현재가로 주식을 즉시 매수.
- **로직**:
    1. **가격 확인**: Redis(또는 DB)에서 해당 종목의 실시간 현재가 조회.
    2. **잔액 검증**: `Account`의 `cashBalance` >= (현재가 * 주문 수량) 여부 확인. 부족 시 예외 발생.
    3. **주문 기록**: `Order` 테이블에 BUY 타입으로 로그 생성.
    4. **포트폴리오 갱신**:
        - 기존 보유 종목인 경우: `quantity` 증가, `averagePurchasePrice` 재계산.
        - 신규 종목인 경우: `Portfolio` 레코드 신규 생성.
    5. **계좌 갱신**: `Account`의 `cashBalance` 차감, `totalPurchaseAmount` 증가.

### 4.2. 시장가 매도 (SELL)

- **기능**: 호출 시점의 현재가로 주식을 즉시 매도.
- **로직**:
    1. **보유 검증**: `Portfolio` 테이블에서 해당 종목의 보유 수량 확인. 주문 수량이 보유량보다 많으면 예외 발생.
    2. **가격 확인**: 실시간 현재가 조회.
    3. **주문 기록**: `Order` 테이블에 SELL 타입으로 로그 생성.
    4. **포트폴리오 갱신**:
        - `quantity` 차감. 수량이 0이 될 경우 레코드 비활성화 처리(`isActive = false`) 또는 삭제.
    5. **계좌 갱신**:
        - `cashBalance` 증가 (매도 금액만큼).
        - `totalPurchaseAmount` 감소: `기존 총 매입액 * (매도 수량 / 매도 전 보유 수량)` 비율로 차감하여 수익률 계산 로직 유지.

---

## KIS API 연동 명세 (데이터 수집용)

Spring Boot 서버는 실시간 시세가 필요할 때 우선적으로 **Redis**를 조회하며, 캐시가 비어있을 경우에만 아래의 API를 호출하여 데이터를 보정(Cache-Aside)한다.

**1. [국내주식] 시세분석 - 관심종목(멀티종목) 시세조회 (REST)**

- **tr_id**: `FHKST11300006`
- **용도**: 장 외 시간이거나, 페이지 진입 시 Redis에 특정 종목의 현재가 캐시가 누락되어 있을 경우 호출 (최대 30종목 일괄 조회 가능).
- **요청 데이터 예시**:
    - `FID_COND_MRKT_DIV_CODE_1` ~ `30`: 조건 시장 분류 코드 (코스피: J)
    - `FID_INPUT_ISCD_1` ~ `30`: 입력 종목코드 (예: `005930`)
- **응답 데이터 매핑 예시**:
    - `inter_shrn_iscd` → 종목코드 (`stockCode`)
    - `inter2_prpr` → 실시간 현재가
    - `inter2_prdy_vrss` → 전일 대비 (상승/하락 금액)
    - `prdy_ctrt` → 전일 대비율 (등락률)
