package com.mju.Jumoney.domain.mockinvestment.repository;

import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByAccountId(Long accountId);

    List<Portfolio> findByAccountIdIn(Collection<Long> accountIds);

    List<Portfolio> findByAccountIdOrderByUpdatedAtDesc(Long accountId);

    Optional<Portfolio> findByAccountIdAndStockId(Long accountId, Long stockId);

    void deleteByAccountId(Long accountId);
}
