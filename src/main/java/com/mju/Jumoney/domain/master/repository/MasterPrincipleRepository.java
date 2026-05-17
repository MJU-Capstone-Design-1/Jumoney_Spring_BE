package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.MasterPrinciple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MasterPrincipleRepository extends JpaRepository<MasterPrinciple, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from MasterPrinciple masterPrinciple
            where masterPrinciple.master.id in :masterIds
            """)
    void deleteByMasterIdIn(@Param("masterIds") Collection<Long> masterIds);

    List<MasterPrinciple> findByMasterIdOrderByIdAsc(Long masterId);
}
