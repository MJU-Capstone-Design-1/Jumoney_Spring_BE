# Master Description Initial Data

거장 소개, 상세 정보, 포트폴리오 차트, 대표 투자 사례 초기 데이터 초안이다.

필드명은 `MASTER_INFO_FEATURE.md`의 API 응답 규칙에 맞춰 camelCase를 사용한다. 저장/매핑 시에는 `masterCode`를 우선 식별자로 사용하고, DB 저장 후 응답에서는 `masterId`를 함께 내려준다.

## 1. 거장 목록/태그

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `masterCode` | String | Y | 거장 식별 코드 |
| `masterName` | String | Y | 거장 이름 |
| `tags` | Array<String> | Y | 거장 성향 태그 |
| `returnRate` | String | N | 과거 또는 연평균 수익률 표시 문구 |

```json
[
  {
    "masterCode": "WARREN_BUFFETT",
    "masterName": "워런 버핏",
    "tags": ["가치 투자", "경제적 해자"],
    "returnRate": "연평균 약 20%"
  },
  {
    "masterCode": "PETER_LYNCH",
    "masterName": "피터 린치",
    "tags": ["성장주 투자", "생활 속 발견"],
    "returnRate": "마젤란 펀드 연평균 약 29%"
  },
  {
    "masterCode": "RAY_DALIO",
    "masterName": "레이 달리오",
    "tags": ["거시 투자", "올웨더 포트폴리오"],
    "returnRate": "연평균 약 7-9%"
  },
  {
    "masterCode": "WILLIAM_ONEIL",
    "masterName": "윌리엄 오닐",
    "tags": ["모멘텀 투자", "CAN SLIM 전략"],
    "returnRate": "과거 25년간 약 40배 수익"
  }
]
```

## 2. 거장 상세 정보

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `masterCode` | String | Y | 거장 식별 코드 |
| `masterName` | String | Y | 거장 이름 |
| `tags` | Array<String> | Y | 거장 성향 태그 |
| `quote` | String | Y | 거장 대표 명언 |
| `philosophy` | Object | Y | 핵심 투자 철학 |
| `philosophy.title` | String | Y | 철학 제목 |
| `philosophy.description` | String | Y | 철학 설명 |
| `principles` | Array | Y | 투자 원칙 목록 |
| `principles[].title` | String | Y | 원칙 제목 |
| `principles[].description` | String | Y | 원칙 설명 |
| `principles[].details` | Array<String> | N | 원칙 보조 설명 목록 |

```json
[
  {
    "masterCode": "WARREN_BUFFETT",
    "masterName": "워런 버핏",
    "tags": ["가치 투자", "경제적 해자"],
    "quote": "규칙 1: 절대 돈을 잃지 마라. 규칙 2: 규칙 1을 잊지 마라.",
    "philosophy": {
      "title": "우량 기업을 장기 보유",
      "description": "단순히 숫자만 보고 사고팔지 말고, 장기적으로 함께할 수 있는 훌륭한 기업에 투자해요."
    },
    "principles": [
      {
        "title": "좋은 회사를 합리적인 가격에 오래 보유하기",
        "description": "회사의 실제 가치보다 낮은 가격에 사서, 나중에 가격이 제자리를 찾을 때까지 느긋하게 기다려요."
      },
      {
        "title": "경제적 해자 확인하기",
        "description": "다른 경쟁사가 흉내 낼 수 없는 강력한 브랜드나 기술을 가진 기업만 골라요."
      },
      {
        "title": "능력 범위 지키기",
        "description": "사업 모델이 너무 복잡해서 이해하기 어려운 곳은 포기하고, 내가 완벽하게 이해할 수 있는 산업에만 집중 투자해요."
      }
    ]
  },
  {
    "masterCode": "PETER_LYNCH",
    "masterName": "피터 린치",
    "tags": ["성장주 투자", "생활 속 발견"],
    "quote": "약간의 신경만 쓴다면 직장이나 동네 쇼핑몰에서 월스트리트 전문가들보다 훨씬 앞서 굉장한 종목을 가질 수 있다.",
    "philosophy": {
      "title": "아는 것에 투자하라",
      "description": "내가 평소에 자주 소비하고 좋다고 느끼는 제품을 만드는 회사에 투자해요."
    },
    "principles": [
      {
        "title": "텐버거 발굴",
        "description": "앞으로 10배 이상 커질 가능성이 있는 작지만 튼튼한 성장주를 끝까지 믿고 기다려요."
      },
      {
        "title": "직접 가서 눈으로 확인하기",
        "description": "매장에 사람이 많은지, 물건이 잘 팔리는지 직접 확인하며 기업의 성장 가능성을 확인해요."
      },
      {
        "title": "빚이 적고 꾸준히 성장하는 기업 선호",
        "description": "아무리 빠르게 성장하더라도 부채가 지나치게 많으면 위험하다고 판단하며, 안정적인 재무 구조를 가진 기업을 선호해요."
      },
      {
        "title": "성장 대비 너무 비싸지 않은 기업 찾기",
        "description": "아무리 좋은 회사라도 성장률 대비 가격이 지나치게 비싸면 투자하지 않아요. (PEG로 확인할 수 있어요.)"
      }
    ]
  },
  {
    "masterCode": "RAY_DALIO",
    "masterName": "레이 달리오",
    "tags": ["거시 투자", "올웨더 포트폴리오"],
    "quote": "분산투자는 투자에서 가장 중요한 원칙이다.",
    "philosophy": {
      "title": "올웨더(All-Weather) 포트폴리오",
      "description": "앞으로 경제가 좋아질지 나빠질지 맞추려 하지 말고, 어떤 상황이든 내 자산을 안전하게 지킬 수 있는 시스템을 만들어요."
    },
    "principles": [
      {
        "title": "서로 다르게 움직이는 자산 배분",
        "description": "하나가 떨어질 때 다른 하나는 오르는, 성격이 다른 자산들을 섞어 전체적인 내 자산의 변동성을 줄여요."
      },
      {
        "title": "경제의 사계절(호황, 불황, 인플레이션, 디플레이션) 대비하기",
        "description": "성장과 물가 상황에 따라 경제가 변할 때마다 수익을 내줄 자산들을 미리 배치해 둬요.",
        "details": [
          "경제가 잘 나갈 때: 주식, 회사채",
          "경제가 위축될 때: 국채",
          "물가가 오를 때: 금, 원자재",
          "물가가 안정될 때: 주식, 국채"
        ]
      },
      {
        "title": "위험의 비중 맞추기",
        "description": "자산을 똑같은 금액으로 나누는 게 아니라, 각 자산이 가진 위험의 크기를 똑같이 맞춰서 포트폴리오의 균형을 잡아요."
      }
    ]
  },
  {
    "masterCode": "WILLIAM_ONEIL",
    "masterName": "윌리엄 오닐",
    "tags": ["모멘텀 투자", "CAN SLIM 전략"],
    "quote": "가장 큰 실수는 이미 하락한 주식을 더 싸게 사려고 하는 것이다. 비싸게 사서 더 비싸게 팔아라.",
    "philosophy": {
      "title": "CAN SLIM 전략",
      "description": "과거 100년간 큰 수익을 낸 주도주들을 분석하여 탄생한 전략으로, 최고의 주식을 최적의 타이밍에 매수해요."
    },
    "principles": [
      {
        "title": "C: 현재 분기 순이익 (Current Quarterly Earnings)",
        "description": "최근 분기의 주당순이익(EPS)이 전년 동기 대비 최소 25% 이상 크고 빠르게 성장하는 기업을 선별해요."
      },
      {
        "title": "A: 연간 순이익 증가율 (Annual Earnings Growth)",
        "description": "과거 3~5년 동안 연간 순이익이 꾸준하고 의미 있게 증가하며 성장세를 유지하는지 확인해요."
      },
      {
        "title": "N: 신제품, 신경영, 신고가 (New Products, Management, Highs)",
        "description": "시장을 뒤흔들 신제품이나 새로운 경영진이 등장했거나, 주가가 저항선을 뚫고 새로운 신고가를 기록할 때 매수해요."
      },
      {
        "title": "S: 수요와 공급 (Supply and Demand)",
        "description": "시장에 유통되는 주식 수가 적거나 자사주를 적극적으로 매입하여, 결정적인 시점에 대규모 수요가 발생할 수 있는 기업을 찾아요."
      },
      {
        "title": "L: 주도주 여부 (Leader or Laggard)",
        "description": "동일한 산업 내에서도 시장을 이끌어가는 주도주만 매수하고, 뒤처지는 소외주는 과감히 피해요."
      },
      {
        "title": "I: 기관 투자자의 뒷받침 (Institutional Sponsorship)",
        "description": "뛰어난 실적과 뚜렷한 철학을 가진 펀드나 기관 투자자들이 최근 들어 집중적으로 매수하고 있는 종목을 따라가요."
      },
      {
        "title": "M: 시장의 방향성 (Market Direction)",
        "description": "아무리 좋은 주식도 하락장에서는 떨어지기 마련이므로, 전체 시장의 방향성이 강세장일 때만 투자하고 약세장에서는 몸을 사려요."
      }
    ]
  }
]
```

## 3. 포트폴리오 차트

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `masterCode` | String | Y | 거장 식별 코드 |
| `masterName` | String | Y | 거장 이름 |
| `returnRate` | String | N | 과거 또는 연평균 수익률 |
| `basePeriod` | String | Y | 포트폴리오 기준 기간 |
| `sectorChart` | Array | Y | 섹터별 자산 배분 비중 |
| `sectorChart[].sector` | String | Y | 산업 섹터 명칭 |
| `sectorChart[].weight` | Number | Y | 해당 섹터 비중 |
| `companyRatioChart` | Array | Y | 세부 종목별 보유 비중 |
| `companyRatioChart[].stockName` | String | Y | 종목명 또는 ETF 명칭 |
| `companyRatioChart[].weight` | Number | Y | 해당 종목 비중 |

```json
[
  {
    "masterCode": "WARREN_BUFFETT",
    "masterName": "워런 버핏",
    "returnRate": "연평균 약 20%",
    "basePeriod": "2025년 4분기 기준",
    "sectorChart": [
      { "sector": "정보기술", "weight": 22.6 },
      { "sector": "금융", "weight": 30.8 },
      { "sector": "필수소비재", "weight": 10.2 },
      { "sector": "에너지/화학", "weight": 7.2 },
      { "sector": "기타", "weight": 29.2 }
    ],
    "companyRatioChart": [
      { "stockName": "애플", "weight": 22.6 },
      { "stockName": "아메리칸 익스프레스", "weight": 20.4 },
      { "stockName": "뱅크오브아메리카", "weight": 10.4 },
      { "stockName": "코카콜라", "weight": 10.2 },
      { "stockName": "쉐브론", "weight": 7.2 },
      { "stockName": "기타", "weight": 29.2 }
    ]
  },
  {
    "masterCode": "PETER_LYNCH",
    "masterName": "피터 린치",
    "returnRate": "마젤란 펀드 연평균 약 29%",
    "basePeriod": "1988년 4분기 기준",
    "sectorChart": [
      { "sector": "필수소비재", "weight": 8.0 },
      { "sector": "금융", "weight": 6.5 },
      { "sector": "자유소비재", "weight": 10.5 },
      { "sector": "기타", "weight": 75.0 }
    ],
    "companyRatioChart": [
      { "stockName": "월마트", "weight": 8.0 },
      { "stockName": "페니메이", "weight": 6.5 },
      { "stockName": "맥도날드", "weight": 4.5 },
      { "stockName": "K마트", "weight": 3.0 },
      { "stockName": "시어스", "weight": 3.0 },
      { "stockName": "기타", "weight": 75.0 }
    ]
  },
  {
    "masterCode": "RAY_DALIO",
    "masterName": "레이 달리오",
    "returnRate": "연평균 약 7-9%",
    "basePeriod": "2025년 4분기 기준",
    "sectorChart": [
      { "sector": "ETF/지수", "weight": 27.2 },
      { "sector": "정보기술", "weight": 3.2 },
      { "sector": "필수소비재", "weight": 2.8 },
      { "sector": "헬스케어", "weight": 2.1 },
      { "sector": "기타", "weight": 64.7 }
    ],
    "companyRatioChart": [
      { "stockName": "IVV(S&P500 ETF)", "weight": 11.1 },
      { "stockName": "SPY(S&P500 ETF)", "weight": 10.6 },
      { "stockName": "IEMG(신흥국 ETF)", "weight": 5.5 },
      { "stockName": "엔비디아", "weight": 3.2 },
      { "stockName": "프록터 앤 갬블", "weight": 2.8 },
      { "stockName": "기타", "weight": 66.8 }
    ]
  },
  {
    "masterCode": "WILLIAM_ONEIL",
    "masterName": "윌리엄 오닐",
    "returnRate": "과거 25년간 약 40배 수익",
    "basePeriod": "2025년 4분기 기준",
    "sectorChart": [
      { "sector": "자유소비재", "weight": 15.7 },
      { "sector": "헬스케어", "weight": 14.9 },
      { "sector": "커뮤니케이션 서비스", "weight": 22.1 },
      { "sector": "정보기술", "weight": 6.7 },
      { "sector": "기타", "weight": 40.6 }
    ],
    "companyRatioChart": [
      { "stockName": "테슬라", "weight": 15.7 },
      { "stockName": "일라이 릴리", "weight": 14.9 },
      { "stockName": "레딧", "weight": 11.1 },
      { "stockName": "알파벳", "weight": 11.0 },
      { "stockName": "엔비디아", "weight": 6.7 },
      { "stockName": "기타", "weight": 40.6 }
    ]
  }
]
```

## 4. 대표 투자 사례

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `masterCode` | String | Y | 거장 식별 코드 |
| `masterName` | String | Y | 거장 이름 |
| `stockName` | String | Y | 대표 투자 사례 종목/펀드명 |
| `sector` | String | Y | 산업 분야 |
| `investmentPeriod` | String | Y | 집중 투자가 이루어진 시기 |
| `investmentResult` | String | Y | 투자 결과 |
| `title` | String | Y | 사례 제목 또는 핵심 교훈 |
| `description` | String | Y | 당시 시장 상황 및 투자 배경 |

```json
[
  {
    "masterCode": "WARREN_BUFFETT",
    "masterName": "워런 버핏",
    "stockName": "코카콜라",
    "sector": "식음료 및 일상소비재",
    "investmentPeriod": "1988년",
    "investmentResult": "약 10년간 10배 이상 상승",
    "title": "가치 투자 및 장기 보유의 정석",
    "description": "1987년 블랙 먼데이 대폭락 이후 시장에 공포가 가득했던 시기였으나 버핏은 강력한 브랜드 파워와 변하지 않는 소비 패턴에 주목하여 과감하게 집중 투자를 단행했어요."
  },
  {
    "masterCode": "PETER_LYNCH",
    "masterName": "피터 린치",
    "stockName": "던킨 도너츠",
    "sector": "생활 프랜차이즈",
    "investmentPeriod": "1980년대 초",
    "investmentResult": "약 10배 이상 수익",
    "title": "생활 밀착형 종목 발굴",
    "description": "인플레이션과 경기 침체가 반복되던 시기였으나 피터 린치는 매일 아침 사람들이 던킨 매장에 줄을 서서 커피와 도너츠를 사는 일상적인 모습에서 폭발적인 확장 가능성을 발견했어요."
  },
  {
    "masterCode": "RAY_DALIO",
    "masterName": "레이 달리오",
    "stockName": "퓨어 알파 펀드",
    "sector": "매크로 헤지펀드",
    "investmentPeriod": "2008년",
    "investmentResult": "시장 폭락 속에서 약 9.4퍼센트 플러스 수익",
    "title": "경제 사이클 기반의 리스크 분산 투자",
    "description": "서브프라임 모기지 사태로 전 세계 금융 시스템이 붕괴되고 주식 시장이 폭락하던 시기였으나 부채 위기 사이클을 미리 예측하고 시스템적인 분산 투자로 대응했어요."
  },
  {
    "masterCode": "WILLIAM_ONEIL",
    "masterName": "윌리엄 오닐",
    "stockName": "시스코 시스템즈",
    "sector": "첨단 IT 통신 장비",
    "investmentPeriod": "1990년",
    "investmentResult": "수년간 수백 퍼센트 이상 상승",
    "title": "신고가 돌파 및 주도주 매수",
    "description": "IT 혁명의 초기 단계로 네트워크 장비 수요가 급증하던 시기였으며 실적 성장과 차트상의 컵 앤 핸들 패턴이 완벽하게 결합된 주도주가 등장하던 상황이에요."
  }
]
```

## 5. 포트폴리오 주식 리스트

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `masterCode` | String | Y | 거장 식별 코드 |
| `masterName` | String | Y | 거장 이름 |
| `basePeriod` | String | Y | 포트폴리오 기준 기간 |
| `stocks` | Array | Y | 보유 종목 리스트 |
| `stocks[].stockName` | String | Y | 종목명 또는 ETF 명칭 |
| `stocks[].weight` | Number | Y | 보유 비중 |
| `stocks[].sector` | String | Y | 산업 섹터 |

```json
[
  {
    "masterCode": "WARREN_BUFFETT",
    "masterName": "워런 버핏",
    "basePeriod": "2025년 4분기 기준",
    "stocks": [
      { "stockName": "애플", "weight": 22.6, "sector": "정보기술" },
      { "stockName": "아메리칸 익스프레스", "weight": 20.4, "sector": "금융" },
      { "stockName": "뱅크오브아메리카", "weight": 10.4, "sector": "금융" },
      { "stockName": "코카콜라", "weight": 10.2, "sector": "필수소비재" },
      { "stockName": "쉐브론", "weight": 7.2, "sector": "에너지/화학" },
      { "stockName": "무디스", "weight": 4.6, "sector": "금융" },
      { "stockName": "옥시덴탈 페트롤리움", "weight": 4.0, "sector": "에너지/화학" },
      { "stockName": "처브", "weight": 3.9, "sector": "금융" },
      { "stockName": "크래프트 하인즈", "weight": 2.9, "sector": "필수소비재" },
      { "stockName": "알파벳", "weight": 2.0, "sector": "커뮤니케이션 서비스" }
    ]
  },
  {
    "masterCode": "PETER_LYNCH",
    "masterName": "피터 린치",
    "basePeriod": "1988년 4분기 기준",
    "stocks": [
      { "stockName": "월마트", "weight": 8.0, "sector": "필수소비재" },
      { "stockName": "페니메이", "weight": 6.5, "sector": "금융" },
      { "stockName": "맥도날드", "weight": 4.5, "sector": "자유소비재" },
      { "stockName": "K마트", "weight": 3.0, "sector": "자유소비재" },
      { "stockName": "시어스", "weight": 3.0, "sector": "자유소비재" },
      { "stockName": "질레트", "weight": 2.25, "sector": "필수소비재" },
      { "stockName": "에머슨 일렉트릭", "weight": 2.0, "sector": "산업재" },
      { "stockName": "레블론", "weight": 1.75, "sector": "필수소비재" },
      { "stockName": "크라이슬러", "weight": 1.5, "sector": "자유소비재" },
      { "stockName": "기타", "weight": 67.5, "sector": "기타" }
    ]
  },
  {
    "masterCode": "RAY_DALIO",
    "masterName": "레이 달리오",
    "basePeriod": "2025년 4분기 기준",
    "stocks": [
      { "stockName": "IVV (S&P 500 ETF)", "weight": 11.1, "sector": "ETF/지수" },
      { "stockName": "SPY (S&P 500 ETF)", "weight": 10.6, "sector": "ETF/지수" },
      { "stockName": "IEMG (신흥국 ETF)", "weight": 5.5, "sector": "ETF/지수" },
      { "stockName": "엔비디아", "weight": 3.2, "sector": "정보기술" },
      { "stockName": "프록터 앤 갬블", "weight": 2.8, "sector": "필수소비재" },
      { "stockName": "존슨앤존슨", "weight": 2.1, "sector": "헬스케어" },
      { "stockName": "마이크로소프트", "weight": 1.7, "sector": "정보기술" },
      { "stockName": "아마존", "weight": 1.6, "sector": "자유소비재" },
      { "stockName": "메타 플랫폼스", "weight": 1.5, "sector": "커뮤니케이션 서비스" },
      { "stockName": "코스트코", "weight": 1.2, "sector": "자유소비재" }
    ]
  },
  {
    "masterCode": "WILLIAM_ONEIL",
    "masterName": "윌리엄 오닐",
    "basePeriod": "2025년 4분기 기준",
    "stocks": [
      { "stockName": "테슬라", "weight": 15.7, "sector": "자유소비재" },
      { "stockName": "일라이 릴리", "weight": 14.9, "sector": "헬스케어" },
      { "stockName": "레딧", "weight": 11.1, "sector": "커뮤니케이션 서비스" },
      { "stockName": "알파벳", "weight": 11.0, "sector": "커뮤니케이션 서비스" },
      { "stockName": "엔비디아", "weight": 6.7, "sector": "정보기술" },
      { "stockName": "로켓 랩", "weight": 4.8, "sector": "산업재" },
      { "stockName": "제타 글로벌", "weight": 4.3, "sector": "정보기술" },
      { "stockName": "트윌리오", "weight": 4.2, "sector": "정보기술" },
      { "stockName": "TSMC", "weight": 2.6, "sector": "정보기술" },
      { "stockName": "퍼스트 솔라", "weight": 2.3, "sector": "에너지/화학" }
    ]
  }
]
```

## 데이터 출처 메모

- 워런 버핏: 2025년 4분기 기준 버크셔 보고서 참고.
- 레이 달리오: 2025년 4분기 기준 브리지워터 참고.
- 피터 린치: 1988년 4분기 기준 마젤란 펀드 참고.
- 윌리엄 오닐: 2023년에 사망했으므로, 윌리엄 오닐의 철학을 계승한 William O'Neil 관련 2025년 4분기 포트폴리오 기준.
