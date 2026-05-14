package com.mju.Jumoney.domain.master.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.dto.MasterResponse;
import com.mju.Jumoney.domain.master.repository.MasterOptionRepository;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterQueryService {

    private final MasterRepository masterRepository;
    private final MasterOptionRepository masterOptionRepository;

    public MasterResponse getMaster(Long masterId) {
        Master master = masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.MASTER_NOT_FOUND));

        return MasterResponse.of(
                master,
                masterOptionRepository.findByMasterIdOrderByDisplayOrderAsc(masterId)
        );
    }
}
