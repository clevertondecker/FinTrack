package com.fintrack.domain.subscription;

import com.fintrack.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

    Subscription save(Subscription subscription);

    Optional<Subscription> findByIdAndOwner(Long id, User owner);

    List<Subscription> findByOwnerAndActiveTrue(User owner);

    boolean existsByOwnerAndMerchantKeyAndActiveTrue(User owner, String merchantKey);

    Optional<Subscription> findByOwnerAndMerchantKeyAndActiveTrue(User owner, String merchantKey);
}
