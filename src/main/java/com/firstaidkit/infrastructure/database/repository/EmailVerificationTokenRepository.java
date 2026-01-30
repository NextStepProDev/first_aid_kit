package com.firstaidkit.infrastructure.database.repository;

import com.firstaidkit.infrastructure.database.entity.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, Integer> {

    @Query("SELECT t FROM EmailVerificationTokenEntity t WHERE t.token = :token AND t.expiresAt > :now AND t.verifiedAt IS NULL")
    Optional<EmailVerificationTokenEntity> findValidToken(@Param("token") String token, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity t WHERE t.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") OffsetDateTime now);
}
