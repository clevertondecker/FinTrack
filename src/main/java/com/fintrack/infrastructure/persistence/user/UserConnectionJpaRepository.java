package com.fintrack.infrastructure.persistence.user;

import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConnectionJpaRepository extends JpaRepository<UserConnection, Long> {

    @Query("SELECT uc.connectedUser FROM UserConnection uc WHERE uc.user = :user")
    List<User> findConnectedUsers(@Param("user") User user);

    Optional<UserConnection> findByUserAndConnectedUser(User user, User connectedUser);
}
