# 💰 JUMONEY(SPRING BE) 가이드 V1

이 가이드는 '주머니(JUMONEY)' 프로젝트의 백엔드 아키텍처 및 스프링 애플리케이션의 구현 지침을 담고 있습니다.
(노드 서버는 해당 가이드에서 다루지 않습니다.)

## 1. 프로젝트 개요
* **서비스 명**: 주머니 (JUMONEY)
* **주요 기능**: 거장 4인(워렌 버핏, 피터 린치, 레이 달리오, 윌리엄 오닐)의 투자 지표 기반 종목 추천 및 모의 투자 서비스입니다.
* **개발 목표**: 1GB RAM 프리티어 환경에서 안정적인 실시간 시세 처리 및 비즈니스 로직을 구현하는 것입니다.

---

## 2. 기술 스택 (Tech Stack)
* **Framework**: Java 17 / Spring Boot 4.0.5 (MVC Pattern)
* **Database**: AWS RDS (MySQL 8.0)
* **Caching & Messaging**: Redis (Pub/Sub Broker)
* **Infrastructure**: AWS EC2 (t2.micro), Docker, Nginx, Certbot(SSL)
* **External API**: 한국투자증권(KIS) REST API

---

## 3. 시스템 아키텍처 (Architecture)

### 🛰️ 데이터 파이프라인
1.  **시세 수집**: `Node.js` 서버가 KIS WebSocket에 상시 연결되어 실시간 데이터를 수집합니다.
2.  **데이터 중계**: 수집된 데이터는 `Redis Pub/Sub` 채널로 발행(Publish)됩니다.
3.  **비즈니스 처리**: `Spring Boot` 서버가 Redis 채널을 구독(Subscribe)하여 추천 가중치 계산 및 비즈니스 로직을 수행합니다.
4.  **사용자 전송**: 실시간 시세 푸시는 `Node.js`가 전담하며, `Spring Boot`는 REST API 응답에 집중합니다.

### 🌐 인프라 구성
* **Nginx (Gateway)**: 포트 80/443 요청을 받아 `/api`는 Spring(8080)으로, 기타 경로는 Node(3000)로 분기합니다.
* **Swap Memory**: EC2 1GB RAM 부족을 해결하기 위해 **2GB Swap 파일**을 활성화했습니다.

---

## 4. 핵심 구현 로직

### ✅ 실시간 추천 가중치 공식
유저 설문 데이터와 실시간 시장 지표를 결합하여 추천 점수를 산출합니다.
$Score = 추후 정의$

### ✅ Spring Batch 작업
* **매일 04:00 AM**: KIS 재무 API를 호출하여 거장별 종목 필터링 및 `investor_recommendations` 테이블을 갱신합니다.

---

## 5. 환경 변수 관리 (.env)
보안을 위해 모든 민감 정보는 환경 변수로 관리하며, `docker-compose.yml`과 `application.yml`에서 참조합니다.

* `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`: RDS 연결 정보
* `REDIS_PASSWORD`: Redis 보안 접속 비밀번호
* `KIS_APP_KEY`, `KIS_APP_SECRET`: 한국투자증권 API 키

---

## 6. 개발 로드맵 (Roadmap)
1.  **Phase 1**: 프로젝트 초기 세팅 및 로컬 인프라 작업
2.  **Phase 2**: EC2 인스턴스 생성 및 RDS(MySQL) 생성, Redis 컨테이너 실행 등 인프라 작업
3.  **Phase 3**: Redis Pub/Sub 리스너 구현
4.  **Phase 4**: Node.js ↔ Redis ↔ Spring 서버 간 연동 파이프라인 검증
5.  **Phase 5**: Nginx 설치 및 리버스 프록시
6.  **Phase 6**: 도메인 연결 및 SSL(HTTPS) 적용 
7.  **Phase 7**: KIS REST API 연동(WebClient) -> REST API 및 추천 로직 개발 + 배치 작업(Spring Batch)
8.  **Phase 8**: 프론트 연동
9.  **Phase 9**: CI/CD 구축