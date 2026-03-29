package com.fintrack.infrastructure.persistence.subscription;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.subscription.Subscription;
import com.fintrack.domain.subscription.SubscriptionRepository;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionJpaRepository extends JpaRepository<Subscription, Long>, SubscriptionRepository {

    @Override
    @Query("SELECT s FROM Subscription s LEFT JOIN FETCH s.category LEFT JOIN FETCH s.creditCard "
         + "WHERE s.owner = :owner AND s.active = true ORDER BY s.name")
    List<Subscription> findByOwnerAndActiveTrue(@Param("owner") User owner);

    @Override
    Optional<Subscription> findByIdAndOwner(Long id, User owner);

    @Override
    boolean existsByOwnerAndMerchantKeyAndActiveTrue(User owner, String merchantKey);

    @Override
    Optional<Subscription> findByOwnerAndMerchantKeyAndActiveTrue(User owner, String merchantKey);

    @Override
    List<Subscription> findByOwner(User owner);

    long countByCategory(Category category);

    List<Subscription> findByCategory(Category category);
}
