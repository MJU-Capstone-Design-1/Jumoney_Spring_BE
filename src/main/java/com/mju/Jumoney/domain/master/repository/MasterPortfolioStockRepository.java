package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.MasterPortfolioStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MasterPortfolioStockRepository extends JpaRepository<MasterPortfolioStock, Long> {

    List<MasterPortfolioStock> findByMasterIdOrderByWeightDesc(Long masterId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from MasterPortfolioStock masterPortfolioStock
            where masterPortfolioStock.master.id in :masterIds
            """)
    void deleteByMasterIdIn(@Param("masterIds") Collection<Long> masterIds);
}
