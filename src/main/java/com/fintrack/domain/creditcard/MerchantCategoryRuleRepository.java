package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MerchantCategoryRule entities.
 * Provides methods for finding and managing merchant categorization rules.
 *
 * <p>Note: Common CRUD operations (save, delete, findById) are inherited from
 * the JPA repository implementation. This interface defines only domain-specific queries.
 */
public interface MerchantCategoryRuleRepository {

    /**
     * Finds a rule by user and merchant key.
     *
     * @param user the user who owns the rule. Must not be null.
     * @param merchantKey the normalized merchant key. Must not be null.
     * @return an Optional containing the rule if found. Never null.
     */
    Optional<MerchantCategoryRule> findByUserAndMerchantKey(User user, String merchantKey);

    /**
     * Finds all rules for a user.
     *
     * @param user the user. Must not be null.
     * @return a list of rules. Never null, may be empty.
     */
    List<MerchantCategoryRule> findByUser(User user);

    /**
     * Finds all auto-apply rules for a user.
     *
     * @param user the user. Must not be null.
     * @return a list of auto-apply rules. Never null, may be empty.
     */
    List<MerchantCategoryRule> findByUserAndAutoApplyTrue(User user);
}
