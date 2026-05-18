# Stock Term Feature Specification

본 문서는 주식 용어 사전의 구현 완료 API와 비즈니스 로직을 정의합니다.

---

## 1. 카테고리 목록 조회

- **API**: `GET /api/stock-terms/categories`
- **기능**: 주식 용어 카테고리 목록을 조회.
- **응답 필드**: `categoryId`, `categoryName`

---

## 2. 카테고리별 용어 리스트 조회

- **API**: `GET /api/stock-terms/categories/{categoryId}/terms`
- **기능**: 특정 카테고리의 용어 목록과 개인화 상태(스크랩/학습)를 조회.
- **응답 구조**:
    - 상단: `categoryId`, `categoryName`
    - 목록 항목: `termId`, `termName`, `isScrapped`, `isLearned`
- **로직**:
    1. `StockTerm`에서 `category` 기준 조회.
    2. `StockTermScrap`, `StockTermLearning`을 사용자 기준으로 조회해 상태값 매핑.
    3. 용어 목록은 `termId` 오름차순으로 반환.

---

## 3. 용어 상세 조회 및 학습 처리

- **API**: `GET /api/stock-terms/terms/{termId}`
- **기능**: 용어 상세를 조회하고, 조회 시 해당 용어를 학습한 것으로 기록.
- **응답 필드**: `termId`, `categoryId`, `categoryName`, `termName`, `description`, `isScrapped`, `isLearned`
- **로직**:
    1. `termId`로 `StockTerm` 조회.
    2. `StockTermScrap`에서 스크랩 여부 확인.
    3. `StockTermLearning`에 `(userId, termId)` 기준으로 학습 이력 존재 여부 확인 후 미존재 시 생성.
    4. 동시 요청으로 인한 중복 생성은 유니크 제약 기반으로 무시.

---

## 4. 용어 스크랩 토글

- **API**: `POST /api/stock-terms/terms/{termId}/scrap`
- **기능**: 용어 스크랩 상태를 토글.
- **응답 필드**: `termId`, `isScrapped`
- **로직**:
    1. `(userId, termId)` 스크랩 존재 시 삭제(해제).
    2. 미존재 시 생성(스크랩).

---

## 5. 스크랩한 용어 리스트 조회

- **API**: `GET /api/stock-terms/scraps`
- **기능**: 로그인 사용자의 스크랩 용어 목록을 최신순으로 조회.
- **응답 필드**: `termId`, `categoryName`, `termName`, `isLearned`
- **로직**:
    1. `StockTermScrap`을 사용자 기준 `createdAt DESC`로 조회.
    2. `StockTermLearning`과 매핑하여 학습 여부를 함께 반환.
