---

### 비즈니스 로직 구현

- **매수/매도 거래 정합성 (동시성 제어)**: 동일한 사용자 계정으로 짧은 시간 안에 여러 번 매수/매도 요청을 보낼 경우 **동시성 문제(잔액과 수량 계산에 경쟁 상태)**  발생 가능 → 거래 시작 시 사용자 계정에 ⚠️ **비관적 락(Pessimistic Lock)**을 걸어 사용자의 **모든 거래 과정을 직렬화**
    - 비관적 락: 트랜잭션이 커밋되거나 롤백될 때까지 다른 트랜잭션 접근 대기
    - 스프링 데이터 JPA에서는 **레포지토리 메서드에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 추가**하여 적용 → 데이터베이스 쿼리에 `FOR UPDATE`가 붙음
- **차트 구현 (분봉)**:
    - **30분마다 KIS REST API 호출로 DB에 확정 1분봉 데이터**를 채우고, ⚠️ **최근 30분 이내는 Node 서버가 실시간으로 Redis에 적재한 미확정 1분봉 데이터**로 채우기
    - 데이터 정합성을 위해 **최근 2분은 DB 확정 저장에서 제외** 
    (해당 KIS 분봉 API 문서에서 **첫 체결 전 이전 분 거래량일 수 있음을 경고**하고 있고, **호출 지연**도 존재하기 때문)
        - KIS 분봉 동기화 스케줄러는 정각/30분 정각보다 2분 늦게 실행
        - KIS 분봉 동기화는 최근 2분은 제외
        - ex) 14:32에 동기화 실행하여 14:30까지의 데이터만 DB에 저장하고, 14:31 이후 구간은 Redis의 미확정 데이터로 채우기

**TODO**

- 복식부기(Double-Entry Bookkeeping) 원장 설계 도입: **단순 잔액 수정** 방식 → 복식부기 방식
    - 모든 돈의 흐름을 원장(Ledger)에 차변(Debits, 나간 돈)과 대변(Credits, 들어온 돈)으로 나눠 기록
    - 계좌 잔액과 거래 이력을 대조하는 정산 로직 생성 가능
- 주문 멱등성 키 도입
    - (네트워크 타임아웃, 재시도 등으로 인해) 같은 주문이 여러 번 들어가는 오류 방지

---

### 추천 기능 구현

- **추천 기능 공통 구조**
    - **오늘의 호주머니**와 **거장의 선택** 두 가지 추천 기능 제공
    - 추천 기능 호출 시, KIS API를 호출하지 않고 **배치로 미리 적재한 데이터를 기반으로 추천**
    - 추천 종목과 추천 태그는 스냅샷 형태로 저장
      (**`RecommendationStock`, `RecommendationStockTag`**)
- **뉴스 분석 결과 반영**
    - Node 뉴스 파이프라인이 **Redis `news:analysis:today`에 저장한 당일 뉴스 분석
      결과**를 조회
    - 추천 로직에 **호재 섹터(`goodSectors`)를 활용**
    - 추천 종목의 섹터가 **`goodSectors`**에 해당하면 **`goodSectorTags`**에 섹터 태그 추가

**[오늘의 호주머니]**

- 사용자의 3단계 설문을 기반으로 추천
    1. **투자 목적** → **`StockIndicator`** (사전 적재 지표)
    2. **위험 감수 수준** → **`HtsStock`** (HTS 조건검색 결과)
    3. **투자 기간** → 체결강도, 거래대금, EPS 성장률, ROE (최종 정렬 기준으로 사용)
- 정렬 우선 순위
    1. 선택지 1 + 선택지 2 + 호재 섹터
    2. 선택지 1 + 선택지 2
    3. 선택지 둘 중 하나만 만족 + 호재 섹터
    4. 선택지 둘 중 하나만 만족

**[거장의 선택]**

- 사용자가 선택한 투자 거장과 세부 조건을 기반으로 추천
- 각 거장의 실제 투자 방식을 세부 조건으로 변환하여 구현

![image.png](attachment:e2b023de-e491-4ad5-85b6-6aa72712481e:image.png)

![image.png](attachment:09fd60cc-8d26-4cba-b6aa-3ac6657da8fa:image.png)

---

### **차트 데이터 적재 및 차트 구현**

- **핵심 원칙**
    - 차트 데이터는 적재량이 많기 때문에 **Spring Batch 기반**으로 처리
    - ⚠️ **확정 데이터 / 미확정 데이터 분리**:  차트 데이터는 **DB 확정 캔들**과 **Redis 미확정 캔들**을 분리하여 관리
        - **Spring 서버**는 **KIS REST API로 DB에 확정 캔들**을 저장 (**`stock_candles`** 테이블에 저장)
        - **Node 서버**는 **KIS WebSocket으로** 체결 데이터를 실시간으로 집계해 **Redis에 미확정 캔들(1분봉)** 저장
    - Spring 차트 조회 API는 **DB 확정 캔들과 Redis 미확정 캔들을 병합**해 응답
    - KRX의 장마감 동시 호가 구간 (15:20~15:29)은 체결 분봉이 호출되지 않기에 15:19를 기준으로 보정
- **분봉 데이터**
    1. 분봉 확정 데이터
        - Spring 스케줄러가 **정각/30분 기준 2분 뒤에 KIS REST 분봉 API 호출하여 적재
          (09:02, 09:32, 10:02 … 15:32)**
        - 15:40에는 장 마감 보정 동기화를 한번 더 수행
    2. 분봉 미확정 데이터
        - **`stock:minute-candles:{code}`** : **최근 미확정 1분봉 배열** 저장
            - Sorted Set
            - 차트 병합에 사용됨
        - **`stock:latest:{code}`**: 현재 진행 중인 **최신 분봉 1개** 저장
            - String
            - 현재가, 등락률, 최신 상태 조회에 사용
- **차트 기간별 데이터**
    - 1일 차트 → **1분봉**
        - **확정 1분봉(DB의 `isFinal=true`)**과 **미확정 1분봉(Redis의 `isFinal=false`)** 병합
        - 겹치는 경우 확정 데이터를 우선
    - 1주 차트 → **30분봉**
        - 확정 30분봉 사용
        - 확정 30분봉은 1분봉 확정 데이터가 모두 준비된 뒤 집계 저장
    - 3개월 차트 → **일봉**
    - 1년 차트 → **일봉**
    - 5년 차트 → **주봉**

---

### **배치 구현**

1. **초기에는 Spring `@Service` 기반의 수동 배치 서비스를 구현 (**StockIndicatorBatchService 클래스)
- **`syncAll` 메서드: 전체를 총괄하는 메서드**
    - **전체 조회:** `stockRepository.findAll()`을 통해 DB에 등록된 **전체 종목 조회**
    - `for`문 안에서 각 종목별로 **`sync()` 메서드를 호출**
    - **에러 격리**: 반복문 안에서 try-catch로 묶어 특정 종목에서 **KIS API 에러가 발생해도 에러 로그만 남기고 전체 배치 작업 계속 진행**
    - ⚠️ **`@Transactional`제거**: **배치 작업 내내 DB 커넥션을 점유하는 문제**를 제거
- **`sync` 메서드: 단일 종목 처리 메서드** ⚠️ **(수집 → 가공 → 검증 → 저장)**
    - **데이터 수집 (Data Fetching): `kisApiClient`를 통해 KIS API**(현재가, 재무비율, 손익계산서, 배당, 신용잔고, 투자자동향 등)를 **순차적으로 호출**
    - **내부적으로 `KisRateLimiter`가 작동하여 250ms씩 대기**하여 API 초당 호출 제한을 방어
    - **파생 지표 계산 (Data Processing):** API 호출로 얻을 수 없는 지표(배당 성향, 20일 합산 등)는 직접 계산하여 저장
    - **필수 값 검증 (Validation):** 필수적인 지표들이 비어있는지 검사
    - **데이터베이스 저장 (Upsert):** 지표가 DB에 있는지 조회하고 있으면 새로운 데이터로 덮어쓰기, 없으면 새로 객체를 생성
1. **Spring Batch를 도입하여 재구현**
- ⚠️ 기존 방식의 단점 개선을 위해 재구현 (현재 규모에서는 동작했지만 차트 데이터와 같은 적재량이 큰 배치에서는 문제 발생 가능성이 있다고 판단)
    - **findAll()로 인한 메모리 과부화**
    - **트랜잭션 효율적 관리 어려움** (배치 작업 내내 DB 커넥션을 점유하는 문제로 인하여 트랜잭션 제거)
    - **자동 실패 복구 불가**
    - skip이나 retry와 같은 예외 처리가 try-catch 문으로 구현
- ⚠️기존 코드를 재활용하여 기존 지표/HTS 서비스를 호출하는 **Tasklet 기반 Job (Tasklet 방식)**으로 구현
- 수동 실행 API (smoke API) 또한 서비스 직접 호출 대신 JobLauncher를 호출
- ⚠️대량의 데이터인 차트 데이터 적재는 **chunk 방식**으로 구현
- KIS 호출 실패 대비 retry 로직 구현
- ⚠️**트랜잭션 개선**
    - 기존 문제(**`@Transactional`**를 전체에 추가했을 때): **전체 배치 작업 동안 DB 트랜잭션 유지**

        <aside>

      **Batch Step 트랜잭션 시작**
      -> 전체 종목 KIS API 호출
      -> 전체 종목 저장
      **Batch Step 트랜잭션 커밋**

        </aside>

    - 개선점: **저장 시점에만 트랜잭션 적용** (종목 저장 시점에만 트랜잭션을 적용하여 한 종목 저장이 성공하면 바로 커밋)

        <aside>

      Batch Step 실행
      -> tasklet 자체는 트랜잭션 없음

      -> 종목 A KIS API 호출
      -> 종목 A 지표 계산
      -> **종목 A 저장만 REQUIRES_NEW 트랜잭션 시작/커밋**

      -> 종목 B KIS API 호출
      -> 종목 B 지표 계산
      -> **종목 B 저장만 REQUIRES_NEW 트랜잭션 시작/커밋**

        </aside>

    - 구현
        - Spring 프레임워크의 **트랜잭션 전파 속성(Transaction Propagation)** 사용
          → 메서드가 다른 메서드를 호출할 때, DB 커넥션(트랜잭션)을 어떻게 이어받을지 결정하는 규칙
            - **REQUIRED(기본값)** → 기존 트랜잭션이 있으면 참여하고, 없으면 새로 시작
            - **REQUIRES_NEW** → 새로운 트랜잭션 생성
            - **PROPAGATION_NOT_SUPPORTED** → 트랜잭션 없이 코드를 실행 (진행 중인 트랜잭션은 잠시 멈추기)
        - **Batch tasklet 트랜잭션 끄기** → StockDataBatchJobConfig 클래스에서 아래 메서드 실행
          (**PROPAGATION_NOT_SUPPORTED** 사용)

            ```java
            private TransactionAttribute nonTransactionalTaskletAttribute() {
            		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();
                // **PROPAGATION_NOT_SUPPORTED -> 트랜잭션 없이 코드 실행**
                transactionAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
                return transactionAttribute;
            }
            
            ```

        - StockIndicatorBatchService 클래스에서 KIS API 호출 메서드, 지표 값계산 메서드는 기존과 동일하게 트랜잭션 없이 수행
        - StockIndicatorBatchService 클래스에서 **`upsert` 메서드 호출할 때만 트랜잭션 적용**
          (**REQUIRES_NEW** 사용)

            ```java
            // StockIndicatorPersistenceService 클래스 
            // REQUIRES_NEW -> 새로운 트랜잭션 생성
            @Transactional(propagation = Propagation.REQUIRES_NEW)
                public void upsert(StockIndicatorMetrics metrics) {
                    StockIndicator stockIndicator = stockIndicatorRepository.findByStockAndBaseTime(metrics.stock(), metrics.baseTime())
                            .orElseGet(() -> StockIndicator.create(
                                    metrics.stock(),
                                    // ~ 각 지표 필드
                            ));
            
                    stockIndicator.updateMetrics(
                            metrics.marketCap(),
                            // ~ 각 지표 필드
                    );
            
                    stockIndicatorRepository.save(stockIndicator);
                }
            ```


---

### **스케줄러 구현**

- **스케줄러**
    - 스케줄러 주기
        - StockIndicator 배치 → **화~토 6시** 배치 작업 수행
        - HtsCondition 배치 → **화~토 6시 30분** 배치 작업 수행
    - 구현
        - StockDataBatchScheduler 클래스
            - **`@Scheduled`메서드**: 명시된 시간에 메서드 자동 실행
                - **`cron` 속성:** 배치가 실행될 시간을 지정
                - **`zone` 속성:** 한국 시간 기준으로 cron 계산

                ```json
                @Scheduled(
                        cron = "${kis.batch.stock-indicator.cron:0 0 6 * * TUE-SAT}", 
                        zone = "${kis.batch.zone-id:Asia/Seoul}"
                )
                public void runStockIndicatorBatch() throws Exception { ... }
                
                ```

            - **`runBatchJob` 메서드**: 기준일을 계산하고 배치 작업 실행
                - **`batchBaseDateResolver.resolveScheduledBaseDate()`:** 오늘 날짜를 기준으로 주식 시장 기준일을 계산 (휴장일 고려)
                - **`.addLocalDate(..., baseDate, true)`**: **Job이 `baseDate`를 식별용 Job Parameter**로 넣어 특정 일자의 **배치가 성공했다면 중복 실행되지 않도록 멱등성 보장**
                - **`jobOperator.start(...)`:** 실제 Spring Batch Job을 실행
        - application.yml
            - `enabled`로 각 환경에서 스케줄러의 작동 유무를 설정 (local에서는 작동 막아둠)
            - `cron`으로 배치 작업의 주기 설정
- **휴장일**
    - KIS API (국내휴장일조회 API) 사용하여 휴장일 조회
    - **휴장일이 포함된 경우, 가장 최근 개장일(`openDay=true`)을 기준 일(`baseDate`)로 적재**
      (최근 개장일이 이미 적재되어있으면 배치 생략)
    - node 서버에도 전달하기 위해 **redis에 적재**
        - key 예시: `market:calendar:KRX:20260501`
        - JSON 예시:

            ```json
              {
                "date": "2026-05-12",
                "businessDay": true,
                "tradingDay": true,
                "openDay": true,
                "settlementDay": true,
                "source": "KIS",
                "fetchedAt": "2026-05-12T06:00:00+09:00"
              }
            ```

- **랭킹 최신화**
    - 1시간마다 스케줄러가 모든 계좌의 totalAsset, totalProfitRate를 계산해 저장

---

### **API 연동 구현**

**[API 클라이언트 - `KisApiClient`]** 

- **핵심 원칙**
    - ⚠️ **Throttling(천천히 흘려보내기) 적용:** 모든 **API 호출 메서드 첫 줄에 `kisRateLimiter.acquire()`를 배치**하여 KIS API의 초당 호출 제한을 넘지 않도록 제어
    - **동기식 블로킹 (`.block()`):** 배치 작업 및 직관적인 트랜잭션 제어를 위해 WebFlux의 비동기 응답을 **동기식으로 대기하여 반환**
    - 복잡한 응답 JSON을 DTO로 받는 역할까지만 수행 (숫자 파싱, 에러 처리 등은 다른 클래스에 위임)
    - **재시도(retry)**: API 오류(네트워크/429/5xx) 발생하면 700ms 간격으로 3회까지 재시도
- **데이터 조회 API 총 9개** (**토큰 발급 API 포함 시 10개**)
    - **`FHKST01010100` (주식현재가 시세)**: 현재가, PER, PBR, 시가총액, 52주/250일 최고가 대비율 조회
    - **`FHKST01010300` (주식현재가 체결):** 당일 체결강도 초기값 조회
    - **`FHKST66430300` (국내주식 재무비율)**: ROE, EPS, 부채비율, 영업이익 성장률 조회
    - **`FHKST66430200` (국내주식 손익계산서)**: 매출액, 영업이익 조회
    - **`HHKDB669102C0` (예탁원정보 배당일정)**: 최근 1년간의 현금 배당금 내역 조회 (시가배당률, 배당성향 계산용)
    - **`FHPST04760000` (국내주식 신용잔고 일별추이)**: 신용 융자 잔고 비율 조회
    - **`FHPTJ04160001` (종목별 투자자매매동향)**: 외국인/기관/개인 순매수 추이 조회
    - **`HHKST03900300` (종목조건검색 목록조회)**: HTS 서버에 저장된 조건 이름과 고유 번호 목록 조회
    - **`HHKST03900400` (종목조건검색조회)**: 특정 조건식을 통과한 종목 리스트 결과 조회
- **예시: `getCurrentPrice` (주식현재가 시세 호출 API)**
    - 특정 종목의 **현재가,** **PER, PBR, 시가총액, 누적거래대금, 52주/250일 최고가 대비율** 등의 지표를 한 번에 조회하는 메서드
    - **특징:** 반환된 수많은 필드 중 JUMONEY 프로젝트에 필요한 데이터만 DTO(`KisCurrentPriceMetrics`)로 추려내어 메모리 낭비를 방지

**[인증 및 Redis 캐싱 - `KisTokenManager`]** 

- **핵심 원칙**
    - 24시간마다 만료되는 OAuth 토큰을 관리
    - **Redis 캐싱**과 **로컬 메모리** Fallback을 통한 이중 구조를 담당 (⚠️ **Redis 실패 시 → 로컬 메모리**)
- **`getAccessToken` 메서드: 토큰 조회 및 발급의 진입점**
    - **캐시 우선 조회**: 외부 API(KIS)를 호출하기 전, **항상 Redis(또는 로컬 캐시)에 유효한 토큰이 있는지** 먼저 검사
    - **안전한 동시성 제어**: synchronized 블록 안에서 한번 더 확인하여 KIS 서버에 중복 요청이 가지 않도록 제어
- **`getCachedToken` / `saveToken` 메서드: Redis 캐싱 및 회로 차단**
    - ⚠️**Redis 이중화 (Fallback):** 토큰을 조회하거나 저장할 때 **Redis 서버에 문제 발생 시 예외를 던지지 않고 로컬 캐시(메모리)를 사용**하도록 안전망 구성
    - **커스텀 서킷 브레이커 작동:** **Redis에 장애가 발생하면** `openRedisCircuit()`을 호출하여 **5분 동안 Redis 연결 시도 자체를 차단**. 장애가 난 Redis를 계속 찔러서 시스템 전체가 느려지는 문제 방지
- **`getLocalCachedToken` / `cacheTokenLocally` 메서드: 로컬 메모리 관리**
    - ⚠️**로컬 메모리**: Redis 서버를 사용할 수 없는 상황에서도 `volatile` 키워드로 선언된 **메모리 변수에 토큰을 유지해 서버 로직이 정상 작동하도록 보장**
    - **만료 시간 관리**: 로컬 변수 자체적으로 `localTokenExpiresAt`을 두어 23시간(TTL)이 지나면 메모리에서도 토큰을 비우고 재발급받도록 안전장치 마련
- **`issueNewToken` 메서드: 실제 KIS API 발급 요청**
    - **OAuth2 연동:** KIS의 `/oauth2/tokenP` 엔드포인트로 `appkey`와 `appsecret`을 보내 24시간짜리 임시 접근 토큰(Bearer)을 발급

**[트래픽 제어 - `KisRateLimiter`]** 

- **핵심 원칙**
    - 백엔드 서버가 트래픽 초과로 외부 API (KIS)에서 차단당하는 것을 막는 로직
    - ⚠️ **글로벌 락(`synchronized`)과 호출 간격(250ms)를 적용해 초당 호출 횟수 제한(18 TPS)를 절대 넘지 않도록 조절**하여 `429 Too Many Requests` 에러를 원천 차단
- **클래스 설계 및 초기화**
    - **글로벌 싱글톤(**`@Component`**):** 이 클래스는 **스프링 컨테이너에 단 1개만** 생성
      → 애플리케이션 내의 수많은 스레드(유저 요청, 배치 작업 등)가 모두 **이 하나의 객체(Limiter)를 거쳐 시스템 전역의 트래픽을 통제**
    - `@Value("${kis.rate-limit.min-interval-millis:250}")`설정값 주입: **API 호출 간격**을 프로퍼티(`application.yml`)로 분리
      → **기본값은 250ms(초당 4건)**로 안전하게 동작하도록 설정
- **`acquire` 메서드: 핵심 트래픽 제어 로직 (Throttling)**
    - **글로벌 락 제어 (`synchronized`):** 메서드 시그니처에 **`synchronized`를 선언**하여 여러 스레드가 동시에 접근하더라도 무조건 하나씩만 제어
    - ⚠️ **안전한 스레드 대기 (`Thread.sleep`):** 남은 시간이 있으면 해당 스레드를 **잠깐 대기시켜 호출 간격을 일정하게 유지**
    - **인터럽트(스레드에게 강제 중단 신호) 방어:** **스레드가 잠들어(`sleep`) 있는 동안 외부에서 강제 종료**할 경우 `Thread.currentThread().interrupt()`를 호출하여 **정상적으로 스레드를 종료시키고 커스텀 예외(`KisApiException`)를 던지기**

---

### **Redis 활용**

- **Redis 캐시 (fallback)**
    - KIS API를 한번 호출 결과는 Redis에 캐시하고, 이후 조회는 **Redis 캐시를 우선 사용**
    - **실시간 신선도: 3분** (3분 이내면 실시간으로 인정)
      **캐시 TTL: 10분** (10분 동안 캐시 데이터 저장)
    - 사용 흐름
        1. **실시간 Redis stock:latest:{stockCode}**
        2. **캐시 Redis stock:current-price:{stockCode}** (실시간 데이터가 없거나 3분보다 오래됨)
        3. 둘 다 없다면 **KIS REST 호출**
- **local/dev 분리**
    - 노드 서버가 배포 서버의 redis로 적재하는 실시간 데이터를 활용하기 위해 **로컬 환경에서도 배포 서버 redis 데이터를 읽도록 구현**
    - **local 환경**: 앱 Redis는 로컬 Redis (배포 서버 Redis의 데이터 보호를 위해), **실시간 피드 Redis는 SSH로 연결한 배포 서버 Redis 읽기**
        - **터널링 전용 서버 접속 필요**(-N 옵션 추가 시 터널만 유지): `ssh -i jumoney-server-key-pair.pem -N -L 6380:localhost:6379 [ubuntu@13.209.125.199](mailto:ubuntu@13.209.125.199)`
    - **dev 환경**: 앱 Redis와 실시간 피드 Redis 둘 다 배포 서버 Redis를 바라볼 수 있지만 코드에서는 역할 분리

---

### 스모크 테스트 (Local Smoke API)

**로컬 검증과 운영 수동 관리를 위해 제공하는 관리자용 API**

- **핵심 원칙**
    - **~~환경 격리 (`@Profile("local")`):** 운영 환경에서는 API가 노출되지 않도록 local 환경에서만 빈 등록~~
    - 파라미터를 자동 세팅되도록 하여 스웨거 테스트 편의성을 높임
    - 관리자 키로 관리자만 사용가능하도록 제어
- **`KisSmokeController`:** HTTP 엔드포인트 노출
- **`KisSmokeService`:** 핵심 로직 및 Spring Batch 연동

---

### 프로젝트 구조 및 컨벤션

- **도메인 주도 설계 (DDD)**
    - 주요 기능을 도메인 단위로 분리
      (`user`, `mockinvestment`, `stock`, `hojumoney`,
      `masterchoice`, `recommendation`, `ranking`)
    - 각 도메인 내부에서 4계층 (`controller`, `service`, `repository`, `domain`)으로 책임 분리
    - 인증/인가, 예외 처리, 공통 응답 등은 공통 모듈에서 처리
- **공통 응답 및 예외 처리**
    - API 응답은 공통 응답 객체를 통해 일관된 형식으로 반환
    - 전역 예외 핸들러를 통해 예외 응답 포맷 통일
    - 도메인별 ErrorCode를 분리