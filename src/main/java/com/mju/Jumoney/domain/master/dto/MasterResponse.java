package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.domain.MasterOption;
import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.sector.enums.SectorType;

import java.util.Arrays;
import java.util.List;

public record MasterResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName,
        String recommendationDescription,
        List<MasterOptionResponse> options,
        List<SectorOptionResponse> sectorOptions
) {

    public static MasterResponse of(Master master, List<MasterOption> options) {
        return new MasterResponse(
                master.getId(),
                master.getMasterCode(),
                master.getMasterName(),
                master.getRecommendationDescription(),
                options.stream()
                        .map(MasterOptionResponse::from)
                        .toList(),
                hasSectorSelectableOption(options) ? buildSectorOptions() : List.of()
        );
    }

    private static boolean hasSectorSelectableOption(List<MasterOption> options) {
        return options.stream()
                .map(MasterOption::getLogicCode)
                .anyMatch(MasterResponse::requiresSectorSelection);
    }

    private static List<SectorOptionResponse> buildSectorOptions() {
        return Arrays.stream(SectorType.values())
                .map(SectorOptionResponse::from)
                .toList();
    }

    private static boolean requiresSectorSelection(MasterOptionLogicCode logicCode) {
        return logicCode == MasterOptionLogicCode.LYNCH_SECTOR
                || logicCode == MasterOptionLogicCode.DALIO_ALL_WEATHER;
    }

    public record MasterOptionResponse(
            Long optionId,
            MasterOptionLogicCode logicCode,
            String content,
            String description,
            int displayOrder,
            boolean requiresSectorSelection
    ) {

        public static MasterOptionResponse from(MasterOption option) {
            return new MasterOptionResponse(
                    option.getId(),
                    option.getLogicCode(),
                    option.getContent(),
                    option.getDescription(),
                    option.getDisplayOrder(),
                    MasterResponse.requiresSectorSelection(option.getLogicCode())
            );
        }
    }

    public record SectorOptionResponse(
            SectorType sectorType,
            String description
    ) {

        public static SectorOptionResponse from(SectorType sectorType) {
            return new SectorOptionResponse(
                    sectorType,
                    sectorType.getDescription()
            );
        }
    }
}
