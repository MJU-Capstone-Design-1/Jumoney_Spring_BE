package com.mju.Jumoney.domain.master.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MasterOptionLogicCode {
    BUFFETT_ROE(MasterCode.WARREN_BUFFETT, "ROE 15% 이상"),
    BUFFETT_PER(MasterCode.WARREN_BUFFETT, "PER 0배 초과 15배 이하"),
    BUFFETT_EPS_GROWTH(MasterCode.WARREN_BUFFETT, "EPS 성장률 10% 이상"),
    BUFFETT_DEBT_RATIO(MasterCode.WARREN_BUFFETT, "부채비율 100% 이하"),
    BUFFETT_OPERATING_MARGIN(MasterCode.WARREN_BUFFETT, "영업이익률 20% 이상"),

    LYNCH_PEG(MasterCode.PETER_LYNCH, "PEG 1.0 이하"),
    LYNCH_EPS_GROWTH(MasterCode.PETER_LYNCH, "EPS 성장률 20% 이상 50% 이하"),
    LYNCH_DEBT_RATIO(MasterCode.PETER_LYNCH, "부채비율 100% 이하"),
    LYNCH_SALES_GROWTH(MasterCode.PETER_LYNCH, "매출액 증가율 10% 이상"),
    LYNCH_SECTOR(MasterCode.PETER_LYNCH, "섹터 선택"),

    DALIO_ALL_WEATHER(MasterCode.RAY_DALIO, "올웨더 포트폴리오"),
    DALIO_PER(MasterCode.RAY_DALIO, "PER 20배 이하"),
    DALIO_MARGIN_DEBT(MasterCode.RAY_DALIO, "신용잔고율 5% 이하"),
    DALIO_DEBT_RATIO(MasterCode.RAY_DALIO, "부채비율 50% 이하"),
    DALIO_EARNINGS_YIELD(MasterCode.RAY_DALIO, "이익수익률 3.38% 이상"),

    ONEIL_EPS_GROWTH(MasterCode.WILLIAM_ONEIL, "EPS 성장률 25% 이상"),
    ONEIL_ROE(MasterCode.WILLIAM_ONEIL, "ROE 17% 이상"),
    ONEIL_HIGH_52_WEEK(MasterCode.WILLIAM_ONEIL, "52주 신고가 갱신 또는 10% 근접"),
    ONEIL_MARKET_LEADER(MasterCode.WILLIAM_ONEIL, "대장주 여부"),
    ONEIL_INST_NET_BUY(MasterCode.WILLIAM_ONEIL, "최근 20거래일 기관 순매수 합계 0 이상");

    private final MasterCode masterCode;
    private final String label;
}
