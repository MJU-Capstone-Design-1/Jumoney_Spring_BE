package com.mju.Jumoney.domain.mockinvestment.repository;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.user.id = :userId")
    Optional<Account> findByUserIdWithLock(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
