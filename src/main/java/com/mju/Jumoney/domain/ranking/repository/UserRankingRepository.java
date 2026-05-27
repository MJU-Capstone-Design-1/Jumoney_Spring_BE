package com.mju.Jumoney.domain.ranking.repository;

import com.mju.Jumoney.domain.ranking.domain.UserRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRankingRepository extends JpaRepository<UserRanking, Long> {

    @Query("""
            select ur
            from UserRanking ur
            join fetch ur.user
            left join fetch ur.master
            where ur.overallRank <= :limit
            order by ur.overallRank asc
            """)
    List<UserRanking> findTopOverallWithUserAndMaster(@Param("limit") int limit);

    @Query("""
            select ur
            from UserRanking ur
            join fetch ur.user
            join fetch ur.master m
            where m.id = :masterId
              and ur.masterRank is not null
              and ur.masterRank <= :limit
            order by ur.masterRank asc
            """)
    List<UserRanking> findTopByMasterIdWithUserAndMaster(@Param("masterId") Long masterId, @Param("limit") int limit);
}
