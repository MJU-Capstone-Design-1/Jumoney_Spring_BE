package com.mju.Jumoney.domain.sector.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.global.realtime.RealtimeRedisReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodSectorService {

    private static final String NEWS_ANALYSIS_TODAY_KEY = "news:analysis:today";
    private static final String GOOD_SECTORS_FIELD = "goodSectors";

    private final RealtimeRedisReader realtimeRedisReader;

    public Set<String> getTodayGoodSectorNames() {
        try {
            Set<String> goodSectorNames = realtimeRedisReader
                    .hashGet(
                            NEWS_ANALYSIS_TODAY_KEY,
                            GOOD_SECTORS_FIELD,
                            new TypeReference<List<NewsSector>>() {
                            }
                    )
                    .stream()
                    .flatMap(Collection::stream)
                    .map(NewsSector::sectorName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(sectorName -> !sectorName.isBlank())
                    .collect(Collectors.toSet());
            log.info("[GoodSector] Redis 호재 섹터 조회 완료: key={}, field={}, goodSectors={}",
                    NEWS_ANALYSIS_TODAY_KEY, GOOD_SECTORS_FIELD, goodSectorNames);
            return goodSectorNames;
        } catch (RuntimeException e) {
            log.warn("[GoodSector] 호재 섹터 조회 실패. 뉴스 섹터 태그 없이 추천을 진행합니다.", e);
            return Set.of();
        }
    }

    public List<String> goodSectorTags(Stock stock, Set<String> goodSectorNames) {
        return hasGoodSectorMatch(stock, goodSectorNames)
                ? List.of(stock.getSector().getSectorName().name())
                : List.of();
    }

    public boolean hasGoodSectorMatch(Stock stock, Set<String> goodSectorNames) {
        if (goodSectorNames.isEmpty()) {
            return false;
        }
        String sectorName = stock.getSector().getSectorName().getDescription();
        return goodSectorNames.contains(sectorName);
    }

    private record NewsSector(
            String sectorName,
            String reason
    ) {
    }
}
