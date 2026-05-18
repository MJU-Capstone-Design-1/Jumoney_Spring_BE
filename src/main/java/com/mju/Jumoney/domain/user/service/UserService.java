package com.mju.Jumoney.domain.user.service;

import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.dto.UserUpdateDTO;
import com.mju.Jumoney.domain.user.exception.UserErrorCode;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserUpdateDTO.Response updateServiceNickname(Long userId, UserUpdateDTO.Request request) {
        // 유저 조회
        User user = findUserById(userId);

        // 닉네임 중복 검증
        validateDuplicateNickname(request.serviceNickname());

        // 서비스 닉네임 업데이트
        user.updateServiceNickname(request.serviceNickname());

        log.info("[UserService] 회원 닉네임 설정 완료 - User ID: {}, Nickname: {}", user.getId(), user.getServiceNickname());

        return UserUpdateDTO.Response.builder()
                .userId(user.getId())
                .serviceNickname(user.getServiceNickname())
                .build();
    }

    // ========== 검증 메서드 ==========
    private void validateDuplicateNickname(String serviceNickname) {
        if (userRepository.existsByServiceNickname(serviceNickname)) {
            throw new CustomException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }

    // ========== 조회 메서드 ==========
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
