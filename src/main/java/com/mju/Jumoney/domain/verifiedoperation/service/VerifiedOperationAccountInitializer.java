package com.mju.Jumoney.domain.verifiedoperation.service;

import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentAccountService;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.enums.AuthProvider;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifiedOperationAccountInitializer {

    private final VerifiedOperationAccountConfigService configService;
    private final UserRepository userRepository;
    private final MockInvestmentAccountService mockInvestmentAccountService;

    @Transactional
    public void initializeAccounts() {
        int initializedCount = 0;
        for (VerifiedOperationAccountConfig config : configService.getAccounts()) {
            User user = upsertSystemUser(config);
            mockInvestmentAccountService.getOrInitializeAccount(user.getId());
            initializedCount++;
        }
        log.info(" 모의 운용 계정 {}개 초기화/갱신 완료", initializedCount);
    }

    private User upsertSystemUser(VerifiedOperationAccountConfig config) {
        return userRepository.findByProviderAndProviderIdIncludeDeleted(AuthProvider.LOCAL.name(), config.providerId())
                .map(user -> {
                    user.updateNickname(config.accountName());
                    user.updateServiceNickname(config.accountName());
                    user.markAsVerifiedOperationAccount();
                    return user;
                })
                .orElseGet(() -> {
                    User user = User.of(AuthProvider.LOCAL, config.providerId(), config.accountName());
                    user.updateServiceNickname(config.accountName());
                    user.markAsVerifiedOperationAccount();
                    return userRepository.save(user);
                });
    }
}
