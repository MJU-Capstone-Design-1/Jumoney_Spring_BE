package com.mju.Jumoney.domain.mockinvestment.repository;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
