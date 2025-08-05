package com.firstaid.infrastructure.database.repository;

import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DrugFormRepository extends JpaRepository<DrugFormEntity, Integer> {
    Optional<DrugFormEntity> findByName(String pill);

    Optional<DrugFormEntity> findByNameIgnoreCase(String pills);
}
