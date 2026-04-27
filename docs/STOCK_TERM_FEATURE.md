# Stock Term Feature Specification

본 문서는 주식 용어 사전의 핵심 기능(조회, 스크랩, 학습 기록)에 대한 비즈니스 로직을 정의합니다.

## AI Assistant Context (개발 원칙)
1. **개인화 데이터 분리**: `StockTerm`은 정적인 마스터 데이터이다. 유저별 '스크랩'과 '읽음(학습)' 상태는 반드시 별도의 매핑 테이블(`StockTermScrap`, `StockTermLearning`)을 참조하여 처리하라.
2. **신규 테이블 생성**: 유저의 용어 학습 여부를 기록하기 위해 아래 구조의 테이블을 엔티티로 추가하라.
    - **`StockTermLearning`**: (PK) learningId, (FK) userId, (FK) termId, (DateTime) createdAt

---

## 1. 카테고리별 용어 리스트 조회
- **기능**: 특정 카테고리에 속한 전체 용어 목록과 유저의 개인화 상태(스크랩/학습)를 반환.
- **로직**:
    1. `StockTerm` 테이블에서 선택된 `category`로 필터링.
    2. `StockTermScrap`, `StockTermLearning` 테이블을 **LEFT JOIN** 하여 유저별 상태값 매핑.
    3. 별도의 페이징 없이 전체 리스트 반환.

---

## 2. 스크랩한 용어 리스트 조회
- **기능**: 유저가 즐겨찾기(스크랩)한 용어들만 모아서 조회.
- **로직**:
    1. `StockTermScrap` 테이블에서 현재 유저(`userId`)의 데이터를 기준점으로 삼음.
    2. `StockTerm`과 **INNER JOIN** 하여 용어 정보를 가져오고, `StockTermLearning`과 **LEFT JOIN** 하여 학습 여부 확인.
    3. 유저가 스크랩한 순서(최신순)로 정렬하여 반환.

---

## 3. 용어 상세 조회 및 학습 기록
- **기능**: 용어의 상세 설명을 조회하고, 진입과 동시에 '학습 완료' 상태로 갱신.
- **로직**:
    1. **상세 정보**: `termId`로 용어명, 상세 설명, 스크랩 여부 조회.
    2. **학습 처리 (Core)**: 상세 페이지 진입 시 `StockTermLearning` 테이블에 `userId`와 `termId`를 **UPSERT**. (중복 기록 방지)
    3. **내비게이션**: 동일 카테고리 내에서 현재 `termId` 기준 이전/다음 용어의 ID를 함께 반환.