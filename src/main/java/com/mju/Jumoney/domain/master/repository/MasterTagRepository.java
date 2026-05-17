package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.MasterTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MasterTagRepository extends JpaRepository<MasterTag, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from MasterTag masterTag
            where masterTag.master.id in :masterIds
            """)
    void deleteByMasterIdIn(@Param("masterIds") Collection<Long> masterIds);

    @Query("""
            select masterTag
            from MasterTag masterTag
            join fetch masterTag.tag
            where masterTag.master.id in :masterIds
            order by masterTag.id asc
            """)
    List<MasterTag> findByMasterIdInWithTag(@Param("masterIds") Collection<Long> masterIds);
}
