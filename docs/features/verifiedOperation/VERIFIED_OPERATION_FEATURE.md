# Verified Operation Feature Specification

본 문서는 추천 검증용 모의 운용 계정 기능의 운용 정책, 초기화 방식, 스케줄 설정, API를 정의합니다.

---

## 1. 기능 목적

- **목적**: 오늘의 호주머니와 거장의 선택 추천 로직을 실제 모의투자 계정에서 지속적으로 검증한다.
- **운용 방식**: 배포 DB에 8개 시스템 계정을 자동 생성하고, 각 계정이 정해진 추천 조건으로 매 영업일 1주씩 거래한다.
- **기반 기능**: 기존 모의투자 `Account`, `Portfolio`, `Order`를 그대로 사용한다.
- **계정 소유자**: `provider = LOCAL`, `providerId = verified-operation:{accountCode}` 형식의 시스템 User를 사용한다.

---

## 2. 계정 초기화

- **설정 파일**: `src/main/resources/data/verified_operation_accounts.json`
- **초기화 위치**: `DataInitializer` 실행 흐름에서 초기 데이터 적재 후 실행한다.
- **초기화 로직**:
    1. JSON에 정의된 8개 계정 설정을 읽는다.
    2. `provider = LOCAL`, `providerId = verified-operation:{accountCode}` 기준으로 User를 조회한다.
    3. User가 없으면 새로 생성한다.
    4. User가 있으면 `nickname`, `serviceNickname`을 JSON의 계정명으로 갱신한다.
    5. 해당 User의 모의투자 계좌가 없으면 기존 `MockInvestmentAccountService`와 동일하게 10,000,000원 계좌와 `DEPOSIT` 주문을 생성한다.
    6. 이미 존재하는 계좌, 보유 종목, 주문 이력은 삭제하거나 초기화하지 않는다.

---

## 3. 운용 계정 목록

### 3.1. 오늘의 호주머니 계정

| accountCode                                   | 계정명                                | 조건                                        |
|-----------------------------------------------|------------------------------------|-------------------------------------------|
| `HOJUMONEY_CAPITAL_PROTECTION_STABILITY_LONG` | 모의 운용 계정 (안정적인 자산 보호 + 매우 낮음 + 장기) | `CAPITAL_PROTECTION`, `STABILITY`, `LONG` |
| `HOJUMONEY_DIVIDEND_INCOME_SAFE_PURSUIT_LONG` | 모의 운용 계정 (배당 수익 + 낮음 + 장기)         | `DIVIDEND_INCOME`, `SAFE_PURSUIT`, `LONG` |
| `HOJUMONEY_STEADY_GROWTH_PROFIT_PURSUIT_MID`  | 모의 운용 계정 (자산의 꾸준한 성장 + 높음 + 중기)    | `STEADY_GROWTH`, `PROFIT_PURSUIT`, `MID`  |
| `HOJUMONEY_CAPITAL_GAIN_AGGRESSIVE_SHORT`     | 모의 운용 계정 (시세 차익 + 매우 높음 + 단기)      | `CAPITAL_GAIN`, `AGGRESSIVE`, `SHORT`     |

### 3.2. 거장의 선택 계정

| accountCode             | 계정명               | 거장               | 조건                                         |
|-------------------------|-------------------|------------------|--------------------------------------------|
| `MASTER_WARREN_BUFFETT` | 모의 운용 계정 (워런 버핏)  | `WARREN_BUFFETT` | `BUFFETT_ROE`, `BUFFETT_PER`               |
| `MASTER_PETER_LYNCH`    | 모의 운용 계정 (피터 린치)  | `PETER_LYNCH`    | `LYNCH_PEG`, `LYNCH_EPS_GROWTH`            |
| `MASTER_RAY_DALIO`      | 모의 운용 계정 (레이 달리오) | `RAY_DALIO`      | `DALIO_DEBT_RATIO`, `DALIO_EARNINGS_YIELD` |
| `MASTER_WILLIAM_ONEIL`  | 모의 운용 계정 (윌리엄 오닐) | `WILLIAM_ONEIL`  | `ONEIL_EPS_GROWTH`, `ONEIL_INST_NET_BUY`   |

---

## 4. 스케줄 설정

### 4.1. 기본 설정

```yaml
verified-operation:
  scheduler:
    enabled: ${VERIFIED_OPERATION_SCHEDULER_ENABLED:false}
    cron: "0 15 9 * * MON-FRI"
    zone-id: Asia/Seoul
```

- **실행 시각**: 매주 월요일부터 금요일까지 09:15 KST
- **휴장일 처리**: `MarketCalendarService.isOpenDay()`가 false를 반환하면 전체 운용을 스킵한다.
- **local 기본값**: `false`
- **prod 기본값**: `true`

### 4.2. `VERIFIED_OPERATION_SCHEDULER_ENABLED`

`VERIFIED_OPERATION_SCHEDULER_ENABLED`는 스케줄러 활성화 여부를 운영 환경에서 덮어쓰기 위한 선택 환경 변수다.

- prod에서는 별도 설정이 없어도 기본적으로 활성화된다.
- local에서는 별도 설정이 없으면 비활성화된다.
- 배포 서버에서 일시적으로 운용을 중지하려면 `VERIFIED_OPERATION_SCHEDULER_ENABLED=false`를 설정한다.
- 로컬에서 수동 검증을 위해 자동 스케줄을 켜려면 `VERIFIED_OPERATION_SCHEDULER_ENABLED=true`를 설정한다.

---

## 5. 매매 정책

### 5.1. 공통 매수 정책

1. 계정별 담당 추천 로직을 실행한다.
2. 추천 결과 중 Top1 종목만 선택한다.
3. 현재가가 없으면 해당 계정 매수를 스킵한다.
4. 현금이 부족하면 해당 계정 매수를 스킵한다.
5. 조건을 만족하면 Top1 종목을 1주 시장가 매수한다.
6. 추천 없음, 현재가 없음, 현금 부족은 실패가 아니라 스킵 로그로 처리한다.

### 5.2. 오늘의 호주머니 매도 정책

오늘의 호주머니 계정은 매수 전 만기 도래 lot을 먼저 전량 매도한다.

| 투자 기간         | 만기 기준       |
|---------------|-------------|
| `ULTRA_SHORT` | 매수 시점 + 1일  |
| `SHORT`       | 매수 시점 + 7일  |
| `MID`         | 매수 시점 + 3개월 |
| `LONG`        | 매수 시점 + 1년  |

- 만기 계산은 calendar 기준이다.
- 만기 매도 후에도 해당 일자의 추천 Top1 매수는 계속 시도한다.

### 5.3. 거장의 선택 매도 정책

- 거장의 선택 계정은 매도나 리밸런싱을 수행하지 않는다.
- 매 영업일 추천 Top1 종목 1주를 누적 매수한다.

---

## 6. Lot 추적 테이블

- **테이블**: `verified_operation_trade_lots`
- **용도**: 오늘의 호주머니 계정의 투자 기간별 만기 매도를 추적한다.
- **주요 필드**:
    - `account_code`: 운용 계정 코드
    - `account_id`: 모의투자 계좌 ID
    - `stock_id`: 종목 ID
    - `buy_order_id`: 매수 주문 ID
    - `sell_order_id`: 매도 주문 ID
    - `quantity`: 최초 매수 수량
    - `remaining_quantity`: 남은 수량
    - `bought_at`: 매수 시각
    - `sell_due_at`: 만기 매도 예정 시각
    - `closed_at`: lot 종료 시각

거장의 선택 계정도 매수 lot은 기록되지만 `sell_due_at`은 `null`이다.

---

## 7. API

모든 API는 로그인된 사용자만 호출할 수 있다.

### 7.1. 계정 목록 조회

```http
GET /api/verified-operations/accounts
```

#### Response Data

```json
{
  "accounts": [
    {
      "accountCode": "HOJUMONEY_CAPITAL_GAIN_AGGRESSIVE_SHORT",
      "accountName": "모의 운용 계정 (시세 차익 + 매우 높음 + 단기)",
      "type": "HOJUMONEY",
      "conditions": [
        {
          "code": "CAPITAL_GAIN",
          "label": "시세 차익"
        },
        {
          "code": "AGGRESSIVE",
          "label": "매우 높음"
        },
        {
          "code": "SHORT",
          "label": "단기(1주일)"
        }
      ],
      "totalAsset": 10000000.0000,
      "totalProfitAmount": 0.0000,
      "totalProfitRate": 0.0000,
      "holdingStockCount": 0,
      "lastTradedAt": null
    }
  ]
}
```

### 7.2. 단일 계정 상세 조회

```http
GET /api/verified-operations/accounts/{accountCode}
```

#### Response Data

```json
{
  "accountCode": "MASTER_WARREN_BUFFETT",
  "accountName": "모의 운용 계정 (워런 버핏)",
  "type": "MASTER_CHOICE",
  "conditions": [
    {
      "code": "BUFFETT_ROE",
      "label": "ROE 15% 이상"
    },
    {
      "code": "BUFFETT_PER",
      "label": "PER 0배 초과 15배 이하"
    }
  ],
  "seedMoney": 10000000.0000,
  "cashBalance": 10000000.0000,
  "totalPurchaseAmount": 0.0000,
  "totalEvaluationAmount": 0.0000,
  "totalAsset": 10000000.0000,
  "totalProfitAmount": 0.0000,
  "totalProfitRate": 0.0000,
  "holdingStockCount": 0,
  "lastTradedAt": null,
  "holdings": [],
  "recentOrders": []
}
```

---

## 8. 수익률 계산

운용 계정 API의 평가 방식은 기존 모의투자 조회 로직과 동일한 기준을 따른다.

1. 보유 종목별 현재가를 `StockCurrentPriceService`로 조회한다.
2. 현재가가 없는 종목의 평가 금액은 0으로 계산한다.
3. 총 평가 금액 = 보유 종목별 `현재가 * 보유 수량` 합계
4. 총 자산 = 예수금 + 총 평가 금액
5. 총 손익 = 총 자산 - 초기 자본금
6. 총 수익률 = `총 손익 / 초기 자본금 * 100`
