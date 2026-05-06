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

| Step | API | TR ID | Usage |
|---:|---|---|---|
| 1 | 주식현재가 시세 | `FHKST01010100` | 현재가, 시가총액, PER, PBR, 거래대금, 250일/52주 고가 대비율 |
| 2 | 주식현재가 체결 | `FHKST01010300` | 초단기 추천 정렬용 당일 체결강도 |
| 3 | 국내주식 재무비율 | `FHKST66430300` | 영업이익증가율, ROE, EPS, 부채비율 |
| 4 | 국내주식 손익계산서 | `FHKST66430200` | 매출액, 영업이익 |
| 5 | 예탁원정보 배당일정 | `HHKDB669102C0` | 주당배당금, 배당 기준일 |
| 6 | 국내주식 신용잔고 일별추이 | `FHPST04760000` | 전체 융자 잔고 비율 |
| 7 | 종목별 투자자매매동향 일별 | `FHPTJ04160001` | 기관계 순매수 수량 |

## Calculated Metrics

### 배당성향

- KIS `FHKST66430500`의 `payout_rate`는 비정상 출력되는 데이터로 확인되어 사용하지 않는다.
- 배당성향은 `기간 내 주당배당금(DPS) 합계 / EPS * 100`으로 계산한다.
- DPS 원천: `HHKDB669102C0`의 `per_sto_divi_amt`
- EPS 원천: `FHKST66430300`의 `eps`
- 결과 단위: 퍼센트 값. 예: DPS 1,488원, EPS 6,564원이면 `22.6691`
- DPS 또는 EPS가 없거나 EPS가 0 이하이면 계산 결과는 `null`로 둔다.

## Excluded APIs

| API | TR ID | Reason |
|---|---|---|
| 국내주식 기타주요비율 | `FHKST66430500` | `payout_rate` 신뢰 불가. 배당성향은 자체 계산한다. |

## Pending Integrations

| API | TR ID | Usage |
|---|---|---|
| 종목조건검색조회 | `HHKST03900400` | 베타, 볼린저밴드, RSI 조건검색 |
| 국내주식 실시간체결가 | `H0STCNT0` | 소수 관심종목 실시간 체결 데이터. 추천 체결강도 정렬은 REST `FHKST01010300` 우선 |
