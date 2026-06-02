# Label Mappings

백엔드 API 응답으로 내려주는 enum/key의 프론트 표시용 한국어 매핑입니다.

## 원칙

- API 응답은 화면 표시 문자열 대신 안정적인 enum/key를 내려준다.
- 프론트는 이 문서의 매핑으로 화면 표시 문자열을 만든다.
- 추천 API의 `tags`, `goodSectorTags`, `sortMetricKey`, `investmentPurpose`, `riskProfile`, `investmentHorizon`은 모두 아래 key를 기준으로 표시한다.
- `goodSectorTags`는 `IT_SEMICONDUCTOR` 같은 `SectorType` enum 코드명으로 내려온다. Redis 뉴스 데이터의 `sectorName`은 한국어 섹터명일 수 있지만, API 응답에서는 enum 코드명으로 변환한다.

```ts
export const labelMappings = {
  INVESTMENT_PURPOSE: "투자 목적",
  RISK_PROFILE: "위험 감수 성향",
  INVESTMENT_HORIZON: "투자 기간",

  CAPITAL_PROTECTION: "안정적인 자산 보호",
  DIVIDEND_INCOME: "배당 수익",
  STEADY_GROWTH: "자산의 꾸준한 성장",
  CAPITAL_GAIN: "시세 차익",

  STABILITY: "매우 낮음",
  SAFE_PURSUIT: "낮음",
  PROFIT_PURSUIT: "높음",
  AGGRESSIVE: "매우 높음",

  ULTRA_SHORT: "초단기(1일)",
  SHORT: "단기(1주일)",
  MID: "중기(3달)",
  LONG: "장기(1년)",

  EXECUTION_STRENGTH: "체결강도",
  ACCUMULATED_TRADE_AMOUNT: "거래대금",
  EPS_GROWTH_RATE: "EPS 성장률",
  ROE: "ROE",

  IT_SEMICONDUCTOR: "IT/반도체",
  AUTOMOBILE_TRANSPORT: "자동차/운송",
  ENERGY_CHEMISTRY: "에너지/화학",
  BIO_HEALTHCARE: "바이오/헬스케어",
  SHIPBUILDING_MACHINERY: "조선/기계",
  FINANCE: "금융",
  COMMUNICATION: "커뮤니케이션",
  STEEL_MATERIALS: "철강/소재",
  CONSTRUCTION_UTILITY: "건설/유틸리티",
  ESSENTIAL_CONSUMER: "필수소비재",
  MARKET_LEADER: "대장주",

  NAME_ASC: "이름 순",
  PRICE_DESC: "주가 높은 순",
  PRICE_ASC: "주가 낮은 순",
  MARKET_CAP_DESC: "시가총액 순",
  TRADE_AMOUNT_DESC: "누적 거래대금 순",

  INITIAL_SELECTION: "처음 선택",
  CHANGED_SELECTION: "다른 거장에서 변경",

  MASTER409_ALREADY_SELECTED: "이미 선택한 거장입니다.",
} as const;

export const recommendationTagLabels = {
  CAPITAL_PROTECTION: "안정적인 자산 보호",
  DIVIDEND_INCOME: "배당 수익",
  STEADY_GROWTH: "자산의 꾸준한 성장",
  CAPITAL_GAIN: "시세 차익",

  STABILITY: "위험도 매우 낮음",
  SAFE_PURSUIT: "위험도 낮음",
  PROFIT_PURSUIT: "위험도 높음",
  AGGRESSIVE: "위험도 매우 높음",
} as const;

export const masterCodeLabels = {
  WARREN_BUFFETT: "워런 버핏",
  PETER_LYNCH: "피터 린치",
  RAY_DALIO: "레이 달리오",
  WILLIAM_ONEIL: "윌리엄 오닐",
} as const;

export const masterOptionLabels = {
  BUFFETT_ROE: "ROE 15% 이상",
  BUFFETT_PER: "PER 0배 초과 15배 이하",
  BUFFETT_EPS_GROWTH: "EPS 성장률 10% 이상",
  BUFFETT_DEBT_RATIO: "부채비율 100% 이하",
  BUFFETT_OPERATING_MARGIN: "영업이익률 20% 이상",

  LYNCH_PEG: "PEG 1.0 이하",
  LYNCH_EPS_GROWTH: "EPS 성장률 20% 이상 50% 이하",
  LYNCH_DEBT_RATIO: "부채비율 100% 이하",
  LYNCH_SALES_GROWTH: "매출액 증가율 10% 이상",
  LYNCH_SECTOR: "섹터 선택",

  DALIO_ALL_WEATHER: "올웨더 포트폴리오",
  DALIO_PER: "PER 20배 이하",
  DALIO_MARGIN_DEBT: "신용잔고율 5% 이하",
  DALIO_DEBT_RATIO: "부채비율 50% 이하",
  DALIO_EARNINGS_YIELD: "이익수익률 3.38% 이상",

  ONEIL_EPS_GROWTH: "EPS 성장률 25% 이상",
  ONEIL_ROE: "ROE 17% 이상",
  ONEIL_HIGH_52_WEEK: "52주 신고가 갱신 또는 10% 근접",
  ONEIL_MARKET_LEADER: "대장주 여부",
  ONEIL_INST_NET_BUY: "최근 20거래일 기관 순매수 합계 0 이상",
} as const;

export const masterSortMetricLabels = {
  ROE: "ROE",
  PEG: "PEG",
  SALES_GROWTH_RATE: "매출액 증가율",
  MARKET_CAP: "시가총액",
  HIGH_52_WEEK_RATE: "52주 신고가 대비 현재가 비율",
} as const;
```

## 오늘의 호주머니 추천 응답 key

- `investmentPurpose`: `CAPITAL_PROTECTION`, `DIVIDEND_INCOME`, `STEADY_GROWTH`, `CAPITAL_GAIN`
- `riskProfile`: `STABILITY`, `SAFE_PURSUIT`, `PROFIT_PURSUIT`, `AGGRESSIVE`
- `investmentHorizon`: `ULTRA_SHORT`, `SHORT`, `MID`, `LONG`
- `recommendations[].tags`: 투자 목적 또는 위험 감수 성향 key
- `recommendations[].goodSectorTags`: `SectorType` key
- `recommendations[].sortMetricKey`: `EXECUTION_STRENGTH`, `ACCUMULATED_TRADE_AMOUNT`, `EPS_GROWTH_RATE`, `ROE`
- `masterCode`: `WARREN_BUFFETT`, `PETER_LYNCH`, `RAY_DALIO`, `WILLIAM_ONEIL`
- `MasterChoiceResponse.recommendations[].tags`: `MasterOptionLogicCode` key
- `MasterChoiceResponse.recommendations[].sortMetricKey`: `ROE`, `PEG`, `MARKET_CAP`, `HIGH_52_WEEK_RATE`
- `MasterResponse.sectorOptions[].sectorType`, `MasterChoiceResponse.recommendations[].goodSectorTags`: `SectorType` key

## 모의투자 응답 key

- `MockInvestmentSectorStockItemResponse.tags`: `SectorType` key, `MARKET_LEADER`
- `MockInvestmentStockDetailResponse.sector`: `SectorType` key
- `MockInvestmentStockDetailResponse.tags`: `SectorType` key, `MARKET_LEADER`
- `MockInvestmentStockSearchSortType`: `NAME_ASC`, `PRICE_DESC`, `PRICE_ASC`, `MARKET_CAP_DESC`, `TRADE_AMOUNT_DESC`

### 종목 상세 응답 구조

- `price.currentPrice`: 현재가
- `price.changeRate`: 전일 대비 등락률
- `price.marketCap`: 시가총액
- `price.accumulatedTradeAmount`: 누적 거래대금
- `investmentMetrics.pbr`: PBR
- `investmentMetrics.per`: PER
- `investmentMetrics.roe`: ROE
- `investmentMetrics.dividendYield`: 시가배당률
- `investmentMetrics.payoutRatio`: 배당성향
- `investmentMetrics.executionStrength`: 체결강도
- `investmentMetrics.instNetBuy20Days`: 최근 20거래일 기관 순매수 수량
- `financialMetrics.sales`: 매출액
- `financialMetrics.operatingProfit`: 영업이익
- `financialMetrics.debtRatio`: 부채비율
