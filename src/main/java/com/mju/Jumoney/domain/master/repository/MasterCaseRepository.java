package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.MasterCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface MasterCaseRepository extends JpaRepository<MasterCase, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from MasterCase masterCase
            where masterCase.master.id in :masterIds
            """)
    void deleteByMasterIdIn(@Param("masterIds") Collection<Long> masterIds);
}
