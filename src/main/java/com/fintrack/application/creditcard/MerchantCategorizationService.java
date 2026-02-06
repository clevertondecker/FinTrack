package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CategorizationSource;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.MerchantCategoryRule;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.creditcard.MerchantCategoryRuleJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing merchant categorization rules.
 *
 * <p>This service handles:
 * <ul>
 *   <li>Creating new rules when users manually categorize items</li>
 *   <li>Updating existing rules based on user confirmations/overrides</li>
 *   <li>Applying rules to new invoice items during import</li>
 *   <li>Finding matching rules for merchant keys</li>
 * </ul>
 */
@Service
@Transactional
public class MerchantCategorizationService {

    private static final Logger logger = LoggerFactory.getLogger(MerchantCategorizationService.class);

    private final MerchantCategoryRuleJpaRepository ruleRepository;
    private final MerchantNormalizationService normalizationService;

    public MerchantCategorizationService(
            final MerchantCategoryRuleJpaRepository ruleRepository,
            final MerchantNormalizationService normalizationService) {
        this.ruleRepository = ruleRepository;
        this.normalizationService = normalizationService;
    }

    /**
     * Result of attempting to apply a categorization rule.
     *
     * @param applied whether a rule was applied
     * @param rule the rule that was applied, if any
     * @param source the categorization source
     */
    public record CategorizationResult(
            boolean applied,
            MerchantCategoryRule rule,
            CategorizationSource source) {

        /**
         * Creates a result indicating no rule was applied.
         */
        public static CategorizationResult notApplied() {
            return new CategorizationResult(false, null, null);
        }

        /**
         * Creates a result indicating a rule was auto-applied.
         */
        public static CategorizationResult autoApplied(final MerchantCategoryRule rule) {
            return new CategorizationResult(true, rule, CategorizationSource.AUTO_RULE);
        }

        /**
         * Creates a result indicating a rule was suggested (not auto-applied).
         */
        public static CategorizationResult suggested(final MerchantCategoryRule rule) {
            return new CategorizationResult(true, rule, CategorizationSource.SUGGESTED);
        }
    }

    /**
     * Attempts to apply a categorization rule to an invoice item.
     *
     * <p>This method:
     * <ol>
     *   <li>Normalizes the item's description to a merchant key</li>
     *   <li>Looks up an existing rule for the user and merchant key</li>
     *   <li>If found and auto-apply is enabled, applies the category</li>
     *   <li>If found but auto-apply is disabled, suggests the category</li>
     * </ol>
     *
     * @param item the invoice item to categorize. Must not be null.
     * @param user the user who owns the item. Must not be null.
     * @return the categorization result. Never null.
     */
    public CategorizationResult applyRule(final InvoiceItem item, final User user) {
        // Normalize the description
        String merchantKey = normalizationService.normalize(item.getDescription());

        if (merchantKey == null) {
            logger.debug("Could not normalize description: {}", item.getDescription());
            return CategorizationResult.notApplied();
        }

        // Store the merchant key on the item
        item.setMerchantKey(merchantKey);

        // Look up existing rule
        Optional<MerchantCategoryRule> ruleOpt = ruleRepository.findByUserAndMerchantKey(user, merchantKey);

        if (ruleOpt.isEmpty()) {
            logger.debug("No rule found for merchant key: {}", merchantKey);
            return CategorizationResult.notApplied();
        }

        MerchantCategoryRule rule = ruleOpt.get();

        // Check if rule should be auto-applied
        if (rule.shouldAutoApply()) {
            // Auto-apply the category
            item.updateCategory(rule.getCategory(), CategorizationSource.AUTO_RULE, rule);
            rule.recordApplication();
            ruleRepository.save(rule);

            logger.info("Auto-applied category '{}' to item '{}' (rule: {})",
                rule.getCategory().getName(), item.getDescription(), rule.getId());

            return CategorizationResult.autoApplied(rule);
        } else {
            // Suggest but don't apply
            logger.debug("Suggesting category '{}' for item '{}' (rule not yet auto-apply)",
                rule.getCategory().getName(), item.getDescription());

            return CategorizationResult.suggested(rule);
        }
    }

    /**
     * Records a manual categorization and updates/creates rules accordingly.
     *
     * <p>This method:
     * <ol>
     *   <li>Normalizes the item's description to a merchant key</li>
     *   <li>If no rule exists, creates a new one</li>
     *   <li>If rule exists with same category, confirms it</li>
     *   <li>If rule exists with different category, overrides it</li>
     * </ol>
     *
     * @param item the invoice item being categorized. Must not be null.
     * @param category the category being assigned. Must not be null.
     * @param user the user performing the categorization. Must not be null.
     * @return the rule that was created or updated. May be null if normalization fails.
     */
    public MerchantCategoryRule recordManualCategorization(
            final InvoiceItem item,
            final Category category,
            final User user) {

        // Normalize the description
        String merchantKey = item.getMerchantKey();
        if (merchantKey == null) {
            merchantKey = normalizationService.normalize(item.getDescription());
        }

        if (merchantKey == null) {
            logger.debug("Could not normalize description for rule creation: {}", item.getDescription());
            return null;
        }

        // Store/update the merchant key on the item
        item.setMerchantKey(merchantKey);

        // Look up existing rule
        Optional<MerchantCategoryRule> existingRuleOpt = 
            ruleRepository.findByUserAndMerchantKey(user, merchantKey);

        MerchantCategoryRule rule;

        if (existingRuleOpt.isEmpty()) {
            // Create new rule
            rule = MerchantCategoryRule.of(user, merchantKey, item.getDescription(), category);
            rule = ruleRepository.save(rule);

            logger.info("Created new categorization rule: {} -> {} (user: {})",
                merchantKey, category.getName(), user.getEmail());

        } else {
            rule = existingRuleOpt.get();

            // Check if same category or different
            if (rule.getCategory().getId().equals(category.getId())) {
                // Same category - confirm
                rule.recordConfirmation();
                rule = ruleRepository.save(rule);

                logger.info("Confirmed categorization rule: {} -> {} (confirmations: {})",
                    merchantKey, category.getName(), rule.getTimesConfirmed());

            } else {
                // Different category - override
                Category oldCategory = rule.getCategory();
                rule.recordOverride(category);
                rule = ruleRepository.save(rule);

                logger.info("Overrode categorization rule: {} from '{}' to '{}' (overrides: {})",
                    merchantKey, oldCategory.getName(), category.getName(), rule.getTimesOverridden());
            }
        }

        return rule;
    }

    /**
     * Applies categorization rules to a list of invoice items.
     *
     * @param items the items to categorize. Must not be null.
     * @param user the user who owns the items. Must not be null.
     * @return the number of items that were auto-categorized.
     */
    public int applyRulesToItems(final List<InvoiceItem> items, final User user) {
        int categorized = 0;

        for (InvoiceItem item : items) {
            // Skip already categorized items
            if (item.getCategory() != null) {
                continue;
            }

            CategorizationResult result = applyRule(item, user);
            if (result.applied() && result.source() == CategorizationSource.AUTO_RULE) {
                categorized++;
            }
        }

        logger.info("Auto-categorized {} of {} items for user {}",
            categorized, items.size(), user.getEmail());

        return categorized;
    }

    /**
     * Finds a rule by user and merchant key.
     *
     * @param user the user. Must not be null.
     * @param merchantKey the normalized merchant key. Must not be null.
     * @return an Optional containing the rule if found. Never null.
     */
    @Transactional(readOnly = true)
    public Optional<MerchantCategoryRule> findRule(final User user, final String merchantKey) {
        return ruleRepository.findByUserAndMerchantKey(user, merchantKey);
    }

    /**
     * Gets all rules for a user.
     *
     * @param user the user. Must not be null.
     * @return a list of rules. Never null, may be empty.
     */
    @Transactional(readOnly = true)
    public List<MerchantCategoryRule> getUserRules(final User user) {
        return ruleRepository.findByUser(user);
    }

    /**
     * Normalizes a description to a merchant key.
     *
     * @param description the description to normalize. Can be null.
     * @return the normalized merchant key, or null if normalization fails.
     */
    public String normalizeMerchantKey(final String description) {
        return normalizationService.normalize(description);
    }
}
