package com.mju.Jumoney.domain.mockinvestment.repository;

import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.enums.OrderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByAccountIdOrderByExecutedAtDesc(Long accountId);

    List<Order> findByAccountIdOrderByExecutedAtDesc(Long accountId, Pageable pageable);

    Optional<Order> findFirstByAccountIdAndOrderTypeInOrderByExecutedAtDesc(Long accountId, Collection<OrderType> orderTypes);

    List<Order> findByAccountIdAndOrderTypeOrderByExecutedAtAsc(Long accountId, OrderType orderType);

    List<Order> findByAccountIdInAndOrderTypeOrderByExecutedAtAsc(Collection<Long> accountIds, OrderType orderType);

    void deleteByAccountId(Long accountId);
}
