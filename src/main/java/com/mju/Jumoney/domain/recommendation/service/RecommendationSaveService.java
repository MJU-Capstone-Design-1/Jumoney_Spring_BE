package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.dto.MasterRecommendationResponse;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.recommendation.domain.*;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationRequest;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationResponse;
import com.mju.Jumoney.domain.recommendation.enums.RecommendationStockTagType;
import com.mju.Jumoney.domain.recommendation.enums.RecommendationType;
import com.mju.Jumoney.domain.recommendation.repository.*;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationSaveService {

    private final UserRepository userRepository;
    private final MasterRepository masterRepository;
    private final StockRepository stockRepository;
    private final RecommendationRepository recommendationRepository;
    private final HojumoneyRecommendationRepository hojumoneyRecommendationRepository;
    private final MasterRecommendationRepository masterRecommendationRepository;
    private final RecommendationStockRepository recommendationStockRepository;
    private final RecommendationStockTagRepository recommendationStockTagRepository;

    @Transactional
    public Long saveHojumoneyRecommendation(
            Long userId,
            HojumoneyRecommendationRequest request,
            HojumoneyRecommendationResponse response
    ) {
        User user = userRepository.getReferenceById(userId);
        Recommendation recommendation = recommendationRepository.save(
                Recommendation.create(user, RecommendationType.HOJUMONEY)
        );

        hojumoneyRecommendationRepository.save(HojumoneyRecommendation.create(
                recommendation,
                response.investmentPurpose(),
                response.riskProfile(),
                response.investmentHorizon(),
                request.selectedOptionIds(),
                response.persona().personaName(),
                response.persona().personaDescription()
        ));

        List<RecommendationStockTag> tags = new ArrayList<>();
        for (HojumoneyRecommendationResponse.RecommendedStockResponse item : response.recommendations()) {
            Stock stock = stockRepository.getReferenceById(item.stockId());
            RecommendationStock recommendationStock = recommendationStockRepository.save(RecommendationStock.create(
                    recommendation,
                    stock,
                    item.rank(),
                    item.matchedConditionCount(),
                    item.sortMetricKey(),
                    item.sortMetricValue(),
                    item.currentPrice(),
                    item.changeRate()
            ));

            item.tags().forEach(tag -> tags.add(RecommendationStockTag.create(
                    recommendationStock,
                    RecommendationStockTagType.SURVEY_LOGIC,
                    tag.getLabel()
            )));
            item.goodSectorTags().forEach(tag -> tags.add(RecommendationStockTag.create(
                    recommendationStock,
                    RecommendationStockTagType.GOOD_SECTOR,
                    tag
            )));
        }

        recommendationStockTagRepository.saveAll(tags);
        return recommendation.getId();
    }

    @Transactional
    public Long saveMasterRecommendation(
            Long userId,
            Long masterId,
            MasterRecommendationResponse response
    ) {
        User user = userRepository.getReferenceById(userId);
        Master master = masterRepository.getReferenceById(masterId);
        Recommendation recommendation = recommendationRepository.save(
                Recommendation.create(user, RecommendationType.MASTER)
        );

        masterRecommendationRepository.save(MasterRecommendation.create(
                recommendation,
                master,
                response.selectedOptionIds()
        ));

        List<RecommendationStockTag> tags = new ArrayList<>();
        for (MasterRecommendationResponse.RecommendedStockResponse item : response.recommendations()) {
            Stock stock = stockRepository.getReferenceById(item.stockId());
            RecommendationStock recommendationStock = recommendationStockRepository.save(RecommendationStock.create(
                    recommendation,
                    stock,
                    item.rank(),
                    item.matchedConditionCount(),
                    item.sortMetricKey(),
                    item.sortMetricValue(),
                    item.currentPrice(),
                    item.changeRate()
            ));

            item.tags().forEach(tag -> tags.add(RecommendationStockTag.create(
                    recommendationStock,
                    RecommendationStockTagType.MASTER_OPTION,
                    tag.name()
            )));
            item.goodSectorTags().forEach(tag -> tags.add(RecommendationStockTag.create(
                    recommendationStock,
                    RecommendationStockTagType.GOOD_SECTOR,
                    tag
            )));
        }

        recommendationStockTagRepository.saveAll(tags);
        return recommendation.getId();
    }
}
