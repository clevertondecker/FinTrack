package com.fintrack.infrastructure.user;

import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(Email email);
} 