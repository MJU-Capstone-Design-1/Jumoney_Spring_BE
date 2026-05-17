# Master Info Feature Specification

본 문서는 투자 거장 소개, 거장 선택, 포트폴리오 화면에 필요한 API를 정의한다.

현재 구현된 거장의 선택 추천 API와 별개로, 피그마 화면 기준으로 필요한 거장 도메인 API를 최신화한다.

---

## 1. 거장 목록 조회 API

- **목적**: 거장 선택 화면에서 카드/리스트에 필요한 최소 정보를 조회한다.
- **Endpoint**: `GET /api/master/masters`
- **상태**: 구현 완료
- **사용 테이블**: `Master`, `MasterTag`, `Tag`

### Response Data

```json
[
  {
    "masterId": 1,
    "masterCode": "WARREN_BUFFETT",
    "masterName": "워런 버핏",
    "tags": ["가치 투자", "경제적 해자"]
  }
]
```

### 필드

- `masterId`: 거장 ID
- `masterCode`: 거장 식별 코드
- `masterName`: 거장 이름
- `tags`: 거장 성향 태그 목록

---

## 2. 거장 상세정보 조회 API

- **목적**: 거장 상세 상단 영역에 필요한 소개 정보를 조회한다.
- **Endpoint**: `GET /api/master/masters/{masterId}/detail`
- **상태**: 구현 완료
- **사용 테이블**: `Master`, `MasterTag`, `Tag`, `MasterPrinciple`

### Response Data

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏",
  "tags": ["가치 투자", "경제적 해자"],
  "quote": "규칙 1: 절대 돈을 잃지 마라. 규칙 2: 규칙 1을 잊지 마라.",
  "philosophy": {
    "title": "우량 기업 장기 보유",
    "description": "단순히 숫자만 보고 사고팔지 말고, 장기적으로 함께할 수 있는 훌륭한 기업에 투자해요."
  },
  "principles": [
    {
      "title": "경제적 해자",
      "description": "장기간 경쟁 우위를 유지할 수 있는 기업을 선호한다.",
      "details": []
    }
  ]
}
```

### 필드

- `masterId`: 거장 ID
- `masterCode`: 거장 식별 코드
- `masterName`: 거장 이름
- `tags`: 거장 성향 태그 목록
- `quote`: 거장 명언
- `philosophy`: 거장 투자 철학 1개
- `principles`: 거장 투자 원칙 여러 개
- `principles[].details`: 원칙 보조 설명 목록. 없으면 빈 배열 또는 생략 가능

---

## 3. 거장 선택 API

- **목적**: 사용자가 자신의 팀으로 사용할 거장을 선택한다.
- **Endpoint**: `POST /api/master/masters/{masterId}/selection`
- **상태**: 구현 완료
- **사용 테이블**: `User`, `Master`

### Request

```json
{}
```

### Request 필드

- 별도 request body는 필요하지 않다.

### Response Data

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏"
}
```

### 구현 시 확인할 점

- 로그인 사용자 기준으로 선택한 거장을 저장한다.
- 저장 위치는 `User.selectedMaster` 또는 별도 선택 이력 테이블 중 서비스 정책에 맞춰 결정한다.

---

## 4. 거장 포트폴리오 차트 조회 API

- **목적**: 포트폴리오 탭의 분야별 차트와 투자 기업 비율 차트를 조회한다.
- **Endpoint**: `GET /api/master/masters/{masterId}/portfolio/chart`
- **상태**: 구현 완료
- **사용 테이블**: `Master`, `MasterPortfolioStock`

### Response Data

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏",
  "basePeriod": "2023년 4분기 기준",
  "sectorChart": [
    {
      "sector": "금융",
      "weight": 42.5
    }
  ],
  "companyRatioChart": [
    {
      "stockName": "Apple",
      "weight": 48.2
    }
  ]
}
```

### 필드

- `basePeriod`: 포트폴리오 기준 기간
- `masterName`: 거장 이름
- `sectorChart`: 분야별 차트. `sector` 기준으로 `weight`를 합산한다.
- `companyRatioChart`: 투자 기업 비율. `weight` 기준 내림차순 정렬한다.

---

## 5. 거장 포트폴리오 설명 조회 API

- **목적**: 포트폴리오 설명 영역의 대표 투자 사례와 주식 리스트를 조회한다.
- **Endpoint**: `GET /api/master/masters/{masterId}/portfolio/description`
- **상태**: 구현 완료
- **사용 테이블**: `MasterCase`, `MasterPortfolioStock`

### Response Data

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏",
  "basePeriod": "2023년 4분기 기준",
  "representativeCase": {
    "stockName": "Coca-Cola",
    "sector": "필수소비재",
    "investmentPeriod": "1988년 이후",
    "investmentResult": "장기 보유를 통한 높은 배당 및 시세 차익",
    "title": "코카콜라 장기 투자",
    "description": "브랜드 경쟁력과 현금 창출력을 보고 장기 보유한 대표 사례다."
  },
  "stocks": [
    {
      "stockName": "Apple",
      "sector": "IT",
      "weight": 48.2
    }
  ]
}
```

### 필드

- `representativeCase`: 대표 투자 사례. 현재 정책은 거장별 1건 조회다.
- `stocks`: 포트폴리오 주식 리스트. `weight` 기준 내림차순 정렬한다.

---

## 구현 우선순위

1. `GET /api/master/masters` - 구현 완료
2. `GET /api/master/masters/{masterId}/detail` - 구현 완료
3. `POST /api/master/masters/{masterId}/selection` - 구현 완료
4. `GET /api/master/masters/{masterId}/portfolio/chart` - 구현 완료
5. `GET /api/master/masters/{masterId}/portfolio/description` - 구현 완료

## 현재 코드와의 차이

- 현재 구현된 `GET /api/master-choice/masters/{masterId}`는 거장의 추천 조건 조회 API에 가깝다.
- 현재 구현된 `POST /api/master-choice/masters/{masterId}/recommendations`는 거장의 선택 추천 API이며, 사용자의 팀 선택 API와 별도 기능이다.
- 목록 조회와 상세정보 조회는 `/api/master`에 구현되어 있다.
- 팀 선택, 포트폴리오 차트 조회, 포트폴리오 설명 조회는 `/api/master`에 구현되어 있다.
