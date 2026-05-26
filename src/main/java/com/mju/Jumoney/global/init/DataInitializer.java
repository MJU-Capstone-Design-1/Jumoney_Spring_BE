package com.mju.Jumoney.global.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.Jumoney.domain.hojumoney.domain.HojumoneyPersona;
import com.mju.Jumoney.domain.hojumoney.domain.SurveyOption;
import com.mju.Jumoney.domain.hojumoney.domain.SurveyOptionRestriction;
import com.mju.Jumoney.domain.hojumoney.domain.SurveyQuestion;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyPersonaInitDto;
import com.mju.Jumoney.domain.hojumoney.dto.SurveyOptionInitDto;
import com.mju.Jumoney.domain.hojumoney.dto.SurveyQuestionInitDto;
import com.mju.Jumoney.domain.hojumoney.repository.HojumoneyPersonaRepository;
import com.mju.Jumoney.domain.hojumoney.repository.SurveyOptionRepository;
import com.mju.Jumoney.domain.hojumoney.repository.SurveyOptionRestrictionRepository;
import com.mju.Jumoney.domain.hojumoney.repository.SurveyQuestionRepository;
import com.mju.Jumoney.domain.master.domain.*;
import com.mju.Jumoney.domain.master.dto.*;
import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.repository.*;
import com.mju.Jumoney.domain.sector.domain.Sector;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import com.mju.Jumoney.domain.sector.repository.SectorRepository;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.dto.StockInitDto;
import com.mju.Jumoney.domain.stock.enums.MarketType;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stockterm.domain.StockTerm;
import com.mju.Jumoney.domain.stockterm.dto.StockTermInitDto;
import com.mju.Jumoney.domain.stockterm.repository.StockTermRepository;
import com.mju.Jumoney.domain.verifiedoperation.service.VerifiedOperationAccountInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final SectorRepository sectorRepository;
    private final StockRepository stockRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final SurveyOptionRestrictionRepository surveyOptionRestrictionRepository;
    private final HojumoneyPersonaRepository hojumoneyPersonaRepository;
    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;
    private final TagRepository tagRepository;
    private final MasterTagRepository masterTagRepository;
    private final MasterPrincipleRepository masterPrincipleRepository;
    private final MasterCaseRepository masterCaseRepository;
    private final MasterPortfolioStockRepository masterPortfolioStockRepository;
    private final StockTermRepository stockTermRepository;
    private final VerifiedOperationAccountInitializer verifiedOperationAccountInitializer;
    private final ObjectMapper objectMapper;
    private Map<MasterCode, Master> masterCache = Map.of();

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("[DataInitializer] 애플리케이션 초기 데이터 세팅 시작");

        initStockData();
        initStockTermData();
        initHojumoneySurveyData();
        initHojumoneyPersonaData();
        initMasterData();
        initMasterTagData();
        initMasterPrincipleData();
        initMasterCaseData();
        initMasterPortfolioStockData();
        initMasterOptionData();
        verifiedOperationAccountInitializer.initializeAccounts();

        log.info("[DataInitializer] 애플리케이션 초기 데이터 세팅 완료");
    }

    private void initStockData() throws Exception {
        log.info(" 종목 데이터 초기화/갱신 진행 중");

        ClassPathResource resource = new ClassPathResource("data/stock_data.json");
        List<StockInitDto> stockDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<StockInitDto>>() {
                }
        );

        int createdCount = 0;
        int updatedCount = 0;

        for (StockInitDto dto : stockDtos) {
            SectorType sectorType = SectorType.fromDescription(dto.sectorName());
            Sector sector = sectorRepository.findBySectorName(sectorType)
                    .orElseGet(() -> sectorRepository.save(Sector.create(sectorType)));

            List<String> descriptionList = Arrays.stream(dto.description().split("\n"))
                    .filter(s -> !s.isBlank())
                    .map(String::trim)
                    .toList();

            MarketType marketType = MarketType.fromCode(dto.marketCode());

            Optional<Stock> existingStock = stockRepository.findByStockCode(dto.ticker());
            if (existingStock.isPresent()) {
                existingStock.get().updateBasicInfo(
                        sector,
                        dto.name(),
                        marketType,
                        descriptionList,
                        dto.isLeader()
                );
                updatedCount++;
            } else {
                Stock stock = Stock.create(
                        sector,
                        dto.ticker(),
                        dto.name(),
                        marketType,
                        descriptionList,
                        dto.isLeader()
                );

                stockRepository.save(stock);
                createdCount++;
            }
        }

        log.info(" 총 {}개 종목 데이터 초기화/갱신 완료(created={}, updated={})", stockDtos.size(), createdCount, updatedCount);
    }

    private void initStockTermData() throws Exception {
        log.info(" 주식 용어 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/stock_term_data.json");
        List<StockTermInitDto> termDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
                , new TypeReference<List<StockTermInitDto>>() {
                }
        );

        for (StockTermInitDto dto : termDtos) {
            stockTermRepository.findByCategoryAndTermName(dto.category(), dto.termName())
                    .ifPresentOrElse(
                            existing -> existing.updateContent(
                                    dto.subtitle(),
                                    dto.description(),
                                    dto.imageFileName()
                            ),
                            () -> stockTermRepository.save(StockTerm.create(
                                    dto.category(),
                                    dto.termName(),
                                    dto.subtitle(),
                                    dto.description(),
                                    dto.imageFileName()
                            ))
                    );
        }

        log.info(" 주식 용어 {}개 초기화 완료", termDtos.size());
    }

    private void initHojumoneySurveyData() throws Exception {
        log.info(" 오늘의 호주머니 설문 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/hojumoney_survey_data.json");
        List<SurveyQuestionInitDto> questionDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<SurveyQuestionInitDto>>() {
                }
        );

        for (SurveyQuestionInitDto questionDto : questionDtos) {
            SurveyQuestion question = surveyQuestionRepository.findByQuestionType(questionDto.questionType())
                    .map(existingQuestion -> {
                        existingQuestion.updateContent(
                                questionDto.content(),
                                questionDto.description(),
                                questionDto.displayOrder()
                        );
                        return existingQuestion;
                    })
                    .orElseGet(() -> surveyQuestionRepository.save(SurveyQuestion.create(
                            questionDto.questionType(),
                            questionDto.content(),
                            questionDto.description(),
                            questionDto.displayOrder()
                    )));

            for (SurveyOptionInitDto optionDto : questionDto.options()) {
                surveyOptionRepository.findByLogicCode(optionDto.logicCode())
                        .ifPresentOrElse(
                                existingOption -> existingOption.updateContent(
                                        optionDto.content(),
                                        optionDto.description(),
                                        optionDto.displayOrder()
                                ),
                                () -> surveyOptionRepository.save(SurveyOption.create(
                                        question,
                                        optionDto.content(),
                                        optionDto.logicCode(),
                                        optionDto.description(),
                                        optionDto.displayOrder()
                                ))
                        );
            }
        }

        List<Long> hojumoneyOptionIds = new ArrayList<>();
        for (SurveyQuestionInitDto questionDto : questionDtos) {
            for (SurveyOptionInitDto optionDto : questionDto.options()) {
                SurveyOption option = surveyOptionRepository.findByLogicCode(optionDto.logicCode())
                        .orElseThrow(() -> new IllegalStateException("설문 선택지 초기화 실패: logicCode=" + optionDto.logicCode()));
                hojumoneyOptionIds.add(option.getId());
            }
        }
        surveyOptionRestrictionRepository.deleteBySourceOptionIdIn(hojumoneyOptionIds);

        Set<String> savedRestrictionKeys = new HashSet<>();
        for (SurveyQuestionInitDto questionDto : questionDtos) {
            for (SurveyOptionInitDto optionDto : questionDto.options()) {
                if (optionDto.restrictedLogicCodes() == null || optionDto.restrictedLogicCodes().isEmpty()) {
                    continue;
                }

                SurveyOption option = surveyOptionRepository.findByLogicCode(optionDto.logicCode())
                        .orElseThrow(() -> new IllegalStateException("설문 선택지 초기화 실패: logicCode=" + optionDto.logicCode()));
                for (var restrictedLogicCode : optionDto.restrictedLogicCodes()) {
                    SurveyOption restrictedOption = surveyOptionRepository.findByLogicCode(restrictedLogicCode)
                            .orElseThrow(() -> new IllegalStateException("제한 선택지 초기화 실패: logicCode=" + restrictedLogicCode));

                    String restrictionKey = option.getId() + ":" + restrictedOption.getId();
                    if (!savedRestrictionKeys.add(restrictionKey)) {
                        continue;
                    }
                    surveyOptionRestrictionRepository.save(SurveyOptionRestriction.create(option, restrictedOption));
                }
            }
        }

        int optionCount = questionDtos.stream()
                .map(SurveyQuestionInitDto::options)
                .mapToInt(List::size)
                .sum();
        log.info(" 오늘의 호주머니 설문 문항 {}개, 선택지 {}개 초기화 완료", questionDtos.size(), optionCount);
    }

    private void initHojumoneyPersonaData() throws Exception {
        log.info(" 오늘의 호주머니 페르소나 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/hojumoney_persona_data.json");
        List<HojumoneyPersonaInitDto> personaDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<HojumoneyPersonaInitDto>>() {
                }
        );

        for (HojumoneyPersonaInitDto dto : personaDtos) {
            hojumoneyPersonaRepository.findByInvestmentPurposeAndRiskProfileAndInvestmentHorizon(
                            dto.investmentPurpose(),
                            dto.riskProfile(),
                            dto.investmentHorizon()
                    )
                    .ifPresentOrElse(
                            existingPersona -> existingPersona.updateContent(dto.personaName(), dto.personaDescription()),
                            () -> hojumoneyPersonaRepository.save(HojumoneyPersona.create(
                                    dto.investmentPurpose(),
                                    dto.riskProfile(),
                                    dto.investmentHorizon(),
                                    dto.personaName(),
                                    dto.personaDescription()
                            ))
                    );
        }

        log.info(" 오늘의 호주머니 페르소나 {}개 초기화 완료", personaDtos.size());
    }

    private void initMasterData() throws Exception {
        log.info(" 거장의 선택 거장 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/master_data.json");
        List<MasterInitDto> masterDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<MasterInitDto>>() {
                }
        );

        for (MasterInitDto dto : masterDtos) {
            masterRepository.findByMasterCode(dto.masterCode())
                    .ifPresentOrElse(
                            existingMaster -> existingMaster.updateContent(
                                    dto.masterCode(),
                                    dto.masterName(),
                                    dto.quote(),
                                    dto.imageFileName(),
                                    dto.portfolioBasePeriod(),
                                    dto.philosophyTitle(),
                                    dto.philosophyDescription(),
                                    dto.recommendationDescription(),
                                    dto.displayOrder()
                            ),
                            () -> masterRepository.save(Master.create(
                                    dto.masterCode(),
                                    dto.masterName(),
                                    dto.quote(),
                                    dto.imageFileName(),
                                    dto.portfolioBasePeriod(),
                                    dto.philosophyTitle(),
                                    dto.philosophyDescription(),
                                    dto.recommendationDescription(),
                                    dto.displayOrder()
                            ))
                    );
        }

        refreshMasterCache();
        log.info(" 거장의 선택 거장 {}명 초기화 완료", masterDtos.size());
    }

    private void initMasterOptionData() throws Exception {
        log.info(" 거장의 선택 옵션 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/master_option_data.json");
        List<MasterOptionInitDto> optionDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<MasterOptionInitDto>>() {
                }
        );

        for (MasterOptionInitDto dto : optionDtos) {
            Master master = resolveMaster(dto.masterCode(), "거장 선택지 초기화 실패");

            masterOptionRepository.findByLogicCode(dto.logicCode())
                    .ifPresentOrElse(
                            existingOption -> existingOption.updateContent(
                                    master,
                                    dto.content(),
                                    dto.description(),
                                    dto.displayOrder()
                            ),
                            () -> masterOptionRepository.save(MasterOption.create(
                                    master,
                                    dto.content(),
                                    dto.description(),
                                    dto.logicCode(),
                                    dto.displayOrder()
                            ))
                    );
        }

        log.info(" 거장의 선택 옵션 {}개 초기화 완료", optionDtos.size());
    }

    private void initMasterTagData() throws Exception {
        log.info(" 거장 태그 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/master_tag_data.json");
        List<MasterTagInitDto> tagDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<MasterTagInitDto>>() {
                }
        );

        List<Long> masterIds = tagDtos.stream()
                .map(dto -> resolveMaster(dto.masterCode(), "거장 태그 초기화 실패").getId())
                .distinct()
                .toList();
        if (!masterIds.isEmpty()) {
            masterTagRepository.deleteByMasterIdIn(masterIds);
        }

        for (MasterTagInitDto dto : tagDtos) {
            Master master = resolveMaster(dto.masterCode(), "거장 태그 초기화 실패");
            for (String tagName : dto.tags()) {
                Tag tag = tagRepository.findByTagName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
                masterTagRepository.save(MasterTag.create(master, tag));
            }
        }

        int tagCount = tagDtos.stream()
                .map(MasterTagInitDto::tags)
                .mapToInt(List::size)
                .sum();
        log.info(" 거장 태그 {}개 초기화 완료", tagCount);
    }

    private void initMasterPrincipleData() throws Exception {
        log.info(" 거장 원칙 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/master_principle_data.json");
        List<MasterPrincipleInitDto> principleDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<MasterPrincipleInitDto>>() {
                }
        );

        List<Long> masterIds = principleDtos.stream()
                .map(dto -> resolveMaster(dto.masterCode(), "거장 원칙 초기화 실패").getId())
                .distinct()
                .toList();
        if (!masterIds.isEmpty()) {
            masterPrincipleRepository.deleteByMasterIdIn(masterIds);
        }

        for (MasterPrincipleInitDto dto : principleDtos) {
            Master master = resolveMaster(dto.masterCode(), "거장 원칙 초기화 실패");
            masterPrincipleRepository.save(MasterPrinciple.create(
                    master,
                    dto.title(),
                    dto.description(),
                    dto.details()
            ));
        }

        log.info(" 거장 원칙 {}개 초기화 완료", principleDtos.size());
    }

    private void initMasterCaseData() throws Exception {
        log.info(" 거장 대표 사례 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/master_case_data.json");
        List<MasterCaseInitDto> caseDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<MasterCaseInitDto>>() {
                }
        );

        List<Long> masterIds = caseDtos.stream()
                .map(dto -> resolveMaster(dto.masterCode(), "거장 대표 사례 초기화 실패").getId())
                .distinct()
                .toList();
        if (!masterIds.isEmpty()) {
            masterCaseRepository.deleteByMasterIdIn(masterIds);
        }

        for (MasterCaseInitDto dto : caseDtos) {
            Master master = resolveMaster(dto.masterCode(), "거장 대표 사례 초기화 실패");
            masterCaseRepository.save(MasterCase.create(
                    master,
                    dto.stockName(),
                    dto.sector(),
                    dto.investmentPeriod(),
                    dto.investmentResult(),
                    dto.title(),
                    dto.description()
            ));
        }

        log.info(" 거장 대표 사례 {}개 초기화 완료", caseDtos.size());
    }

    private void initMasterPortfolioStockData() throws Exception {
        log.info(" 거장 포트폴리오 종목 데이터 초기화 진행 중");

        ClassPathResource resource = new ClassPathResource("data/master_portfolio_stock_data.json");
        List<MasterPortfolioStockInitDto> stockDtos = objectMapper.readValue(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8),
                new TypeReference<List<MasterPortfolioStockInitDto>>() {
                }
        );

        List<Long> masterIds = stockDtos.stream()
                .map(dto -> resolveMaster(dto.masterCode(), "거장 포트폴리오 종목 초기화 실패").getId())
                .distinct()
                .toList();
        if (!masterIds.isEmpty()) {
            masterPortfolioStockRepository.deleteByMasterIdIn(masterIds);
        }

        for (MasterPortfolioStockInitDto dto : stockDtos) {
            Master master = resolveMaster(dto.masterCode(), "거장 포트폴리오 종목 초기화 실패");
            masterPortfolioStockRepository.save(MasterPortfolioStock.create(
                    master,
                    dto.stockName(),
                    dto.sector(),
                    dto.weight()
            ));
        }

        log.info(" 거장 포트폴리오 종목 {}개 초기화 완료", stockDtos.size());
    }

    private Master resolveMaster(MasterCode masterCode, String messagePrefix) {
        Master master = masterCache.get(masterCode);
        if (master == null) {
            throw new IllegalStateException(messagePrefix + ": masterCode=" + masterCode);
        }
        return master;
    }

    private void refreshMasterCache() {
        masterCache = masterRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Master::getMasterCode, master -> master));
    }

}
