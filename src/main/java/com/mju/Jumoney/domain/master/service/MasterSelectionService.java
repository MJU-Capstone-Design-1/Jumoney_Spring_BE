package com.mju.Jumoney.domain.master.service;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.dto.MasterSelectionResponse;
import com.mju.Jumoney.domain.master.repository.MasterRepository;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.exception.UserErrorCode;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterSelectionService {

    private final MasterRepository masterRepository;
    private final UserRepository userRepository;

    @Transactional
    public MasterSelectionResponse selectMaster(Long userId, Long masterId) {
        User user = findUserById(userId);
        Master master = findMasterById(masterId);

        user.updateSelectedMaster(master);

        return new MasterSelectionResponse(
                master.getId(),
                master.getMasterCode(),
                master.getMasterName()
        );
    }

    // ========== 조회 메서드 ==========
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private Master findMasterById(Long masterId) {
        return masterRepository.findById(masterId)
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.MASTER_NOT_FOUND));
    }
}
