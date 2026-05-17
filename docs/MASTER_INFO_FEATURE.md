# Master Info Feature Specification

본 문서는 투자 거장 소개, 거장 선택, 포트폴리오 화면에 필요한 API를 정의한다.

현재 구현된 거장의 선택 추천 API와 별개로, 피그마 화면 기준으로 필요한 거장 도메인 API를 최신화한다.

## AI Assistant Context

1. **화면 단위 API 분리**
    - 거장 목록, 상세 정보, 선택 결과, 포트폴리오 차트, 포트폴리오 설명을 각각 분리한다.
    - 화면에서 동시에 필요하지 않은 큰 컬렉션을 한 응답에 몰아넣지 않는다.
2. **N+1 쿼리 방지**
    - `Master`, `MasterTag`, `MasterPrinciple`, `MasterCase`, `MasterPortfolioStock`을 한 번에 다중 fetch join하지 않는다.
    - 컬렉션은 API 목적별로 분리 조회하거나 batch fetch를 활용한다.
3. **포트폴리오 차트 가공**
    - 분야별 차트는 `MasterPortfolioStock` 목록을 조회한 뒤 애플리케이션 레벨에서 `sector` 기준으로 그룹화하고 `weight`를 합산한다.
    - DB `GROUP BY`는 필수는 아니며, 포트폴리오 종목 수가 작다는 전제에서는 Java Stream 가공을 우선한다.
4. **응답 래핑**
    - 모든 응답은 공통 `ApiResponse<T>` 형태를 따른다.
    - 예: `{ "success": true, "code": "...", "message": "...", "data": ... }`

---

## 1. 거장 목록 조회 API

- **목적**: 거장 선택 화면에서 카드/리스트에 필요한 최소 정보를 조회한다.
- **Endpoint**: `GET /api/master-choice/masters`
- **상태**: 신규 구현 필요
- **사용 테이블**: `Master`, `MasterTag`, `Tag`

### Response Data

```json
[
  {
    "masterId": 1,
    "masterCode": "WARREN_BUFFETT",
    "masterName": "워런 버핏",
    "tags": ["가치 투자", "경제적 해자"],
    "returnRate": "연평균 약 20%"
  }
]
```

### 필드

- `masterId`: 거장 ID
- `masterCode`: 거장 식별 코드
- `masterName`: 거장 이름
- `tags`: 거장 성향 태그 목록
- `returnRate`: 과거 또는 연평균 수익률 표시 문구

---

## 2. 거장 상세정보 조회 API

- **목적**: 거장 상세 상단 영역에 필요한 소개 정보를 조회한다.
- **Endpoint**: `GET /api/master-choice/masters/{masterId}/detail`
- **상태**: 신규 구현 필요
- **사용 테이블**: `Master`, `MasterTag`, `Tag`, `MasterPrinciple`

### Response Data

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏",
  "tags": ["가치 투자", "경제적 해자"],
  "returnRate": "연평균 약 20%",
  "quote": "규칙 1: 절대 돈을 잃지 마라. 규칙 2: 규칙 1을 잊지 마라.",
  "philosophy": {
    "title": "우량 기업을 장기 보유",
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
- `returnRate`: 과거 또는 연평균 수익률 표시 문구
- `quote`: 거장 명언
- `philosophy`: 거장 투자 철학 1개
- `principles`: 거장 투자 원칙 여러 개
- `principles[].details`: 원칙 보조 설명 목록. 없으면 빈 배열 또는 생략 가능

---

## 3. 거장 선택 API

- **목적**: 사용자가 선택한 거장과 조건을 기반으로 추천 종목을 조회한다.
- **Endpoint**: `POST /api/master-choice/masters/{masterId}/recommendations`
- **상태**: 일부 구현됨
- **현재 구현 위치**: `MasterController.recommendMaster`
- **사용 테이블**: `Master`, `MasterOption`, `Stock`, `StockIndicator`, `Sector`

### Request

```json
{
  "selectedOptionIds": [1, 2, 3],
  "sectorTypes": ["IT_SEMICONDUCTOR"]
}
```

### Request 필드

- `selectedOptionIds`: 거장 추천 조건 ID 목록. 비우면 해당 거장의 모든 조건을 적용한다.
- `sectorTypes`: 섹터 선택이 필요한 조건(`LYNCH_SECTOR`, `DALIO_ALL_WEATHER`)을 선택한 경우에만 전달한다.

### Response Data

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏",
  "selectedOptionIds": [1, 2, 3],
  "totalCount": 10,
  "recommendations": [
    {
      "stockId": 1,
      "stockCode": "005930",
      "stockName": "삼성전자",
      "rank": 1,
      "tags": ["BUFFETT_ROE", "BUFFETT_PER"],
      "goodSectorTags": ["IT_SEMICONDUCTOR"],
      "matchedConditionCount": 2,
      "sortMetricKey": "ROE",
      "sortMetricValue": 15.2,
      "currentPrice": 75000,
      "changeRate": 1.23
    }
  ]
}
```

### 구현 시 확인할 점

- 현재 API는 추천 결과를 DB에 저장하지 않는다.
- 히스토리/최신 조회가 필요해지면 `RecommendationSaveService.saveMasterRecommendation` 연결 여부를 다시 결정한다.

---

## 4. 거장 포트폴리오 차트 조회 API

- **목적**: 포트폴리오 탭의 분야별 차트와 투자 기업 비율 차트를 조회한다.
- **Endpoint**: `GET /api/master-choice/masters/{masterId}/portfolio/chart`
- **상태**: 신규 구현 필요
- **사용 테이블**: `Master`, `MasterPortfolioStock`

### Response Data

```json
{
  "masterId": 1,
  "masterCode": "WARREN_BUFFETT",
  "masterName": "워런 버핏",
  "basePeriod": "2023년 4분기 기준",
  "returnRate": "연평균 약 20%",
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
- `returnRate`: 과거 또는 연평균 수익률 표시 문구
- `sectorChart`: 분야별 차트. `sector` 기준으로 `weight`를 합산한다.
- `companyRatioChart`: 투자 기업 비율. `weight` 기준 내림차순 정렬한다.

---

## 5. 거장 포트폴리오 설명 조회 API

- **목적**: 포트폴리오 설명 영역의 대표 투자 사례와 주식 리스트를 조회한다.
- **Endpoint**: `GET /api/master-choice/masters/{masterId}/portfolio/description`
- **상태**: 신규 구현 필요
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

1. `GET /api/master-choice/masters`
2. `GET /api/master-choice/masters/{masterId}/detail`
3. `GET /api/master-choice/masters/{masterId}/portfolio/chart`
4. `GET /api/master-choice/masters/{masterId}/portfolio/description`
5. 기존 `POST /api/master-choice/masters/{masterId}/recommendations`와 피그마의 거장 선택 플로우 연결 검증

## 현재 코드와의 차이

- 현재 구현된 `GET /api/master-choice/masters/{masterId}`는 거장의 추천 조건 조회 API에 가깝다.
- 피그마 기준의 상세정보 조회 API는 명언, 성향 태그, 철학, 원칙을 포함해야 하므로 별도 DTO와 엔드포인트가 필요하다.
- 목록 조회, 포트폴리오 차트 조회, 포트폴리오 설명 조회는 아직 컨트롤러/서비스/DTO 구현이 필요하다.
