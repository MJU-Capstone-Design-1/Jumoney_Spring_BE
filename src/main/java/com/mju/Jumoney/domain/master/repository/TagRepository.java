package com.mju.Jumoney.domain.master.repository;

import com.mju.Jumoney.domain.master.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByTagName(String tagName);
}
