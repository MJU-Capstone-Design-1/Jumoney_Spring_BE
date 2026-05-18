package com.mju.Jumoney.domain.hojumoney.repository;

import com.mju.Jumoney.domain.hojumoney.domain.SurveyOptionRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SurveyOptionRestrictionRepository extends JpaRepository<SurveyOptionRestriction, Long> {

    List<SurveyOptionRestriction> findBySourceOptionId(Long sourceOptionId);

    List<SurveyOptionRestriction> findBySourceOptionIdIn(Collection<Long> sourceOptionIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from SurveyOptionRestriction restriction
            where restriction.sourceOption.id in :sourceOptionIds
            """)
    void deleteBySourceOptionIdIn(@Param("sourceOptionIds") Collection<Long> sourceOptionIds);
}
