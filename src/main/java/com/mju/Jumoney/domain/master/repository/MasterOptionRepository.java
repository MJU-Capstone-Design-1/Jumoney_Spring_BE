package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.MasterOption;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MasterOptionRepository extends JpaRepository<MasterOption, Long> {

    Optional<MasterOption> findByLogicCode(MasterOptionLogicCode logicCode);

    List<MasterOption> findByMasterIdOrderByDisplayOrderAsc(Long masterId);

    List<MasterOption> findByIdIn(Collection<Long> ids);
}
