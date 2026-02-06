package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.MerchantCategoryRule;
import com.fintrack.domain.creditcard.MerchantCategoryRuleRepository;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository implementation for MerchantCategoryRule entities.
 * Extends both JpaRepository for Spring Data JPA functionality
 * and MerchantCategoryRuleRepository for domain interface compliance.
 */
@Repository
public interface MerchantCategoryRuleJpaRepository
        extends JpaRepository<MerchantCategoryRule, Long>, MerchantCategoryRuleRepository {

    /**
     * Finds a rule by user and merchant key.
     *
     * @param user the user who owns the rule. Must not be null.
     * @param merchantKey the normalized merchant key. Must not be null.
     * @return an Optional containing the rule if found. Never null.
     */
    @Override
    Optional<MerchantCategoryRule> findByUserAndMerchantKey(User user, String merchantKey);

    /**
     * Finds all rules for a user.
     *
     * @param user the user. Must not be null.
     * @return a list of rules. Never null, may be empty.
     */
    @Override
    List<MerchantCategoryRule> findByUser(User user);

    /**
     * Finds all auto-apply rules for a user.
     *
     * @param user the user. Must not be null.
     * @return a list of auto-apply rules. Never null, may be empty.
     */
    @Override
    List<MerchantCategoryRule> findByUserAndAutoApplyTrue(User user);
}
