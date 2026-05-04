package com.mju.Jumoney.domain.sector.repository;

import com.mju.Jumoney.domain.sector.domain.Sector;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectorRepository extends JpaRepository<Sector, Long> {
    Optional<Sector> findBySectorName(SectorType sectorName);
}
