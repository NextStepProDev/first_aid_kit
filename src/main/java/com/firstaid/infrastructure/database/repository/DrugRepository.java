package com.firstaid.infrastructure.database.repository;

import com.firstaid.infrastructure.database.entity.DrugEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface DrugRepository extends JpaRepository<DrugEntity, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<DrugEntity> {

    @NonNull
    Page<DrugEntity> findAll(@NonNull Pageable pageable);

    long countByExpirationDateBefore(OffsetDateTime now);

    long countByAlertSentTrue();

    @Query("SELECT d.drugForm.name, COUNT(d) FROM DrugEntity d GROUP BY d.drugForm.name")
    List<Object[]> countGroupedByForm();

    List<DrugEntity> findByExpirationDateLessThanEqualAndAlertSentFalse(OffsetDateTime date);
}
