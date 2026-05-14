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

  KOSPI: "코스피"
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
```

## 오늘의 호주머니 추천 응답 key

- `investmentPurpose`: `CAPITAL_PROTECTION`, `DIVIDEND_INCOME`, `STEADY_GROWTH`, `CAPITAL_GAIN`
- `riskProfile`: `STABILITY`, `SAFE_PURSUIT`, `PROFIT_PURSUIT`, `AGGRESSIVE`
- `investmentHorizon`: `ULTRA_SHORT`, `SHORT`, `MID`, `LONG`
- `recommendations[].tags`: 투자 목적 또는 위험 감수 성향 key
- `recommendations[].goodSectorTags`: `SectorType` key
- `recommendations[].sortMetricKey`: `EXECUTION_STRENGTH`, `ACCUMULATED_TRADE_AMOUNT`, `EPS_GROWTH_RATE`, `ROE`
