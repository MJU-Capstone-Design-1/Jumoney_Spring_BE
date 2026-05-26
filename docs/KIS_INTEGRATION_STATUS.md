# KIS Integration Status

본 문서는 Jumoney에서 실제 연동 대상으로 사용하는 KIS API와 계산 지표 정책을 정리한다.

## Package Layout

- `global.client.kis.core`: KIS 호출, 토큰, 응답 매핑, 계산 지표 공통 로직
- `global.client.kis.dto.common`: KIS 공통 응답 및 토큰 DTO
- `global.client.kis.dto.price`: 현재가/시가총액/PER/PBR/거래대금 DTO
- `global.client.kis.dto.finance`: 재무비율, 손익계산서 DTO
- `global.client.kis.dto.dividend`: 배당일정 DTO
- `global.client.kis.dto.trading`: 신용잔고, 투자자매매동향 DTO
- `global.client.kis.enums`: KIS 요청 옵션 enum
- `global.client.kis.smoke`: local 프로필 전용 KIS 연동 검증 API

## Active REST APIs

| Step | API            | TR ID           | Usage                                      |
|-----:|----------------|-----------------|--------------------------------------------|
|    1 | 주식현재가 시세       | `FHKST01010100` | 현재가, 시가총액, PER, PBR, 거래대금, 250일/52주 고가 대비율 |
|    2 | 주식현재가 체결       | `FHKST01010300` | 초단기 추천 정렬용 당일 체결강도                         |
|    3 | 국내주식 재무비율      | `FHKST66430300` | 영업이익증가율, ROE, EPS, 부채비율                    |
|    4 | 국내주식 손익계산서     | `FHKST66430200` | 매출액, 영업이익                                  |
|    5 | 예탁원정보 배당일정     | `HHKDB669102C0` | 주당배당금, 배당 기준일                              |
|    6 | 국내주식 신용잔고 일별추이 | `FHPST04760000` | 전체 융자 잔고 비율                                |
|    7 | 종목별 투자자매매동향 일별 | `FHPTJ04160001` | 기관계 순매수 수량                                 |
|    8 | 종목조건검색 목록조회    | `HHKST03900300` | HTS 서버저장 조건 seq 확인                         |
|    9 | 종목조건검색조회       | `HHKST03900400` | 위험 성향별 베타, 볼린저밴드, RSI 조건검색 결과              |

## Rate Limit Policy

- KIS REST production 한도는 계정/app key당 초당 18회로 관리한다.
- KIS 권장사항에 따라 동시/연속 호출은 최소 100~150ms 이상 간격을 둔다.
- Jumoney 운영 기본값은 다른 현재가 조회, 추천, 배치, 조건검색 호출 여유를 남기기 위해 REST 호출 간 최소 `250ms` 간격으로 둔다. 즉 앱 전체 KIS REST 호출은 약 4 rps 이하로
  제한한다.
- local 프로필도 실전 계좌 API를 사용하므로 운영 기본값과 동일한 `250ms` 간격을 따른다.
- 이 제한은 `KisApiClient`의 모든 REST API 호출에 공통 적용한다. 개별 서비스나 배치가 별도 병렬 호출을 추가하더라도 같은 계정/app key 제한을 공유해야 한다.
- 여러 실전 계정/app key를 연결하면 REST 처리량을 늘릴 수 있지만, 토큰 캐시/RateLimiter/호출 라우팅을 credential별로 분리해야 하므로 현재는 구현하지 않는다. 필요 시
  `KisCredentialProvider`와 credential별 `KisTokenManager`, `KisRateLimiter`로 확장한다.
- 초단기 추천 체결강도는 장중 Redis `stock:latest:{code}.strength`가 freshness 조건을 만족하면 이 값을 우선 사용한다.
- Redis 값이 없거나 오래됐으면 `StockIndicator.executionStrength`로 fallback한다.
- 장마감 후 `15:45` 스케줄은 KIS를 다시 호출하지 않고, 같은 거래일의 Redis `stock:latest:{code}.strength`로 최신 기존 `StockIndicator.baseTime` 행의
  `executionStrength`를 보정한다.
- 추천 API는 사용자 요청 중 Spring REST `FHKST01010300` fallback을 호출하지 않는다.

## Realtime WebSocket Data

- Node 서버는 여러 실전 계정/app key를 활용해 KOSPI 200 종목의 `H0STCNT0` 국내주식 실시간체결가(KRX)를 구독하고 Redis에 적재할 수 있다.
- Spring은 장중 실시간성 추천/차트 데이터에 대해 KIS REST를 반복 호출하지 않고 Redis를 우선 조회한다.
- 장외 시간에는 Redis가 새 틱을 받지 않으므로, 사용자 응답은 장 마감 확정 데이터나 배치 적재 데이터가 우선 기준이 된다.
- Spring은 Redis 연결을 앱 내부 캐시용 Redis와 Node 실시간 피드 조회용 Redis로 논리 분리한다.
    - local 프로필: 앱 Redis는 로컬 Redis, 실시간 피드 Redis는 SSH 터널로 연결한 배포 Redis를 사용한다.
    - prod 프로필: 두 연결이 같은 운영 Redis를 바라볼 수 있지만, 코드에서는 역할을 분리한다.
    - 실시간 피드 Redis는 `RealtimeRedisReader`를 통해 읽기 전용으로 접근한다.
- `H0STCNT0`에서 Spring/추천/차트가 활용할 주요 필드는 다음과 같다.

| Field                                 | Usage                       |
|---------------------------------------|-----------------------------|
| `STCK_CNTG_HOUR`                      | 체결 시간. 틱 정렬과 분봉 bucket 기준   |
| `STCK_PRPR`                           | 현재가/체결가격. 분봉 close와 실시간 현재가 |
| `STCK_OPRC`                           | 당일 시가                       |
| `STCK_HGPR`                           | 당일 고가                       |
| `STCK_LWPR`                           | 당일 저가                       |
| `CNTG_VOL`                            | 체결 거래량. 분봉 volume 합산 원천     |
| `ACML_VOL`                            | 누적 거래량. 누락 보정 및 일봉 volume   |
| `ACML_TR_PBMN`                        | 누적 거래대금. 단기 추천 정렬 원천        |
| `CTTR`                                | 체결강도. 초단기 추천 정렬 원천          |
| `SELN_CNTG_SMTN` / `SHNU_CNTG_SMTN`   | 총 매도/매수 수량. 매수세/매도세 판단 보조   |
| `SHNU_RATE`                           | 매수비율                        |
| `PRDY_CTRT`                           | 전일 대비율                      |
| `VOL_TNRT`                            | 거래량 회전율                     |
| `ASKP_RSQN1` / `BIDP_RSQN1`           | 1호가 매도/매수 잔량                |
| `TOTAL_ASKP_RSQN` / `TOTAL_BIDP_RSQN` | 총 매도/매수 잔량                  |
| `TRHT_YN`                             | 거래정지 여부                     |
| `HOUR_CLS_CODE`                       | 장중/예상가/시간외 구분               |

- 봉 차트는 체결가 하나만으로는 만들 수 없고, 시간순 틱 데이터의 가격과 거래량을 집계해야 한다.
- 1분봉은 `STCK_CNTG_HOUR`를 분 단위로 묶고, 첫 가격을 open, 최고 가격을 high, 최저 가격을 low, 마지막 가격을 close, `CNTG_VOL` 합계를 volume으로 만든다.
- 당일 일봉은 `STCK_OPRC`, `STCK_HGPR`, `STCK_LWPR`, `STCK_PRPR`, `ACML_VOL`로 실시간 표시할 수 있다.
- 틱 누락이나 재연결이 발생할 수 있으므로 장중 Redis 분봉은 실시간 표시용으로 사용하고, 확정 분봉/일봉은 REST 분봉조회 또는 장마감 동기화로 보정한다.

## HTS Condition Search

- HTS 조건검색 API는 eFriend Plus [0110] 조건검색에서 조건을 만든 뒤 "사용자조건 서버저장"을 완료해야 호출할 수 있다.
- `HHKST03900300` 목록조회는 서버저장된 조건 목록과 `seq`를 확인하는 용도로 사용한다.
- `HHKST03900400` 결과조회는 `user_id`와 `seq`를 입력해 조건별 종목 목록을 가져온다.
- `MCA05762` "조회가 계속 됩니다. (다음을 누르십시오.)" 응답은 API 페이지네이션 문제가 아니라, HTS 조건이 API에서 조회 가능한 서버저장 상태가 아닐 때 발생한다. eFriend
  Plus [0110] 조건검색 화면에서 조건 등록 후 왼쪽 하단의 "사용자조건 서버저장"을 실행해야 한다.
- Jumoney는 설문 2의 위험 성향 4가지를 HTS 조건 4개와 1:1로 매핑한다.

| Search Type      | HTS Condition Name | Seq | Usage                       |
|------------------|--------------------|----:|-----------------------------|
| `STABILITY`      | 안전형                | `3` | 매우 낮음: 베타 0.7 이하, 볼린저밴드 15% |
| `SAFE_PURSUIT`   | 안전추구형              | `2` | 낮음: 베타 1.0 이하, 볼린저밴드 25%    |
| `PROFIT_PURSUIT` | 수익추구형              | `1` | 높음: 베타 1.0~1.2, RSI 50~65   |
| `AGGRESSIVE`     | 공격투자형              | `0` | 매우 높음: 베타 1.1 이상, RSI 60 이상 |

- HTS ID는 `kis.hts.user-id`로 설정한다.
- HTS ID 환경 변수는 `KIS_HTS_USER_ID`를 사용한다.
- 조건 seq는 민감정보가 아니고 추천 정책과 묶여 있으므로 `application.yml`에 고정값으로 관리한다.
- 조건검색 결과는 조건당 최대 100건이며, `HtsStock(searchType, baseDate)`에 저장한다. KIS 응답에 KOSPI 200 외 종목이 포함될 수 있으므로 Stock 테이블에 사전 적재된
  KOSPI 200 종목만 저장한다.
- KIS가 조건 결과 0건을 `MCA05918`로 응답하는 경우 빈 결과로 처리한다.

## Calculated Metrics

### 배당성향

- KIS `FHKST66430500`의 `payout_rate`는 비정상 출력되는 데이터로 확인되어 사용하지 않는다.
- 배당성향은 `기간 내 주당배당금(DPS) 합계 / EPS * 100`으로 계산한다.
- DPS 원천: `HHKDB669102C0`의 `per_sto_divi_amt`
- EPS 원천: `FHKST66430300`의 `eps`
- 결과 단위: 퍼센트 값. 예: DPS 1,488원, EPS 6,564원이면 `22.6691`
- DPS 또는 EPS가 없거나 EPS가 0 이하이면 계산 결과는 `null`로 둔다.

## StockIndicator Batch

- `StockIndicatorBatchService`는 Stock 테이블의 전체 종목을 순회하며 `StockIndicator`를 `stock + baseTime(yyyyMM)` 기준으로 upsert한다.
- 정기 스케줄 기준일은 KIS 국내휴장일조회 API(`CTCA0903R`)의 `opnd_yn=Y` 기준 직전 개장일이다. 화요일~토요일 오전 배치가 전일 장 마감 데이터를 적재하는 것을 기본 정책으로 한다.
- 국내휴장일조회 API 결과는 Node 서버와 공유할 수 있도록 Redis 날짜별 key(`market:calendar:KRX:yyyyMMdd`)에 JSON으로 적재한다. 스케줄러는 Redis에서 직전 개장일을
  먼저 찾고, 캐시가 없으면 KIS API를 호출해 Redis를 갱신한 뒤 계산한다. 서버 메모리 캐시는 사용하지 않는다.
- 기본 스케줄은 `stockIndicatorBatchJob` 화요일~토요일 06:00, `htsConditionBatchJob` 화요일~토요일 06:30(`Asia/Seoul`)이다.
- 스케줄러는 공통/local 설정에서 기본 비활성화되어 있으며, `KIS_STOCK_INDICATOR_SCHEDULER_ENABLED`, `KIS_HTS_CONDITION_SCHEDULER_ENABLED`로 켤 수
  있다. prod 프로필은 운영 배치 실행을 위해 기본 활성화한다.
- 수동 실행은 요청한 `baseDate`를 그대로 사용한다. 단, `stockIndicatorBatchJob`은 오늘 기준 실행 시 KIS 투자자매매동향 일별 API(`FHPTJ04160001`) 제한 때문에
  15:40 전 실행을 차단한다.
- `stockIndicatorBatchJob`은 종목 단위 저장은 계속 진행하지만, 하나 이상의 종목이 실패하면 Job 상태를 실패로 끝낸다. 같은 `baseDate`로 재실행할 수 있게 하기 위한 정책이다.
- 재무비율과 손익계산서는 연간 데이터(`KisFinancialPeriod.YEAR`)를 사용한다.
- 최신 결산년월 데이터를 현재 값으로, 그 이전 결산년월 데이터를 전년 값으로 사용한다.
- 신규 상장주처럼 전년도 재무 데이터가 없는 종목도 적재 대상에 포함한다.
- `lastYearEps`, `lastYearSales`는 전년도 데이터가 없으면 `null`로 저장한다.
- EPS 성장률, PEG, 매출액 증가율처럼 전년 데이터가 필요한 추천 조건에서만 해당 종목을 제외한다.
- 배당금은 기준일로부터 최근 1년 배당일정의 DPS 합계로 저장한다.
- 배당수익률은 `DPS 합계 / 현재가 * 100`으로 계산한다.
- 기관 순매수 수량은 KIS 투자자매매동향 응답의 최근 최대 20개 행을 합산한다.
- KIS 응답에 필수 지표가 없으면 해당 종목만 건너뛰고 다음 종목을 계속 처리한다.

## Excluded APIs

| API         | TR ID           | Reason                              |
|-------------|-----------------|-------------------------------------|
| 국내주식 기타주요비율 | `FHKST66430500` | `payout_rate` 신뢰 불가. 배당성향은 자체 계산한다. |

## Realtime Integration Contract

| Source                                   | Usage                                           |
|------------------------------------------|-------------------------------------------------|
| Node Redis `stock:latest:{code}`         | 현재 진행 중인 최신 1분 분봉 1개 조회. 현재가/등락률 표시, 초단기 추천 체결강도, 장마감 DB 보정, 초기 스냅샷에 사용 |
| Node Redis `stock:minute-candles:{code}` | 최근 40분 미확정 1분봉 조회. 분봉 차트 병합에 사용                 |
| Spring REST `FHKST01010300`              | 06:00 전체 지표 배치의 초기 체결강도 저장에 사용. 사용자 요청/장마감 보정 중 직접 fallback 호출 없음 |
