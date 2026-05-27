package com.mju.Jumoney.domain.user.repository;

import com.mju.Jumoney.domain.user.domain.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // @SQLRestriction을 무시하고 탈퇴한 회원까지 찾아오기 위한 쿼리 (탈퇴 후 재가입 시 Unique 에러 방지 및 계정 복구를 위함)
    @Query(value = "SELECT * FROM users WHERE provider = :provider AND provider_id = :providerId", nativeQuery = true)
    Optional<User> findByProviderAndProviderIdIncludeDeleted(@Param("provider") String provider, @Param("providerId") String providerId);

    // 개발자용 임시 로그인을 위한 닉네임 기반 조회
    Optional<User> findByNickname(String nickname);

    // 서비스 닉네임 중복 여부 확인
    boolean existsByServiceNickname(String serviceNickname);

    boolean existsByServiceNicknameAndIdNot(String serviceNickname, Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM users WHERE user_id = :userId", nativeQuery = true)
    int hardDeleteById(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM users
            WHERE deleted_at IS NOT NULL
              AND deleted_at < :cutoff
              AND provider = 'KAKAO'
              AND provider_id NOT LIKE 'DEV_%'
            """, nativeQuery = true)
    int hardDeleteWithdrawnKakaoUsersBefore(@Param("cutoff") LocalDateTime cutoff);
}
