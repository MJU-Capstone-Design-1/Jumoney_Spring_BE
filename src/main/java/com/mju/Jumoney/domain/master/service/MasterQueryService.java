package com.mju.Jumoney.domain.master.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.domain.MasterCase;
import com.mju.Jumoney.domain.master.domain.MasterPortfolioStock;
import com.mju.Jumoney.domain.master.domain.MasterPrinciple;
import com.mju.Jumoney.domain.master.dto.*;
import com.mju.Jumoney.domain.master.repository.*;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterQueryService {

    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;
    private final MasterTagRepository masterTagRepository;
    private final MasterPrincipleRepository masterPrincipleRepository;
    private final MasterPortfolioStockRepository masterPortfolioStockRepository;
    private final MasterCaseRepository masterCaseRepository;

    public List<MasterListResponse> getMasterList() {
        List<Master> masters = masterRepository.findAllByOrderByDisplayOrderAsc();
        if (masters.isEmpty()) {
            return List.of();
        }

        List<Long> masterIds = masters.stream()
                .map(Master::getId)
                .toList();
        Map<Long, List<String>> tagMap = masterTagRepository.findByMasterIdInWithTag(masterIds).stream()
                .collect(Collectors.groupingBy(
                        masterTag -> masterTag.getMaster().getId(),
                        Collectors.mapping(masterTag -> masterTag.getTag().getTagName(), Collectors.toList())
                ));

        return masters.stream()
                .map(master -> new MasterListResponse(
                        master.getId(),
                        master.getMasterCode(),
                        master.getMasterName(),
                        tagMap.getOrDefault(master.getId(), List.of())
                ))
                .toList();
    }

    public MasterDetailResponse getMasterDetail(Long masterId) {
        Master master = findMasterById(masterId);

        List<String> tags = masterTagRepository.findByMasterIdInWithTag(List.of(masterId)).stream()
                .map(masterTag -> masterTag.getTag().getTagName())
                .toList();
        List<MasterDetailResponse.PrincipleResponse> principles = masterPrincipleRepository.findByMasterIdOrderByIdAsc(masterId).stream()
                .map(this::toPrincipleResponse)
                .toList();

        return new MasterDetailResponse(
                master.getId(),
                master.getMasterCode(),
                master.getMasterName(),
                tags,
                master.getQuote(),
                new MasterDetailResponse.PhilosophyResponse(
                        master.getPhilosophyTitle(),
                        master.getPhilosophyDescription()
                ),
                principles
        );
    }

    public MasterResponse getMaster(Long masterId) {
        Master master = findMasterById(masterId);

        return MasterResponse.of(
                master,
                masterOptionRepository.findByMasterIdOrderByDisplayOrderAsc(masterId)
        );
    }

    public MasterPortfolioChartResponse getMasterPortfolioChart(Long masterId) {
        Master master = findMasterById(masterId);
        List<MasterPortfolioStock> portfolioStocks = findPortfolioStocksByMasterId(masterId);

        return new MasterPortfolioChartResponse(
                master.getId(),
                master.getMasterCode(),
                master.getMasterName(),
                master.getPortfolioBasePeriod(),
                toSectorChartResponses(portfolioStocks),
                toCompanyRatioChartResponses(portfolioStocks)
        );
    }

    public MasterPortfolioDescriptionResponse getMasterPortfolioDescription(Long masterId) {
        Master master = findMasterById(masterId);
        List<MasterPortfolioStock> portfolioStocks = findPortfolioStocksByMasterId(masterId);

        return new MasterPortfolioDescriptionResponse(
                master.getId(),
                master.getMasterCode(),
                master.getMasterName(),
                master.getPortfolioBasePeriod(),
                findRepresentativeCase(masterId)
                        .map(this::toRepresentativeCaseResponse)
                        .orElse(null),
                toStockResponses(portfolioStocks)
        );
    }

    // ========== 조회 메서드 ==========
    private Master findMasterById(Long masterId) {
        return masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.MASTER_NOT_FOUND));
    }

    private List<MasterPortfolioStock> findPortfolioStocksByMasterId(Long masterId) {
        return masterPortfolioStockRepository.findByMasterIdOrderByWeightDesc(masterId);
    }

    private Optional<MasterCase> findRepresentativeCase(Long masterId) {
        return masterCaseRepository.findFirstByMasterIdOrderByIdAsc(masterId);
    }

    // ========== 비즈니스 로직 메서드 ==========
    private MasterDetailResponse.PrincipleResponse toPrincipleResponse(MasterPrinciple principle) {
        return new MasterDetailResponse.PrincipleResponse(
                principle.getTitle(),
                principle.getDescription(),
                principle.getDetails() == null ? List.of() : List.copyOf(principle.getDetails())
        );
    }

    private List<MasterPortfolioChartResponse.SectorChartResponse> toSectorChartResponses(List<MasterPortfolioStock> portfolioStocks) {
        return portfolioStocks.stream()
                .collect(Collectors.groupingBy(
                        MasterPortfolioStock::getSector,
                        Collectors.reducing(BigDecimal.ZERO, MasterPortfolioStock::getWeight, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> new MasterPortfolioChartResponse.SectorChartResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<MasterPortfolioChartResponse.CompanyRatioChartResponse> toCompanyRatioChartResponses(List<MasterPortfolioStock> portfolioStocks) {
        return portfolioStocks.stream()
                .map(stock -> new MasterPortfolioChartResponse.CompanyRatioChartResponse(
                        stock.getStockName(),
                        stock.getWeight()
                ))
                .toList();
    }

    private MasterPortfolioDescriptionResponse.RepresentativeCaseResponse toRepresentativeCaseResponse(MasterCase masterCase) {
        return new MasterPortfolioDescriptionResponse.RepresentativeCaseResponse(
                masterCase.getStockName(),
                masterCase.getSector(),
                masterCase.getInvestmentPeriod(),
                masterCase.getInvestmentResult(),
                masterCase.getTitle(),
                masterCase.getDescription()
        );
    }

    private List<MasterPortfolioDescriptionResponse.StockResponse> toStockResponses(List<MasterPortfolioStock> portfolioStocks) {
        return portfolioStocks.stream()
                .map(stock -> new MasterPortfolioDescriptionResponse.StockResponse(
                        stock.getStockName(),
                        stock.getSector(),
                        stock.getWeight()
                ))
                .toList();
    }
}
