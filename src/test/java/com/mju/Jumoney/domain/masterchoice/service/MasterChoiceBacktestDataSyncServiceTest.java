package com.mju.Jumoney.domain.masterchoice.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MasterChoiceBacktestDataSyncServiceTest {

    @Test
    void calculateAvailableDateUsesInterimDisclosureDelayForQuarterAndHalfYearSettlements() {
        assertThat(MasterChoiceBacktestDataSyncService.calculateAvailableDate("202603"))
                .isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(MasterChoiceBacktestDataSyncService.calculateAvailableDate("202606"))
                .isEqualTo(LocalDate.of(2026, 8, 14));
        assertThat(MasterChoiceBacktestDataSyncService.calculateAvailableDate("202609"))
                .isEqualTo(LocalDate.of(2026, 11, 14));
    }

    @Test
    void calculateAvailableDateUsesAnnualDisclosureDelayForYearEndSettlements() {
        assertThat(MasterChoiceBacktestDataSyncService.calculateAvailableDate("202512"))
                .isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void previousYearSettlementYearMonthUsesSameSettlementMonth() {
        assertThat(MasterChoiceBacktestDataSyncService.previousYearSettlementYearMonth("202603"))
                .isEqualTo("202503");
        assertThat(MasterChoiceBacktestDataSyncService.previousYearSettlementYearMonth("202512"))
                .isEqualTo("202412");
    }
}
