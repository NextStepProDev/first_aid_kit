package com.drugs.infrastructure.database.repository;

import com.drugs.infrastructure.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @SuppressWarnings("unused")
    UserEntity findByEmail(String email);
    UserEntity findByUserName(String userName);

}
