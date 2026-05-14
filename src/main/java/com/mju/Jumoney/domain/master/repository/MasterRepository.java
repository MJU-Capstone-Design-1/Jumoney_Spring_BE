package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.Master;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterRepository extends JpaRepository<Master, Long> {

    Optional<Master> findByMasterName(String masterName);

    List<Master> findAllByOrderByDisplayOrderAsc();
}
