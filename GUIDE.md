# 💰 JUMONEY(SPRING BE) 가이드 V1

이 가이드는 '주머니(JUMONEY)' 프로젝트의 백엔드 아키텍처 및 스프링 애플리케이션의 구현 지침을 담고 있습니다.
(노드 서버는 해당 가이드에서 다루지 않습니다.)

## 1. 프로젝트 개요
* **서비스 명**: 주머니 (JUMONEY)
* **주요 기능**: 설문 기반 종목 추천, 거장의 투자 지표 기반 종목 추천 및 모의 투자 서비스입니다.
* **개발 목표**: 1GB RAM 프리티어 환경에서 안정적인 실시간 시세 처리 및 비즈니스 로직을 구현하는 것입니다.

---

## 2. 기술 스택 (Tech Stack)
* **Language**: Java 17
* **Framework**: Spring Boot 4.0.3 (MVC Pattern)
* **Database**: PostgreSQL (Main), Redis (Cache/Real-time)
* **ORM**: Spring Data JPA, QueryDSL
* **Infrastructure**: AWS EC2 (t2.micro), Docker, Nginx, Certbot(SSL)
* **Architecture**: Node.js(실시간 데이터) + Spring Boot(비즈니스 로직) MSA 구조
* **External API**: 한국투자증권(KIS) REST API

---

## 3. 시스템 아키텍처 (Architecture)

### 🛰️ 데이터 파이프라인
1.  **시세 수집**: `Node.js` 서버가 KIS WebSocket에 상시 연결되어 실시간 데이터를 수집합니다.
2.  **데이터 중계**: `Node.js`에서 수집된 실시간 틱 데이터는 **Redis Stream** 및 **ZSET**에 적재됩니다.
3.  **비즈니스 처리**: 
 - `Spring Boot` 서버는 KIS WebSocket API를 직접 호출하지 않습니다.
 - `Spring Boot` 서버는 KIS REST API만 직접 호출합니다. (WebClient 논블로킹 연동)
 - `Spring Boot` 서버는 배치를 통해 사전 적재해둔 DB의 지표 데이터와 Redis(ZSET)의 인메모리 데이터로 추천 알고리즘, 차트 생성, 수익률 계산 등을 수행합니다.
4.  **사용자 전송**: 프론트엔드로의 실시간 시세 푸시는 `Node.js`가 전담하며, `Spring Boot`는 REST API 응답에 집중합니다.

### 🌐 인프라 구성
* **Nginx (Gateway)**: 요청 경로에 따라 Spring Boot API 서버와 Node.js 실시간 서버로 분기합니다.
* **Swap Memory**: EC2 1GB RAM 부족을 완화하기 위해 Swap 파일(2GB)을 활성화했습니다.
* **Internal DB**: AWS RDS 비용 절감을 위해 EC2 내부 Docker 컨테이너로 PostgreSQL과 Redis를 직접 구축하여 운영합니다.
* **Local Realtime Feed Access**: 로컬 Spring 개발 환경은 보안 터널을 통해 EC2 내부 Redis의 실시간 데이터를 읽을 수 있습니다. 앱 내부 캐시 Redis와 실시간 피드 Redis는 설정과 코드에서 역할을 분리합니다.
  - 로컬 환경 변수 예시: `REALTIME_REDIS_HOST=<local-forward-host>`, `REALTIME_REDIS_PORT=<local-forward-port>`, `REALTIME_REDIS_PASSWORD=<redis-password>`
  - 로컬 검증 API: `/api/local/realtime-redis/value`, `/api/local/realtime-redis/hash`, `/api/local/realtime-redis/zset`

---

## 4. 핵심 구현 로직
### ✅ 데이터베이스
- `docs/ERD_SPEC.md` 참고

### ✅ 비즈니스 로직
- `docs/` 폴더 내 도메인별 기능 명세서(.md) 참고
  - `HOME_FEATURE.md` (홈/랭킹)
  - `STOCK_TERM_FEATURE.md` (주식 용어)
  - `MASTER_INFO_FEATURE.md` (투자 거장 소개)
  - `MOCK_INVESTMENT_FEATURE.md` (모의투자/주문)
  - `STOCK_DETAIL_FEATURE.md` (기업 상세/차트)

### ✅ 종목 추천 알고리즘
- `docs/RECOMMEND_LOGIC.md` 참고
- 오늘의 호주머니: 뉴스 분석(llm) + 설문 기반 지표 필터링
- 거장의 선택: 뉴스 분석(llm) + 거장별 투자 지표 필터링

### ✅ Spring Batch & Scheduler 로직
시스템 부하 분산을 위해 데이터 수집 주기를 분리합니다.
- **[주간/일간 배치]**: 무거운 재무지표(`StockIndicator`) 수집 및 HTS 조건검색 필터링 결과 저장.
- **[장 마감 배치]**: 매일 16:00 이후 당일 확정된 일/주/월/년봉 차트 데이터를 DB(`StockPrice`)에 이관.
- **[1분 스케줄러]**: 장 중 1분마다 확정된 1분봉 데이터를 수집하여 Redis ZSET에 갱신.
- **[1시간 스케줄러]**: 모의투자 전체 사용자의 수익률을 계산하여 랭킹(`UserRanking`) DB 갱신.

---

## 5. 환경 변수 관리 (.env)
보안을 위해 모든 민감 정보는 환경 변수로 관리하며, `docker-compose.yml`과 `application.yml`에서 참조합니다.

* `SPRING_PROFILES_ACTIVE`: 실행 프로필 (`local` 또는 `prod`)
* `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_URL`: PostgreSQL 연결 정보
* `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`: Redis 접속 정보
* `REALTIME_REDIS_HOST`, `REALTIME_REDIS_PORT`, `REALTIME_REDIS_PASSWORD`: Node 서버가 적재한 실시간 Redis 데이터 조회용 접속 정보
* `KIS_APP_KEY`, `KIS_APP_SECRET`: 한국투자증권 API 키
* `JWT_SECRET_KEY`: JWT 서명 키
* `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`: 카카오 로그인 설정
* `SPRING_JPA_HIBERNATE_DDL_AUTO`: 초기 연동 단계에서는 `update`, 데이터 보존 단계에서는 Flyway 도입 후 `validate` 권장

### 프로필과 배포
- `local`: 로컬 개발용 프로필입니다. JPA `ddl-auto=update`, SQL 로그 출력, 로컬 PostgreSQL/Redis를 기본으로 사용합니다.
- `prod`: EC2 운영용 프로필입니다. 환경 변수 기반 DB/Redis/KIS/Kakao 설정을 사용하며, 운영 쿠키는 기본적으로 `Secure`와 `SameSite=None`을 사용합니다.
- EC2 프리티어에서는 Gradle/Docker 빌드가 메모리 부족을 유발할 수 있으므로 Spring 이미지는 로컬 PC 또는 CI에서 빌드해 Container Registry에 push하고, EC2에서는 pull만 수행합니다.
- 운영 DB 스키마는 현재 초기 연동 단계에서 `ddl-auto=update`를 허용하지만, 보존해야 하는 데이터가 생기면 Flyway 마이그레이션으로 전환합니다.

---

## 6. 개발 로드맵 (Roadmap)
1. **Phase 1**: 프로젝트 초기 세팅 및 로컬 인프라 작업
2. **Phase 2**: EC2 인스턴스 생성, Docker(PostgreSQL, Redis) 컨테이너 실행 등 인프라 작업
3. **Phase 3**: Node.js ↔ Redis(Stream/ZSET) ↔ Spring 서버 간 데이터 파이프라인 연동
4. **Phase 4**: Nginx 설치 및 리버스 프록시
5. **Phase 5**: 도메인 연결 및 SSL(HTTPS) 적용
6. **Phase 6**: KIS REST API 연동(WebClient) -> REST API 및 추천 로직 개발 + Batch/Scheduler 작업
7. **Phase 7**: 프론트엔드 API 연동
8. **Phase 8**: Github Actions 등을 활용한 CI/CD 구축
