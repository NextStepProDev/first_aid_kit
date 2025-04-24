package com.drugs.infrastructure.database.repository;

import com.drugs.infrastructure.database.entity.DrugsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface DrugsRepository extends JpaRepository<DrugsEntity, Integer> {

    Page<DrugsEntity> findAll(Pageable pageable);

    List<DrugsEntity> findAllByDrugsNameIgnoreCase(String name);

    List<DrugsEntity> findByExpirationDateBetweenOrderByExpirationDateAsc(
            OffsetDateTime start,
            OffsetDateTime end
    );

    List<DrugsEntity> findByDrugsDescriptionIgnoreCaseContaining(String text);

    List<DrugsEntity> findByExpirationDateBetween(OffsetDateTime now, OffsetDateTime oneMonthLater);

    long countByExpirationDateBefore(OffsetDateTime now);

    long countByAlertSentTrue();

    @Query("SELECT d.drugsForm.name, COUNT(d) FROM DrugsEntity d GROUP BY d.drugsForm.name")
    List<Object[]> countGroupedByForm();

    List<DrugsEntity> findByExpirationDateBetweenAndAlertSentFalse(
            OffsetDateTime start,
            OffsetDateTime end
    );

}
