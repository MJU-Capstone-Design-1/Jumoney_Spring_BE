package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.Master;
import com.mju.Jumoney.domain.master.enums.MasterCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterRepository extends JpaRepository<Master, Long> {

    Optional<Master> findByMasterCode(MasterCode masterCode);

    Optional<Master> findByMasterName(String masterName);

    List<Master> findAllByOrderByDisplayOrderAsc();
}
