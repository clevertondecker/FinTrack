package com.fintrack.infrastructure.persistence.user;

import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
    @Override
    Optional<User> findByEmail(Email email);
    @Override
    Optional<User> findByName(String name);
} 