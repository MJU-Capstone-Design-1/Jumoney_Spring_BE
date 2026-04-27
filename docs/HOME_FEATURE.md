# Home Feature Specification

본 문서는 홈(Main) 화면에 노출되는 4가지 핵심 기능의 비즈니스 로직과 API/DB 연동 명세를 정의합니다.

## AI Assistant Context
- 시스템의 복잡도를 낮추기 위해 과도한 캐싱(Redis)을 지양하고 **DB 중심의 아키텍처**를 구성한다.
- 랭킹 시스템은 실시간이 아닌 **1시간 단위 Spring Scheduler 배치**를 통해 `UserRanking` DB 테이블을 갱신하는 방식을 사용한다.
- 실시간 웹소켓 데이터(Node.js 담당)는 Spring Boot 연동 명세에서 제외하며, Spring Boot는 철저히 DB(`StockPrice`)에 적재된 데이터를 가공하여 응답한다.

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

## 4. 모의 투자 자산 및 수익률 (My Assets & Top Stock Chart)
- **기능**: 로그인한 사용자의 총 자산, 수익률 및 투자 비중이 가장 높은 대표 기업의 일봉 차트 노출.
- **사용 테이블**: `Account`, `Portfolio`, `Stock`, `StockPrice`
- **로직**:
    1. **자산 조회**: `Account` 테이블에서 해당 유저의 총 자산 및 수익률 조회.
    2. **대표 기업 선정**: `Portfolio` 테이블에서 `totalPurchaseAmount`가 가장 큰 1개 종목 추출.
    3. **차트 데이터 조회**:
        - `StockPrice` 테이블에서 해당 종목의 `intervalType = 'DAY'` 인 최근 3개월 치 종가(`closePrice`)와 기준 일자(`baseTime`) 조회.
- **성능 최적화 (Lazy Loading)**:
    - 자산/수익률 요약 정보는 홈 진입 시 즉시 반환하고, 차트 데이터 배열은 클라이언트에서 비동기(AJAX)로 지연 로딩하도록 엔드포인트를 분리 설계.
    - 예: 요약 정보는 /api/home/assets, 차트 데이터는 /api/home/assets/chart 로 분리하여 컨트롤러 구현

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