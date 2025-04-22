package com.drugs.infrastructure.database.repository;

import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DrugsFormRepository extends JpaRepository<DrugsFormEntity, Integer> {
    Optional<DrugsFormEntity> findByName(String pill);
}
