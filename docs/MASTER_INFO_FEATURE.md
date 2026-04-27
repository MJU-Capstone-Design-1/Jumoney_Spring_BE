# Master Info Feature Specification

본 문서는 투자 거장(워렌 버핏, 피터 린치 등) 소개 및 포트폴리오 소개의 비즈니스 로직을 정의합니다.

## AI Assistant Context (개발 원칙)
1. **N+1 쿼리 방지**: 거장(`Master`) 엔티티 조회 시 연관된 컬렉션(`Tag`, `MasterCase`, `MasterPortfolioStock`)을 한 번의 `Fetch Join`으로 모두 끌어오면 카테시안 곱(Cartesian Product) 에러나 메모리 초과가 발생한다.
    - 반드시 **다중 쿼리로 분리**하거나 `hibernate.default_batch_fetch_size`를 활용하여 최적화하라.
2. **데이터 가공 (Stream API 활용)**: '분야별(섹터) 차트' 데이터 추출을 위해 별도의 `GROUP BY` DB 쿼리를 날리지 마라. 포트폴리오 주식 리스트 조회 시 가져온 `MasterPortfolioStock` 엔티티 리스트를 **Java Stream API를 활용하여 애플리케이션 레벨에서 가공**하여 응답하라.
3. **엔드포인트 분리**: 화면 뎁스(Depth)에 따라 [거장 요약 프로필], [포트폴리오/차트], [투자 사례] API를 분리 설계하라.

---

## 1. 거장 리스트 및 요약 프로필 조회
- **기능**: 4명의 투자 거장 중 한 명을 선택했을 때 노출되는 기본 프로필(이름, 명언, 태그, 핵심 투자 철학) 조회.
- **사용 테이블**: `Master`, `MasterTag`, `Tag`
- **로직**:
    1. `Master` 테이블에서 특정 `masterId`에 해당하는 데이터(`masterName`, `quote`, `philosophyTitle`, `philosophyDescription`) 조회.
    2. 해당 거장과 매핑된 `MasterTag` -> `Tag`를 JOIN하여 해시태그 리스트를 함께 반환.

---

## 2. 거장 포트폴리오 차트 및 비율 조회
- **기능**: 거장의 [자세히 보기] 탭 진입 시 노출되는 '분야별 차트', '투자 기업 비율', '기준 연도' 정보 조회.
- **사용 테이블**: `Master`, `MasterPortfolioStock`
- **로직**:
    1. **기준일**: `Master` 테이블의 `portfolioBasePeriod` (예: "2023년 4분기 기준") 반환.
    2. **주식 리스트 및 비중**: `MasterPortfolioStock` 테이블에서 해당 `masterId`의 종목(`stockName`), 섹터(`sector`), 비중(`weight`) 리스트 조회 (비중 기준 내림차순 정렬).
    3. **분야별 차트 데이터 (Aggregating)**:
        - 2번 로직에서 조회된 엔티티 리스트를 Java 로직에서 `sector` 기준으로 그룹화하고, 각 섹터의 `weight`를 합산하여 차트 렌더링용 DTO로 반환.

---

## 3. 거장 대표 투자 사례 조회
- **기능**: 거장이 과거에 투자했던 대표적인 사례 스토리 조회. (현재는 거장 별로 1가지 사례만 존재)
- **사용 테이블**: `MasterCase`
- **로직**:
    1. `MasterCase` 테이블에서 해당 `masterId`를 가진 투자 사례를 단건(LIMIT 1)으로 조회.
    2. 조회된 단건의 `stockName`, `sector`, `investmentPeriod`, `investmentResult`, `title`, `description` 필드를 DTO 객체 형태로 반환.