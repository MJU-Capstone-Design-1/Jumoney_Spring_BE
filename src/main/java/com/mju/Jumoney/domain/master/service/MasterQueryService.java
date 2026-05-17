package com.mju.Jumoney.domain.master.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.domain.MasterPrinciple;
import com.mju.Jumoney.domain.master.dto.MasterDetailResponse;
import com.mju.Jumoney.domain.master.dto.MasterListResponse;
import com.mju.Jumoney.domain.master.dto.MasterResponse;
import com.mju.Jumoney.domain.master.repository.MasterOptionRepository;
import com.mju.Jumoney.domain.master.repository.MasterPrincipleRepository;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.master.repository.MasterTagRepository;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterQueryService {

    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;
    private final MasterTagRepository masterTagRepository;
    private final MasterPrincipleRepository masterPrincipleRepository;

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
        Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.MASTER_NOT_FOUND));

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
        Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.MASTER_NOT_FOUND));

        return MasterResponse.of(
                master,
                masterOptionRepository.findByMasterIdOrderByDisplayOrderAsc(masterId)
        );
    }

    private MasterDetailResponse.PrincipleResponse toPrincipleResponse(MasterPrinciple principle) {
        return new MasterDetailResponse.PrincipleResponse(
                principle.getTitle(),
                principle.getDescription(),
                principle.getDetails() == null ? List.of() : List.copyOf(principle.getDetails())
        );
    }
}
