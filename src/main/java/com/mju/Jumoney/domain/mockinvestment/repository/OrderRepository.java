package com.mju.Jumoney.domain.mockinvestment.repository;

import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByAccountIdOrderByExecutedAtDesc(Long accountId);

    void deleteByAccountId(Long accountId);
}
