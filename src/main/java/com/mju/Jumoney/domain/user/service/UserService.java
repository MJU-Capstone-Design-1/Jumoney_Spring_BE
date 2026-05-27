package com.mju.Jumoney.domain.user.service;

import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.dto.UserInfoDTO;
import com.mju.Jumoney.domain.user.dto.UserUpdateDTO;
import com.mju.Jumoney.domain.user.exception.UserErrorCode;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "RT:";
    private static final long WITHDRAWAL_GRACE_PERIOD_DAYS = 7L;

    private final UserRepository userRepository;
    private final RedisUtil redisUtil;

    @Transactional
    public UserUpdateDTO.Response updateServiceNickname(Long userId, UserUpdateDTO.Request request) {
        // 유저 조회
        User user = findUserById(userId);

        // 닉네임 중복 검증
        validateDuplicateNickname(request.serviceNickname(), user.getId());

        // 서비스 닉네임 업데이트
        user.updateServiceNickname(request.serviceNickname());

        log.info("[UserService] 회원 닉네임 수정 완료 - User ID: {}, Nickname: {}", user.getId(), user.getServiceNickname());

        return UserUpdateDTO.Response.builder()
                .userId(user.getId())
                .serviceNickname(user.getServiceNickname())
                .build();
    }

    public UserInfoDTO.Response getUserInfo(Long userId) {
        User user = findUserById(userId);

        Long selectedMasterId = user.getSelectedMaster() == null ? null : user.getSelectedMaster().getId();
        String nickname = user.getServiceNickname() != null ? user.getServiceNickname() : user.getNickname();

        return UserInfoDTO.Response.builder()
                .nickname(nickname)
                .selectedMasterId(selectedMasterId)
                .build();
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = findUserById(userId);
        redisUtil.delete(getRefreshTokenKey(userId));

        if (user.isDevAccount()) {
            userRepository.hardDeleteById(userId);
            log.info("[UserService] 개발자 임시 계정 영구 탈퇴 완료 - User ID: {}", userId);
            return;
        }

        user.softDelete();

        log.info("[UserService] 회원 탈퇴 완료 - User ID: {}, permanentDeletionAfterDays: {}", userId, WITHDRAWAL_GRACE_PERIOD_DAYS);
    }

    @Scheduled(cron = "${user.withdrawal.cleanup.cron:0 0 * * * *}", zone = "${user.withdrawal.cleanup.zone-id:Asia/Seoul}")
    @Transactional
    public void cleanupWithdrawnUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(WITHDRAWAL_GRACE_PERIOD_DAYS);
        int deletedCount = userRepository.hardDeleteWithdrawnKakaoUsersBefore(cutoff);
        if (deletedCount > 0) {
            log.info("[UserService] 탈퇴 유예기간 경과 회원 영구 삭제 완료 - count: {}, cutoff: {}", deletedCount, cutoff);
        }
    }

    // ========== 검증 메서드 ==========
    private void validateDuplicateNickname(String serviceNickname, Long userId) {
        if (userRepository.existsByServiceNicknameAndIdNot(serviceNickname, userId)) {
            throw new CustomException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }

    // ========== 조회 메서드 ==========
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private String getRefreshTokenKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}
