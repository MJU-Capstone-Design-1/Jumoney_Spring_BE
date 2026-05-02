package com.mju.Jumoney.domain.user.repository;

import com.mju.Jumoney.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // @SQLRestriction을 무시하고 탈퇴한 회원까지 찾아오기 위한 쿼리 (탈퇴 후 재가입 시 Unique 에러 방지 및 계정 복구를 위함)
    @Query(value = "SELECT * FROM users WHERE provider = :provider AND provider_id = :providerId", nativeQuery = true)
    Optional<User> findByProviderAndProviderIdIncludeDeleted(@Param("provider") String provider, @Param("providerId") String providerId);

    // 개발자용 임시 로그인을 위한 닉네임 기반 조회
    Optional<User> findByNickname(String nickname);
}
