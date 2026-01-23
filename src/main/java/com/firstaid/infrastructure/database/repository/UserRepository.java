package com.firstaid.infrastructure.database.repository;

import com.firstaid.infrastructure.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    UserEntity findByEmail(String email);

    UserEntity findByUserName(String userName);

    Optional<UserEntity> findByUserId(Integer userId);

    boolean existsByUserName(String userName);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u JOIN u.role r WHERE r.role = :roleName AND u.email <> :excludeEmail")
    List<UserEntity> findByRoleExcludingEmail(@Param("roleName") String roleName, @Param("excludeEmail") String excludeEmail);
}
