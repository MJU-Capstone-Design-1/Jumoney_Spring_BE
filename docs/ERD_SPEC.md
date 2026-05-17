# Database Specification (ERD)

본 문서는 '주머니' 프로젝트의 데이터베이스 상세 명세를 다룹니다. 총 31개의 테이블로 구성되어 있습니다.
코드를 작성할 때 참고하나, 더 좋은 설계가 있을 경우 설명과 함께 변경해도 됩니다.

## AI Assistant Context & Rules
이 문서를 읽는 AI는 다음의 백엔드 설계 원칙을 참고하여 JPA Entity 및 DB 쿼리를 생성하라:
1. **Database Dialect**: PostgreSQL을 사용한다.
2. **Logical N:M (역정규화)**: `selectedOptionIds` 등 명세에 `JSON`으로 표기된 id 필드는 물리적인 매핑 테이블(FK)을 생성하지 마라. PostgreSQL의 `JSONB` 타입으로 생성하고, JPA에서는 `List<Long>` 타입 등으로 매핑하여 어플리케이션 레벨에서 관리한다.
3. **Super-type/Sub-type**: `Recommendation`은 슈퍼타입이며, `HojumoneyRecommendation`과 `MasterRecommendation`은 서브타입이다. JPA 생성 시 `@Inheritance(strategy = InheritanceType.JOINED)`를 사용하라.
4. **결측치 허용**: `StockPrice`의 시세 및 거래량 데이터는 시장 상황(거래정지 등)에 따라 Null이 허용되므로 Primitive type(long, double) 대신 Wrapper class(Long, BigDecimal)를 사용하라.
5. **PK / FK Mapping**: `(PK)`가 명시된 필드는 `@Id`로 설정하고, `(FK)`가 명시된 필드는 연관관계의 주인으로서 `@JoinColumn`을 명시하라.

---

### **사용자 및 소셜 (User & Social)**

- **`User`: 사용자 테이블**
    - 관계
        - **`UserRanking`**(1:1)
        - **`Master`**(N:1)
        - **`Account`**(1:1)
        - **`Recommendation`**(1:N)
        - **`StockTermBookmark`**(1:N)
    - 필드
        - **사용자 ID / userId / BIGINT / NOT NULL (PK)**
        - 소셜 플랫폼 / provider / VARCHAR(20) / NOT NULL
        - 소셜 고유 ID / providerId / VARCHAR(100) / NOT NULL
        - 카카오닉네임 / nickname / VARCHAR(100) / NOT NULL
        - 거장 ID / masterId / BIGINT / NULL (FK)
        - 활성화 여부 / isActive / BOOLEAN / NOT NULL
        - 생성일시 / createdAt / TIMESTAMP / NOT NULL
        - 수정일시 / updatedAt / TIMESTAMP / NOT NULL

- **`UserRanking`: 사용자 랭킹 테이블**
    - 관계
        - **`User`**(1:1)
        - **`Master`**(N:1)
    - 필드
        - **랭킹 ID / rankingId / BIGINT / NOT NULL (PK)**
        - **사용자 ID / userId / BIGINT / NOT NULL (FK)**
        - **거장 ID / masterId / BIGINT / NOT NULL (FK)**
        - 현재 수익률 / profitRate / DECIMAL / NOT NULL
        - 전체 순위 / overallRank / INTEGER / NOT NULL
        - 팀 내 순위 / groupRank / INTEGER / NOT NULL
        - 랭킹 기준 일자 / rankingDate / DATE / NOT NULL

### **주식 용어 (Stock Terms)**

- **`StockTerm`: 주식 용어 테이블**
    - 관계
        - **`StockTermBookmark`** (1:N)
    - 필드
        - **주식용어 ID / termId / BIGINT / NOT NULL (PK)**
        - 카테고리명 / category / ENUM / NOT NULL
        - 용어 이름 / termName / VARCHAR(50) / NOT NULL
        - 용어 설명 / description / TEXT / NOT NULL
        - 이미지 파일 명 / imageFileName / VARCHAR(100) / NOT NULL

- **`StockTermScrap`: 용어 스크랩 테이블 (주식 용어 - 사용자 매핑 테이블)**
    - 관계
        - **`User`** (N:1)
        - **`StockTerm`** (N:1)
    - 필드
        - **스크랩 ID / scrapId / BIGINT / NOT NULL (PK)**
        - **주식 용어 ID / termId / BIGINT / NULL (FK)**
        - **유저 ID / userId / BIGINT / NOT NULL (FK)**
        - 생성일시 / createdAt / TIMESTAMP / NOT NULL

### **주식 종목 (Stock Items)**

- **`Stock`: 종목 테이블**
    - 관계
        - **`StockIndicator`**(1:N)
        - **`StockPrice`**(1:N)
        - **`Portfolio`**(1:N)
        - **`Order`**(1:N)
        - **`RecommendationStock`**(1:N)
        - **`Sector`**(N:1)
    - 필드
        - **종목 ID / stockId / BIGINT / NOT NULL (PK)**
        - **섹터 ID / sectorId / BIGINT / NOT NULL (FK)**
        - 종목 코드 / stockCode / VARCHAR(6) / NOT NULL / 예: 005930
        - 종목명 / name / VARCHAR(50) / NOT NULL / 예: 삼성전자
        - 시장 구분 / marketType / ENUM / NOT NULL / 예: KOSPI
        - 종목 설명 / description / JSONB / NULL
        - 대장주 여부 / isMarketLeader / BOOLEAN  / NOT NULL

- **`StockIndicator`: 종목 지표 테이블**
    - 관계
        - **`Stock`**(N:1)
    - 필드
        - **지표 ID / indicatorId / BIGINT / NOT NULL (PK)**
        - **종목 ID / stockId / BIGINT / NOT NULL (FK)**
        - 기준년월 / baseTime / VARCHAR(6) / NOT NULL / 예: "202403"
        - 시가총액 / marketCap / BIGINT / NOT NULL
        - 부채비율 / debtRatio / DECIMAL / NOT NULL
        - 영업이익 / operatingProfit / BIGINT / NOT NULL
        - 영업이익 증가율 / operatingProfitGrowthRate / DECIMAL / NOT NULL
        - 주당배당금 / dps / DECIMAL / NOT NULL / 기간 내 주당배당금(DPS) 합계
        - 시가배당률 / dividendYield / DECIMAL / NOT NULL / `DPS / 현재가 * 100`으로 계산
        - 배당성향 / payoutRatio / DECIMAL / NULL / `기간 내 주당배당금(DPS) 합계 / EPS * 100`으로 계산, DPS 또는 EPS 결측 시 NULL
        - ROE / roe / DECIMAL / NOT NULL
        - PER / per / DECIMAL / NOT NULL
        - PBR / pbr / DECIMAL / NOT NULL
        - 당기 EPS / currentEps / DECIMAL / NOT NULL
        - 전년 동기 EPS / lastYearEps / DECIMAL / NULL / 신규 상장주 등 전년도 EPS가 없으면 NULL
        - 당기 매출액 / currentSales / BIGINT / NOT NULL
        - 전년 동기 매출액 / lastYearSales / BIGINT / NULL / 신규 상장주 등 전년도 매출액이 없으면 NULL
        - 신용잔고율 / marginDebtRate / DECIMAL / NOT NULL
        - 52주 신고가 대비 현재가 비율 / high52WeekRate / DECIMAL / NOT NULL
        - 최근 20거래일 기관 순매수 합계 / instNetBuy20Days / BIGINT / NOT NULL

- **`StockPrice`: 종목 시세 테이블**
    - 관계
        - **`Stock`**(N:1)
    - 필드
        - **시세 ID / stockPriceId / BIGINT / NOT NULL (PK)**
        - **종목 ID / stockId / BIGINT / NOT NULL (FK)**
        - 캔들 타입 / intervalType  / ENUM / NOT NULL (MIN, DAY, WEEK, MONTH, YEAR)
        - 기준 시간 / baseTime / TIMESTAMP / NOT NULL
        - 시가 / openPrice / DECIMAL / NULL
        - 고가 / highPrice / DECIMAL / NULL
        - 저가 / lowPrice / DECIMAL / NULL
        - 종가 / closePrice / DECIMAL / NULL
        - 수정 종가 / adjustedClosePrice / DECIMAL / NULL
        - 거래량 / volume / BIGINT / NULL
        - 누적 거래대금 / tradingValue / BIGINT / NULL
        - 전일 대비 금액 / changeAmount / DECIMAL / NULL
        - 전일 대비 등락률 / changeRate / DECIMAL / NULL

- **`Sector`: 섹터 테이블**
    - 관계
        - **`Stock`** (1:N)
        - **`NewsSectorMapping`**(1:N)
    - 필드
        - **섹터 ID / sectorId / BIGINT / NOT NULL (PK)**
        - 섹터명 / sectorName / ENUM / NOT NULL

- **`HtsStock`: HTS 조건검색 종목 테이블**
    - 관계
        - **`Stock`**(N:1)
    - 필드
        - **HTS 종목 ID / htsStockId / BIGINT / NOT NULL (PK)**
        - **종목 ID / stockId / BIGINT / NOT NULL (FK)**
        - 조건검색 유형 / searchType / ENUM / NOT NULL (STABILITY, SAFE_PURSUIT, PROFIT_PURSUIT, AGGRESSIVE)
        - 기준 일자 / baseDate / DATE / NOT NULL

### **자산 (Asset)**

- **`Account`: 계좌 테이블**
    - 관계
        - **`User`**(1:1)
        - **`Portfolio`**(1:N)
        - **`Order`**(1:N)
    - 필드
        - **계좌 ID / accountId / BIGINT / NOT NULL (PK)**
        - **유저 ID / userId / BIGINT / NOT NULL (FK)**
        - 초기 자본금 / seedMoney / DECIMAL / NOT NULL
        - 예수금 / cashBalance / DECIMAL / NOT NULL
        - 총 매수 금액 / totalPurchaseAmount / DECIMAL / NOT NULL
        - 총 자산 /  totalAsset / DECIMAL / NOT NULL
        - 총 수익률 / totalProfitRate / DECIMAL / NULL
        - 계좌 개설 일시 / createdAt / TIMESTAMP / NOT NULL

- **`Portfolio`: 보유 종목 테이블 (계좌-종목 매핑 테이블)**
    - 관계
        - **`Account`**(N:1)
        - **`Stock`**(N:1)
    - 필드
        - **포트폴리오 ID / portfolioId / BIGINT / NOT NULL (PK)**
        - **계좌 ID / accountId / BIGINT / NOT NULL (FK)**
        - **종목 ID / stockId / BIGINT / NOT NULL (FK)**
        - 보유 수량 / quantity / INT / NOT NULL
        - 매수 평단가 / averagePurchasePrice / DECIMAL / NOT NULL
        - 총 매수 금액 / totalPurchaseAmount / DECIMAL / NOT NULL
        - 마지막 매매 일시 / updatedAt / TIMESTAMP /  NOT NULL

- **`Order`: 주문 이력 테이블**
    - 관계
        - **`Account`**(N:1)
        - **`Stock`**(N:1)
    - 필드
        - **주문 ID / orderId / BIGINT / NOT NULL (PK)**
        - **계좌 ID / accountId / BIGINT / NOT NULL (FK)**
        - **종목 ID / stockId / BIGINT / Nullable (FK)**
        - 주문 유형 / orderType / ENUM / NOT NULL / 예: BUY, SELL
        - 체결가 / executionPrice / DECIMAL / NOT NULL
        - 주문 수량 / quantity / INT / NOT NULL
        - 총 체결 금액 / totalExecutionAmount / DECIMAL / NOT NULL
        - 주문 일시 / executedAt / TIMESTAMP / NOT NULL

### **태그 (Tag)**

- **`Tag`: 태그 테이블**
    - 관계
        - **`MasterTag`**(1:N)
    - 필드
        - **태그 ID / tagId / BIGINT / NOT NULL (PK)**
        - 태그 이름 / tagName / VARCHAR / NOT NULL

- **`MasterTag`: 거장-태그 매핑 테이블**
    - 관계
        - **`Tag`** (N:1)
        - **`Master`** (N:1)
    - 필드
        - **거장태그 ID / masterTagId /  BIGINT / NOT NULL (PK)**
        - **태그 ID / tagId / BIGINT / NOT NULL (FK)**
        - **거장 ID / masterId / BIGINT / NOT NULL (FK)**

- **`RecommendationStockTag`: 추천 종목-태그 매핑 테이블**
    - 관계
        - **`RecommendationStock`**(N:1)
    - 필드
        - **추천 종목 태그 ID / recommendationStockTagId / BIGINT / NOT NULL (PK)**
        - **추천 종목 ID / recommendationStockId / BIGINT / NOT NULL (FK)**
        - 태그 타입 / tagType / ENUM / NOT NULL / 예: SURVEY_LOGIC, GOOD_SECTOR
        - 태그명 / tagName / VARCHAR(100) / NOT NULL
    - 비고
        - `SURVEY_LOGIC` 태그는 enum 코드명이 아니라 사용자에게 표시 가능한 `SurveyLogicCode.label` 값을 저장한다.
        - `GOOD_SECTOR` 태그는 프론트 표시 문자열이 아니라 `SectorType` enum 코드명을 저장한다. 예: `IT_SEMICONDUCTOR`

### **거장 (Master)**

- **`Master`: 거장 테이블**
    - 관계
        - **`User`**(1:N)
        - **`MasterRecommendation`**(1:N)
        - **`MasterOption`**(1:N)
        - **`MasterTag`**(1:N)
        - **`MasterCase`**(1:N)
        - **`MasterPortfolioStock`**(1:N)
        - **`MasterPrinciple`**(1:N)
    - 필드
        - **거장 ID / masterId / BIGINT / NOT NULL (PK)**
        - 거장 이름 / masterName / VARCHAR(50) / NOT NULL
        - 명언 / quote / VARCHAR(200) /  NULL
        - 이미지 파일 명 / imageFileName / VARCHAR(200) / NULL
        - 포트폴리오 기준일 / portfolioBasePeriod / VARCHAR(50) / NOT NULL
        - 철학 제목 / philosophyTitle / VARCHAR(100) / NOT NULL
        - 철학 설명 / philosophyDescription / TEXT / NOT NULL
        - 거장의 선택 설명 / recommendationDescription /  TEXT / NOT NULL

- **`MasterPrinciple`: 거장 원칙 테이블**
    - 관계
        - **`Master`**(N:1)
    - 필드
        - **거장 원칙 ID / masterPrincipleId / BIGINT / NOT NULL (PK)**
        - **거장 ID / masterId / BIGINT / NOT NULL (FK)**
        - 원칙 제목 / title / VARCHAR(100) / NOT NULL
        - 원칙 설명 / description / TEXT / NOT NULL
        - 원칙 보조 설명 목록 / details / JSONB / NULL

- **`MasterCase`: 거장 투자 사례 테이블**
    - 관계
        - **`Master`**(1:N)
    - 필드
        - **거장 투자 사례 ID / masterCaseId / BIGINT / NOT NULL (PK)**
        - **거장 ID / masterId / BIGINT / NOT NULL (FK)**
        - 종목명 / stockName / VARCHAR(50) / NOT NULL
        - 섹터 / sector / VARCHAR(50) / NOT NULL
        - 투자 시기 / investmentPeriod / VARCHAR(50) / NOT NULL
        - 투자 결과 / investmentResult / VARCHAR(100) / NOT NULL
        - 사례 제목 / title / VARCHAR(100) / NOT NULL
        - 사례 설명 / description / TEXT / NOT NULL

- **`MasterPortfolioStock`: 거장 포트폴리오 종목 테이블**
    - 관계
        - **`Master`**(N:1)
    - 필드
        - **거장 포트폴리오 종목 ID / masterPortfolioStockId / BIGINT / NOT NULL (PK)**
        - **거장 ID / masterId / BIGINT / NOT NULL (FK)**
        - 종목명 / stockName / VARCHAR(50) / NOT NULL
        - 섹터 / sector / VARCHAR(50) / NOT NULL
        - 투자 비중 / weight / DECIMAL / NOT NULL

### **분석 (Analysis)**

- **`News`: 뉴스 원문 테이블**
    - 관계
        - **`NewsAnalysisMapping`**(1:N)
    - 필드
        - **뉴스 ID / newsId / BIGINT / NOT NULL (PK)**
        - 뉴스 URL / newsUrl / VARCHAR(255) / NOT NULL
        - 제목 / title / VARCHAR(255) / NOT NULL
        - 내용 / content / TEXT / NOT NULL
        - 뉴스 발행 일시 / publishedAt / TIMESTAMP / NOT NULL

- **`NewsAnalysisMapping`: 뉴스-뉴스 분석 매핑 테이블**
    - 관계
        - **`News`**(N:1)
        - **`NewsAnalysis`**(N:1)
    - 필드
        - **매핑 ID / mappingId / BIGINT / NOT NULL (PK)**
        - **뉴스 ID / newsId / BIGINT / NOT NULL (FK)**
        - **뉴스 분석 ID / newsAnalysisId / BIGINT / NOT NULL (FK)**

- **`NewsAnalysis`: 뉴스 분석 테이블**
    - 관계
        - **`NewsAnalysisMapping`**(1:N)
        - **`NewsSectorMapping`**(1:N)
        - **`Recommendation`**(1:N)
    - 필드
        - **뉴스 분석 ID / newsAnalysisId / BIGINT / NOT NULL (PK)**
        - 분석 시간 / baseTime / TIMESTAMP / NOT NULL
        - 분석 결과 / analysisResult / TEXT / NOT NULL
        - 분석 결과 요약 / summary / TEXT / NOT NULL
        - 분석 결과 이유 / reasoning / TEXT / NOT NULL
        - 분석 뉴스 개수 / newsCount / INT / NOT NULL
        - 핵심 키워드 / keyword / VARCHAR(50) / NOT NULL

- **`NewsSectorMapping`: 섹터 분석 테이블 (뉴스 분석-섹터 매핑 테이블)**
    - 관계
        - **`Sector`** (N:1)
        - **`NewsAnalysis`** (N:1)
    - 필드
        - **섹터 분석 ID / newsSectorId / BIGINT / NOT NULL (PK)**
        - **뉴스 분석 ID / newsAnalysisId / BIGINT / NOT NULL (FK)**
        - **섹터 ID / sectorId / BIGINT / NOT NULL (FK)**
        - 섹터 타입 / sectorType / ENUM / NOT NULL / 예: GOOD

### **추천 (Recommendation)**

- **`MasterOption`: 거장 선택지 테이블**
    - 관계
        - **`Master`**(N:1)
        - **`MasterRecommendation`**(Logical N:M)
    - 필드
        - **거장 선택지 ID / masterOptionId / BIGINT / NOT NULL (PK)**
        - **거장 ID / masterId / BIGINT / NOT NULL (FK)**
        - 선택지 내용 / content / VARCHAR(100) / NOT NULL
        - 선택지 설명 / description / TEXT / NOT NULL
        - 로직 코드 / logicCode / ENUM / NOT NULL

- **`MasterRecommendation`: 거장의 선택 추천 결과 테이블**
    - 관계
        - **`Recommendation`**(1:1)
        - **`Master`**(N:1)
        - **`MasterOption`**(Logical N:M)
    - 필드
        - **거장 추천 ID / masterRecommendationId / BIGINT / NOT NULL (PK)**
        - **추천 ID / recommendationId / BIGINT / NOT NULL (FK)**
        - **거장 ID / masterId / BIGINT / NOT NULL (FK)**
        - 선택된 옵션 리스트 / selectedOptionIds / JSONB / NOT NULL / 예: [1, 3, 4]

- **`SurveyQuestion`: 설문 문항 테이블**
    - 관계
        - **`SurveyOption`**(1:N)
    - 필드
        - **설문 문항 ID / surveyQuestionId / BIGINT / NOT NULL (PK)**
        - 문항 타입 / questionType / ENUM / NOT NULL / UNIQUE
        - 문항 내용 / content / VARCHAR(255) / NOT NULL
        - 문항 설명 / description / TEXT / NOT NULL
        - 표시 순서 / displayOrder / INT / NOT NULL

- **`SurveyOption`: 설문 선택지 테이블**
    - 관계
        - **`SurveyQuestion`**(N:1)
        - **`SurveyOptionRestriction`**(1:N, source option)
        - **`SurveyOptionRestriction`**(1:N, restricted option)
        - **`HojumoneyRecommenndation`**(Logical N:M)
    - 필드
        - **설문 선택지 ID / surveyOptionId / BIGINT / NOT NULL (PK)**
        - **설문 문항 ID / surveyQuestionId / BIGINT / NOT NULL (FK)**
        - 선택지 내용 / content / VARCHAR(255) / NOT NULL
        - 로직 코드 / logicCode / ENUM / NOT NULL
        - 선택지 설명 / description / JSONB / NOT NULL / 예:
            ```json
            [
              {
                "indicatorName": "시가총액 상위 50위",
                "indicatorDescription": "시장의 신뢰를 받는 대형주로서, 하락장에서 매수 대기 자금이 풍부해 주가 방어력이 뛰어나요."
              },
              {
                "indicatorName": "부채비율 100% 이하",
                "indicatorDescription": "부채가 적기에 금리가 오르거나 자금줄이 막히는 위기 상황에서도 파산 위험 없이 생존할 수 있어요."
              }
            ]
            ```
        - 표시 순서 / displayOrder / INT / NOT NULL

- **`SurveyOptionRestriction`: 설문 선택지 제한 매핑 테이블**
    - 관계
        - **`SurveyOption`**(N:1, sourceOption)
        - **`SurveyOption`**(N:1, restrictedOption)
    - 필드
        - **제한 매핑 ID / restrictionId / BIGINT / NOT NULL (PK)**
        - **기준 선택지 ID / sourceOptionId / BIGINT / NOT NULL (FK)**
        - **제한 선택지 ID / restrictedOptionId / BIGINT / NOT NULL (FK)**
    - 제약
        - `(sourceOptionId, restrictedOptionId)` UNIQUE

- **`HojumoneyRecommendation`: 호주머니 추천 결과 테이블**
    - 관계
        - **`Recommendation`**(1:1)
        - **`SurveyOption`**(Logical N:M)
    - 필드
        - **호주머니 추천 ID / hojumoneyRecommendationId / BIGINT / NOT NULL (PK)**
        - **추천 ID / recommendationId / BIGINT / NOT NULL (FK)**
        - 투자 목적 / investmentPurpose / ENUM / NOT NULL
        - 위험 성향 / riskProfile / ENUM / NOT NULL
        - 투자 호흡 / investmentHorizon / ENUM / NOT NULL
        - 선택된 옵션 리스트 / selectedOptionIds / JSONB / NOT NULL / 예: [1, 3, 4]
        - 페르소나명 / personaName / VARCHAR(100) / NOT NULL
        - 페르소나 설명 / personaDescription / TEXT / NOT NULL

- **`Recommendation`: 추천 결과 테이블**
    - 관계
        - **`User`**(N : 1)
        - **`HojumoneyRecommendation`**(1 : 1)
        - **`MasterRecommendation`**(1 : 1)
        - **`RecommendationStock`**(1 : N)
    - 필드
        - **추천 ID / recommendationId / BIGINT / NOT NULL (PK)**
        - **사용자 ID / userId / BIGINT / NOT NULL (FK)**
        - 추천 타입 / recommendationType / ENUM / NOT NULL
        - 생성일시 / createdAt / TIMESTAMP / NOT NULL
        - 수정일시 / updatedAt / TIMESTAMP / NOT NULL
    - 인덱스
        - `(userId, recommendationType, createdAt)`
    - 비고
        - 현재 오늘의 호주머니 추천은 Redis 뉴스 분석 결과를 정렬과 태그에만 반영하며, `NewsAnalysis` FK는 저장하지 않는다.
        - 저장된 오늘의 호주머니 추천은 로그인 사용자의 최신 결과를 조회하는 API에서 다시 재조립한다.

- **`RecommendationStock`: 추천 종목 테이블 (추천 결과-종목 매핑 테이블)**
    - 관계
        - **`Recommendation`**(N:1)
        - **`Stock`**(N:1)
        - **`RecommendationStockTag`**(1:N)
    - 필드
        - **추천 종목 ID / recommendationStockId / BIGINT / NOT NULL (PK)**
        - **추천 ID / recommendationId / BIGINT / NOT NULL (FK)**
        - **종목 ID / stockId / BIGINT  / NOT NULL (FK)**
        - 추천 순위 / rank / INTEGER / NOT NULL
        - 만족 조건 수 / matchedConditionCount / INTEGER / NOT NULL
        - 정렬 지표 키 / sortMetricKey / VARCHAR(50) / NOT NULL
        - 정렬 지표 값 / sortMetricValue / DECIMAL / NULL
        - 현재가 / currentPrice / DECIMAL / NULL
        - 등락률 / changeRate / DECIMAL / NULL
