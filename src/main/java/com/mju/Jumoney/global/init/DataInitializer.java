package com.mju.Jumoney.global.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.Jumoney.domain.sector.domain.Sector;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import com.mju.Jumoney.domain.sector.repository.SectorRepository;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.dto.StockInitDto;
import com.mju.Jumoney.domain.stock.enums.MarketType;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final SectorRepository sectorRepository;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("[DataInitializer] 애플리케이션 초기 데이터 세팅 시작");
        
        initStockData();
        
        log.info("[DataInitializer] 애플리케이션 초기 데이터 세팅 완료");
    }

    private void initStockData() throws Exception {
        if (stockRepository.count() > 0) {
            log.info(" 종목 데이터가 이미 존재하여 초기화를 건너뜁니다.");
            return;
        }

        log.info(" 종목 데이터(200개) 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/stock_data.json");
        List<StockInitDto> stockDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<StockInitDto>>() {}
        );

        for (StockInitDto dto : stockDtos) {
            SectorType sectorType = SectorType.fromDescription(dto.sectorName());
            Sector sector = sectorRepository.findBySectorName(sectorType)
                    .orElseGet(() -> sectorRepository.save(Sector.create(sectorType)));

            List<String> descriptionList = Arrays.stream(dto.description().split("\n"))
                    .filter(s -> !s.isBlank())
                    .map(String::trim)
                    .toList();

            MarketType marketType = MarketType.fromCode(dto.marketCode());

            Stock stock = Stock.create(
                    sector,
                    dto.ticker(),
                    dto.name(),
                    marketType,
                    descriptionList,
                    dto.isLeader()
            );

            stockRepository.save(stock);
        }

        log.info(" 총 {}개 종목 및 섹터 데이터 초기화 완료", stockDtos.size());
    }
}
