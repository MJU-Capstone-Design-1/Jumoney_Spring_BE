package com.mju.Jumoney.domain.mockinvestment.service;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentAccountResponse;
import com.mju.Jumoney.domain.mockinvestment.exception.MockInvestmentErrorCode;
import com.mju.Jumoney.domain.mockinvestment.repository.AccountRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.OrderRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.exception.UserErrorCode;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockInvestmentAccountService {

    private static final BigDecimal INITIAL_SEED_MONEY = BigDecimal.valueOf(10_000_000L);

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    @Transactional
    public MockInvestmentAccountResponse initializeAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .map(account -> toResponse(account, false))
                .orElseGet(() -> toResponse(createInitialAccount(userId), true));
    }

    @Transactional
    public Account getOrInitializeAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> createInitialAccount(userId));
    }

    public Account getRequiredAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(MockInvestmentErrorCode.MOCK_INVESTMENT_ACCOUNT_NOT_FOUND));
    }

    public void validateAccountExists(Long userId) {
        if (!accountRepository.existsByUserId(userId)) {
            throw new CustomException(MockInvestmentErrorCode.MOCK_INVESTMENT_ACCOUNT_NOT_FOUND);
        }
    }

    public Account getRequiredAccountWithLock(Long userId) {
        return accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new CustomException(MockInvestmentErrorCode.MOCK_INVESTMENT_ACCOUNT_NOT_FOUND));
    }

    @Transactional
    public void resetAccount(Long userId) {
        accountRepository.findByUserId(userId)
                .ifPresent(account -> {
                    orderRepository.deleteByAccountId(account.getId());
                    portfolioRepository.deleteByAccountId(account.getId());
                    accountRepository.delete(account);
                });
    }

    // ========== 비즈니스 메서드 ==========
    private Account createInitialAccount(Long userId) {
        User user = findUserById(userId);
        Account account = accountRepository.save(Account.create(user, INITIAL_SEED_MONEY));
        orderRepository.save(Order.createDeposit(account, INITIAL_SEED_MONEY));
        return account;
    }

    private MockInvestmentAccountResponse toResponse(Account account, boolean created) {
        return new MockInvestmentAccountResponse(
                account.getId(),
                account.getSeedMoney(),
                account.getCashBalance(),
                account.getTotalPurchaseAmount(),
                account.getTotalAsset(),
                account.getTotalProfitRate(),
                created
        );
    }

    // ========== 조회 메서드 ==========
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
