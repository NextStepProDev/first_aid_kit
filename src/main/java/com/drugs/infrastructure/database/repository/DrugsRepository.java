package com.drugs.infrastructure.database.repository;

import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface DrugsRepository extends JpaRepository<DrugsEntity, Integer> {

    @NonNull
    Page<DrugsEntity> findAll(@NonNull Pageable pageable);

    List<DrugsEntity> findByExpirationDateLessThanEqualOrderByExpirationDateAsc(OffsetDateTime until);

    List<DrugsEntity> findByDrugsDescriptionIgnoreCaseContaining(String text);

    long countByExpirationDateBefore(OffsetDateTime now);

    long countByAlertSentTrue();

    @Query("SELECT d.drugsForm.name, COUNT(d) FROM DrugsEntity d GROUP BY d.drugsForm.name")
    List<Object[]> countGroupedByForm();

    List<DrugsEntity> findByExpirationDateLessThanEqualAndAlertSentFalse(OffsetDateTime date);

    List<DrugsEntity> findByDrugsNameContainingIgnoreCase(String name);

    List<DrugsEntity> findByDrugsForm(DrugsFormEntity formEnum);
}
