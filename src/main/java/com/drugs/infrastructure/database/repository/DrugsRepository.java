package com.drugs.infrastructure.database.repository;

import com.drugs.infrastructure.database.entity.DrugsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface DrugsRepository extends JpaRepository<DrugsEntity, Integer> {

    Logger logger = LoggerFactory.getLogger(DrugsRepository.class);

    Page<DrugsEntity> findAll(Pageable pageable);

    List<DrugsEntity> findAllByDrugsNameIgnoreCase(String name);

    List<DrugsEntity> findByExpirationDateBetweenOrderByExpirationDateAsc(
            OffsetDateTime start,
            OffsetDateTime end
    );

    List<DrugsEntity> findByDrugsDescriptionIgnoreCaseContaining(String text);

    long countByExpirationDateBefore(OffsetDateTime now);

    long countByAlertSentTrue();

    @Query("SELECT d.drugsForm.name, COUNT(d) FROM DrugsEntity d GROUP BY d.drugsForm.name")
    List<Object[]> countGroupedByForm();

    List<DrugsEntity> findByExpirationDateBetweenAndAlertSentFalse(
            OffsetDateTime start,
            OffsetDateTime end
    );

    default void logQuery(String query) {
        logger.info("Executing query: {}", query);
    }
}
