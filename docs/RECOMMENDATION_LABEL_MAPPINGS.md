# Recommendation Label Mappings

추천 API 응답으로 내려주는 영어 enum/key의 프론트 표시용 한국어 매핑입니다.

```ts
export const recommendationLabels = {
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

  KOSPI: "코스피",
  KOSDAQ: "코스닥",
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
