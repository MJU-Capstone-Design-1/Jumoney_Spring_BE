package com.mju.Jumoney.domain.user.domain;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.user.enums.AuthProvider;
import com.mju.Jumoney.domain.user.enums.Role;
import com.mju.Jumoney.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "users",
        // 복합 유니크 키: 추후 다른 소셜 로그인이 붙더라도 ID 충돌 방지
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider_providerid", columnNames = {"provider", "provider_id"})
        },
        // 인덱스: 카카오 로그인 시 빠른 조회를 위한 단일 인덱스 유지
        indexes = {
                @Index(name = "idx_user_provider_id", columnList = "provider_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "service_nickname", length = 100)
    private String serviceNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "verified_operation_account", nullable = false)
    private boolean verifiedOperationAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_master_id")
    private Master selectedMaster;

    // ========== 비즈니스 메서드 ==========
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateServiceNickname(String serviceNickname) {
        this.serviceNickname = serviceNickname;
    }

    public void updateSelectedMaster(Master selectedMaster) {
        this.selectedMaster = selectedMaster;
    }

    public void markAsVerifiedOperationAccount() {
        this.verifiedOperationAccount = true;
    }

    public boolean isDevAccount() {
        return provider == AuthProvider.KAKAO && providerId != null && providerId.startsWith("DEV_");
    }

    // OAuth2 로그인용 정적 팩토리 메서드
    public static User of(AuthProvider provider, String providerId, String nickname) {
        return User.builder()
                .provider(provider)
                .providerId(providerId)
                .nickname(nickname)
                .role(Role.USER)
                .verifiedOperationAccount(false)
                .build();
    }
}
