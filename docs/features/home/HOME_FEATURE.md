# Home Feature Specification

본 문서는 홈(Main) 화면에 노출되는 4가지 핵심 기능의 비즈니스 로직과 API/DB 연동 명세를 정의합니다.

---

## 1. 실시간 뉴스 (Real-time News)
- **기능**: 시스템에 적재된 최신 뉴스 중 핵심 뉴스 1개의 본문 및 요약을 조회.
- **사용 테이블**: `News`, `NewsAnalysis`
- **로직**:
    1. `NewsAnalysis` 테이블에서 분석 결과와 매핑된 `News` 데이터 중 랜덤으로 1건 조회.

---

## 2. 모의투자 랭킹 (Mock Investment Ranking)
- **기능**: 전체 사용자 및 소속 팀별 랭킹 상위 5명과, 각 랭커의 투자 비중 Top 3 종목 조회.
- **사용 테이블**: `UserRanking`, `User`, `Portfolio`, `Stock`
- **핵심 로직 (1시간 주기 스케줄러)**:
    1. **현재가 수집**: 유저들이 보유한 종목들의 최신 가격을 KIS 다건조회 API(`FHKST11300006`)를 통해 일괄 수집.
    2. **자산 평가**: `Account`의 예수금과 `Portfolio`의 보유 종목 현재가를 합산하여 총 자산 및 수익률 계산.
    3. **순위 저장 (DB)**: 계산된 수익률을 기준으로 전체 순위 및 팀 내 순위를 정렬하여 `UserRanking` 테이블의 `overallRank`, `groupRank`, `profitRate` 필드를 일괄 `UPDATE`.
    4. **랭커 정보 조회 (API 응답 시)**:
        - `UserRanking` 테이블에서 전체 순위(overallRank) 기준 오름차순으로 상위 5명(LIMIT 5)의 데이터 조회.
        - 해당 유저의 `Portfolio` 테이블에서 `totalPurchaseAmount`(총 매수 금액) 내림차순으로 Top 3 `stockId` 추출.
        - DB(`Stock`)에서 기업명 등을 매핑하여 프론트로 반환.

---

## 3. 오늘의 추천 용어 (Today's Stock Term)
- **기능**: 주식 초보자를 위한 오늘의 주식 용어 1건 노출.
- **사용 테이블**: `StockTerm`
- **로직**:
    1. DB에서 매일 1개의 용어를 선정.
    2. (예시) `오늘 날짜의 일(Day) 값 % 전체 용어 개수` 로직 등을 활용하여 매일 자정(00:00)을 기준으로 용어명과 상세 설명 반환.

---

## 4. 모의투자 계좌 요약 및 대표 종목 차트
- **기능**: 로그인한 사용자의 총 매수금, 총 평가손익, 총 수익률과 대표 보유 종목 1건, 그리고 해당 종목의 1일 차트를 홈에서 노출.
- **현재 Endpoint**
    - `GET /api/home/mock-investment-summary`
    - `GET /api/home/mock-investment-summary/chart`
- **사용 테이블**: `Account`, `Portfolio`, `Order`, `Stock`, `StockCandle`
- **연동 서비스**: `StockCurrentPriceService`, `MockInvestmentQueryService`

### 4.1. 계좌 요약 조회
- **기능**: 홈 카드의 숫자/텍스트 영역을 우선 렌더링하기 위한 경량 요약 API.
- **로직**:
    1. `Account` 테이블에서 로그인 사용자의 모의투자 계좌 존재 여부를 확인한다.
    2. 계좌가 없으면 자동 생성하지 않고 `hasAccount=false`와 `null` 데이터로 응답한다.
    3. 계좌가 있으면 `Portfolio` 목록을 조회한다.
    4. 각 보유 종목의 현재가는 `StockCurrentPriceService`로 조회한다.
    5. 현재가 조회가 실패하면 `StockCandle`의 최신 `DAY` 확정 봉 종가로 폴백한다.
    6. 끝까지 가격을 확인할 수 없는 종목이 하나라도 있으면 총 손익/총 수익률은 잘못된 손실 값으로 계산하지 않고 `null`로 반환한다.
    7. 총 손익은 `총 평가손익` 기준으로 계산한다.
        - `총 평가손익 = Σ(현재가 * 보유수량) - totalPurchaseAmount`
        - `총 수익률 = 총 평가손익 / totalPurchaseAmount * 100`

### 4.2. 대표 종목 선정
- **기능**: 홈 카드에 노출할 대표 보유 종목 1건을 결정한다.
- **선정 기준**:
    1. `Portfolio.totalPurchaseAmount` 내림차순
    2. 동률이면 더 먼저 매수한 종목 우선
    3. 최종 동률이면 `stockId` 오름차순
- **구현 메모**:
    - “먼저 매수한 종목” 기준은 `Order` 테이블의 `BUY` 주문 이력 중 최초 `executedAt`으로 판단한다.
    - 홈 응답 성능을 위해 종목별로 주문을 반복 조회하지 않고, 계좌의 `BUY` 주문을 한 번에 읽어 tie-breaker 계산에 사용한다.

### 4.3. 대표 종목 차트 조회
- **기능**: 대표 종목의 홈 전용 1일 OHLC 차트를 지연 로딩한다.
- **로직**:
    1. 계좌가 없으면 `hasAccount=false`, 빈 차트를 반환한다.
    2. 대표 종목이 없으면 `hasAccount=true`, 종목 메타는 `null`, 차트는 빈 배열로 반환한다.
    3. 차트 원천 조회는 기존 `MockInvestmentQueryService.getChart(stockCode, ONE_DAY, null)`를 재사용한다.
    4. 홈 응답에는 `candleTime`, `openPrice`, `highPrice`, `lowPrice`, `closePrice`만 포함한다.
    5. 차트 조회 중 예외가 발생하면 홈 API 전체를 실패시키지 않고 빈 차트를 반환한다.

### 4.4. 기준일 보정 규칙
- `date`를 직접 받지 않는 홈 차트와 `GET /api/mock-investments/stocks/{stockCode}/chart`의 기본 기준일 보정 규칙은 동일하다.
- `requestedDate`가 없을 때:
    - 오늘이 개장일이고 현재 시각이 `09:00` 이상이면 오늘 기준
    - 그 외에는 직전 개장일 기준
- 따라서 장 시작 전(예: 새벽 1시)에는 당일이 아닌 최근 장 열린 날짜의 1일 차트를 보여준다.

### 4.5. 홈 로딩 전략
- 홈 모의투자 영역은 무거운 차트 응답을 초기 응답에 합치지 않고 `summary -> chart` 순으로 분리 로딩한다.
- 권장 호출 순서:
    1. 오늘의 주식용어, 랭킹 등 가벼운 홈 API 호출
    2. `GET /api/home/mock-investment-summary` 호출
    3. `hasAccount=true`이고 대표 종목이 있을 때만 `GET /api/home/mock-investment-summary/chart` 호출
- 이 구조로 계좌가 없는 사용자는 차트 API를 아예 호출하지 않아도 되고, 홈 첫 렌더 지연을 줄일 수 있다.

#### Summary Response Example

```json
{
  "hasAccount": true,
  "totalPurchaseAmount": 1250000.0000,
  "totalProfitAmount": 84250.0000,
  "totalProfitRate": 6.7400,
  "topHolding": {
    "stockId": 1,
    "stockCode": "005930",
    "stockName": "삼성전자",
    "purchaseAmount": 700000.0000,
    "profitAmount": 42000.0000,
    "profitRate": 6.0000
  }
}
```

#### Chart Response Example

```json
{
  "hasAccount": true,
  "stockId": 1,
  "stockCode": "005930",
  "stockName": "삼성전자",
  "date": "2026-05-27",
  "includesRealtime": false,
  "candles": [
    {
      "candleTime": "2026-05-27T09:00:00",
      "openPrice": 71000.0000,
      "highPrice": 71200.0000,
      "lowPrice": 70900.0000,
      "closePrice": 71100.0000
    }
  ]
}
```

---

## KIS API 연동 명세 (데이터 수집용)

Spring Boot 서버가 배치(Batch) 작업 시 내부 DB 갱신을 위해 호출하는 REST API 명세. (웹소켓 데이터 처리는 분리된 Node.js 서버에서 담당하므로 생략함)
구현 시에는 kis_api 폴더의 실제 api 명세서 마크다운 파일을 참고할 것.

**1. [국내주식] 시세분석 - 관심종목 시세조회 (REST)**
- **tr_id**: `FHKST11300006`
- **용도**: 1시간 단위 랭킹 갱신 스케줄러 구동 시, 유저들이 보유한 다수의 종목 현재가를 한 번에 빠르게 수집하여 평가 자산을 계산.
- **요청 데이터**:
    - 다건의 종목코드를 묶어서 요청
- **응답 데이터 매핑 예시**:
    - `inter_shrn_iscd` → `stockCode`
    - `inter2_prpr` → 종목 현재가
    - `prdy_ctrt` → 전일 대비율

**2. [국내주식] 기본시세 - 국내주식기간별시세(일/주/월/년) (REST)**
- **tr_id**: `FHKST03010100`
- **용도**: 매일 장 마감 후 해당 일자의 최종 일봉(DAY) 데이터를 `StockPrice` 테이블에 Batch Insert 하기 위해 사용.
- **요청 데이터 예시**:
    - `FID_COND_MRKT_DIV_CODE`: `J` (코스피 등 주식시장)
    - `FID_INPUT_ISCD`: 종목코드
    - `FID_PERIOD_DIV_CODE`: `D` (일봉)
    - `FID_ORG_ADJS_PRC`: `1` (수정주가 반영)
- **응답 데이터 매핑 예시**:
    - `stck_bsop_date` → `baseTime` (기준 일자, YYYYMMDD)
    - `stck_clpr` → `closePrice` (종가)
