package com.firstaid.infrastructure.database.repository;

import com.firstaid.infrastructure.database.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Integer> {

    @Query("SELECT t FROM PasswordResetTokenEntity t WHERE t.token = :token AND t.expiresAt > :now AND t.usedAt IS NULL")
    Optional<PasswordResetTokenEntity> findValidToken(@Param("token") String token, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") OffsetDateTime now);
}
