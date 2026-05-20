package com.mju.Jumoney.domain.mockinvestment.repository;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findByUserIdWithLock(Long userId);

    boolean existsByUserId(Long userId);
}
