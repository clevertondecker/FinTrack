package com.fintrack.application.creditcard;

import com.fintrack.domain.budget.Budget;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.MerchantCategoryRule;
import com.fintrack.domain.subscription.Subscription;
import com.fintrack.dto.creditcard.CategoryUsageResponse;
import com.fintrack.infrastructure.persistence.budget.BudgetJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceItemJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.MerchantCategoryRuleJpaRepository;
import com.fintrack.infrastructure.persistence.subscription.SubscriptionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryJpaRepository categoryRepository;
    private final InvoiceItemJpaRepository invoiceItemRepository;
    private final MerchantCategoryRuleJpaRepository ruleRepository;
    private final BudgetJpaRepository budgetRepository;
    private final SubscriptionJpaRepository subscriptionRepository;

    public CategoryService(CategoryJpaRepository categoryRepository,
                           InvoiceItemJpaRepository invoiceItemRepository,
                           MerchantCategoryRuleJpaRepository ruleRepository,
                           BudgetJpaRepository budgetRepository,
                           SubscriptionJpaRepository subscriptionRepository) {
        this.categoryRepository = categoryRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.ruleRepository = ruleRepository;
        this.budgetRepository = budgetRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional
    public Category create(String name, String color, String icon) {
        if (categoryRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("A category with name '" + name + "' already exists.");
        }
        Category category = Category.of(name.trim(), color);
        category.setIcon(icon);

        Integer maxOrder = categoryRepository.findAll().stream()
                .map(Category::getDisplayOrder)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .orElse(0);
        category.setDisplayOrder(maxOrder + 1);

        return categoryRepository.save(category);
    }

    @Transactional
    public Category create(String name, String color) {
        return create(name, color, null);
    }

    @Transactional
    public Category update(Long id, String name, String color, String icon) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        if (categoryRepository.existsByNameAndIdNot(name.trim(), id)) {
            throw new IllegalArgumentException("A category with name '" + name + "' already exists.");
        }

        category.setName(name.trim());
        category.setColor(color);
        category.setIcon(icon);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        long itemCount = invoiceItemRepository.countByCategory(category);
        if (itemCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category '" + category.getName()
                    + "' because it is used by " + itemCount + " invoice items. "
                    + "Use merge to move items to another category first.");
        }

        long ruleCount = ruleRepository.countByCategory(category);
        if (ruleCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category '" + category.getName()
                    + "' because it is used by " + ruleCount + " merchant rules. "
                    + "Use merge to move rules to another category first.");
        }

        long budgetCount = budgetRepository.countByCategory(category);
        if (budgetCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category '" + category.getName()
                    + "' because it is used by " + budgetCount + " budgets. "
                    + "Use merge to move budgets to another category first.");
        }

        long subCount = subscriptionRepository.countByCategory(category);
        if (subCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category '" + category.getName()
                    + "' because it is used by " + subCount + " subscriptions. "
                    + "Use merge to move subscriptions to another category first.");
        }

        categoryRepository.delete(category);
        LOG.info("Deleted category: {} (id={})", category.getName(), id);
    }

    @Transactional
    public Category merge(Long sourceCategoryId, Long targetCategoryId) {
        if (sourceCategoryId.equals(targetCategoryId)) {
            throw new IllegalArgumentException("Source and target categories must be different.");
        }

        Category source = categoryRepository.findById(sourceCategoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source category not found with id: " + sourceCategoryId));
        Category target = categoryRepository.findById(targetCategoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Target category not found with id: " + targetCategoryId));

        List<InvoiceItem> items = invoiceItemRepository.findByCategory(source);
        for (InvoiceItem item : items) {
            item.updateCategory(target);
        }
        invoiceItemRepository.saveAll(items);

        List<MerchantCategoryRule> rules = ruleRepository.findByCategory(source);
        for (MerchantCategoryRule rule : rules) {
            rule.recordOverride(target);
        }
        ruleRepository.saveAll(rules);

        List<Budget> budgets = budgetRepository.findByCategory(source);
        for (Budget budget : budgets) {
            budget.assignCategory(target);
        }
        budgetRepository.saveAll(budgets);

        List<Subscription> subs = subscriptionRepository.findByCategory(source);
        for (Subscription sub : subs) {
            sub.assignCategory(target);
        }
        subscriptionRepository.saveAll(subs);

        categoryRepository.delete(source);

        LOG.info("Merged category '{}' into '{}'. "
                + "Migrated {} items, {} rules, {} budgets, {} subs.",
                source.getName(), target.getName(),
                items.size(), rules.size(),
                budgets.size(), subs.size());

        return target;
    }

    @Transactional
    public void reorder(List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            Long catId = orderedIds.get(i);
            Category category = categoryRepository.findById(catId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + catId));
            category.setDisplayOrder(i + 1);
            categoryRepository.save(category);
        }
    }

    @Transactional(readOnly = true)
    public CategoryUsageResponse getUsage(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));

        return new CategoryUsageResponse(
                category.getId(),
                category.getName(),
                invoiceItemRepository.countByCategory(category),
                ruleRepository.countByCategory(category),
                budgetRepository.countByCategory(category),
                subscriptionRepository.countByCategory(category)
        );
    }
}
