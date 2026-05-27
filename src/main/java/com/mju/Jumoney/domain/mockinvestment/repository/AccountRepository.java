package com.mju.Jumoney.domain.mockinvestment.repository;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.user.id = :userId")
    Optional<Account> findByUserIdWithLock(@Param("userId") Long userId);

    @Query("""
            select a
            from Account a
            join fetch a.user u
            left join fetch u.selectedMaster
            where u.verifiedOperationAccount = false
              and not (u.provider = com.mju.Jumoney.domain.user.enums.AuthProvider.KAKAO and u.providerId like 'DEV_%')
            """)
    List<Account> findAllWithActiveUsers();

    List<Account> findByUserIdIn(Collection<Long> userIds);

    boolean existsByUserId(Long userId);
}
