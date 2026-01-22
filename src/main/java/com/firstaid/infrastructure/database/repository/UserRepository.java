package com.firstaid.infrastructure.database.repository;

import com.firstaid.infrastructure.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    UserEntity findByEmail(String email);

    UserEntity findByUserName(String userName);

    Optional<UserEntity> findByUserId(Integer userId);

    boolean existsByUserName(String userName);

    boolean existsByEmail(String email);
}
