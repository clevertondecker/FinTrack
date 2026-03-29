package com.fintrack.application.creditcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fintrack.domain.budget.Budget;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.MerchantCategoryRule;
import com.fintrack.domain.subscription.BillingCycle;
import com.fintrack.domain.subscription.Subscription;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryUsageResponse;
import com.fintrack.infrastructure.persistence.budget.BudgetJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceItemJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.MerchantCategoryRuleJpaRepository;
import com.fintrack.infrastructure.persistence.subscription.SubscriptionJpaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceTest {

    @Mock
    private CategoryJpaRepository categoryRepository;

    @Mock
    private InvoiceItemJpaRepository invoiceItemRepository;

    @Mock
    private MerchantCategoryRuleJpaRepository ruleRepository;

    @Mock
    private BudgetJpaRepository budgetRepository;

    @Mock
    private SubscriptionJpaRepository subscriptionRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(
                categoryRepository, invoiceItemRepository,
                ruleRepository, budgetRepository, subscriptionRepository);
    }

    private Category createCategory(Long id, String name, String color) {
        Category cat = Category.of(name, color);
        ReflectionTestUtils.setField(cat, "id", id);
        return cat;
    }

    @SuppressWarnings("java:S3011")
    private InvoiceItem createInvoiceItem(Long id, Category category) {
        try {
            var constructor = InvoiceItem.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            InvoiceItem item = constructor.newInstance();
            ReflectionTestUtils.setField(item, "id", id);
            ReflectionTestUtils.setField(item, "category", category);
            ReflectionTestUtils.setField(item, "description", "Test item");
            ReflectionTestUtils.setField(item, "amount", java.math.BigDecimal.TEN);
            return item;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Should return categories ordered by displayOrder and name")
        void shouldReturnOrderedCategories() {
            Category cat1 = createCategory(1L, "Food", "#FF0000");
            cat1.setDisplayOrder(1);
            Category cat2 = createCategory(2L, "Transport", "#0000FF");
            cat2.setDisplayOrder(2);
            when(categoryRepository.findAllByOrderByDisplayOrderAscNameAsc())
                    .thenReturn(List.of(cat1, cat2));

            List<Category> result = categoryService.findAll();

            assertEquals(2, result.size());
            assertEquals("Food", result.get(0).getName());
            assertEquals("Transport", result.get(1).getName());
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void shouldReturnEmptyWhenNoCategories() {
            when(categoryRepository.findAllByOrderByDisplayOrderAscNameAsc())
                    .thenReturn(List.of());

            List<Category> result = categoryService.findAll();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should create category with name, color and icon")
        void shouldCreateCategoryWithAllFields() {
            when(categoryRepository.existsByName("Food")).thenReturn(false);
            when(categoryRepository.findAll()).thenReturn(List.of());
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(inv -> {
                        Category cat = inv.getArgument(0);
                        ReflectionTestUtils.setField(cat, "id", 1L);
                        return cat;
                    });

            Category result = categoryService.create("Food", "#FF0000", "utensils");

            assertNotNull(result.getId());
            assertEquals("Food", result.getName());
            assertEquals("#FF0000", result.getColor());
            assertEquals("utensils", result.getIcon());
            assertEquals(1, result.getDisplayOrder());
        }

        @Test
        @DisplayName("Should set displayOrder to max + 1")
        void shouldSetDisplayOrderCorrectly() {
            Category existing = createCategory(1L, "Existing", "#000");
            existing.setDisplayOrder(5);
            when(categoryRepository.existsByName("New")).thenReturn(false);
            when(categoryRepository.findAll()).thenReturn(List.of(existing));
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.create("New", "#FFF", null);

            assertEquals(6, result.getDisplayOrder());
        }

        @Test
        @DisplayName("Should throw when duplicate name exists")
        void shouldThrowOnDuplicateName() {
            when(categoryRepository.existsByName("Food")).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> categoryService.create("Food", "#FF0000", null));

            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Should trim name before saving")
        void shouldTrimName() {
            when(categoryRepository.existsByName("Food")).thenReturn(false);
            when(categoryRepository.findAll()).thenReturn(List.of());
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.create("  Food  ", "#FF0000", null);

            assertEquals("Food", result.getName());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should update category name, color and icon")
        void shouldUpdateCategory() {
            Category existing = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndIdNot("Alimentação", 1L)).thenReturn(false);
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.update(1L, "Alimentação", "#00FF00", "utensils");

            assertEquals("Alimentação", result.getName());
            assertEquals("#00FF00", result.getColor());
            assertEquals("utensils", result.getIcon());
        }

        @Test
        @DisplayName("Should throw when category not found")
        void shouldThrowWhenNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> categoryService.update(99L, "X", "#000", null));
        }

        @Test
        @DisplayName("Should throw when name conflicts with another category")
        void shouldThrowOnNameConflict() {
            Category existing = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndIdNot("Transport", 1L)).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> categoryService.update(1L, "Transport", "#000", null));

            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Should allow updating to same name (own name)")
        void shouldAllowSameNameUpdate() {
            Category existing = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndIdNot("Food", 1L)).thenReturn(false);
            when(categoryRepository.save(any(Category.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.update(1L, "Food", "#00FF00", null);

            assertEquals("Food", result.getName());
            assertEquals("#00FF00", result.getColor());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Should delete category with no usages")
        void shouldDeleteUnusedCategory() {
            Category cat = createCategory(1L, "Empty", "#000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
            when(invoiceItemRepository.countByCategory(cat)).thenReturn(0L);
            when(ruleRepository.countByCategory(cat)).thenReturn(0L);
            when(budgetRepository.countByCategory(cat)).thenReturn(0L);
            when(subscriptionRepository.countByCategory(cat)).thenReturn(0L);

            categoryService.delete(1L);

            verify(categoryRepository).delete(cat);
        }

        @Test
        @DisplayName("Should throw when category has invoice items")
        void shouldThrowWhenHasItems() {
            Category cat = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
            when(invoiceItemRepository.countByCategory(cat)).thenReturn(5L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.delete(1L));

            assertTrue(ex.getMessage().contains("5 invoice items"));
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw when category has merchant rules")
        void shouldThrowWhenHasRules() {
            Category cat = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
            when(invoiceItemRepository.countByCategory(cat)).thenReturn(0L);
            when(ruleRepository.countByCategory(cat)).thenReturn(3L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.delete(1L));

            assertTrue(ex.getMessage().contains("3 merchant rules"));
        }

        @Test
        @DisplayName("Should throw when category has budgets")
        void shouldThrowWhenHasBudgets() {
            Category cat = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
            when(invoiceItemRepository.countByCategory(cat)).thenReturn(0L);
            when(ruleRepository.countByCategory(cat)).thenReturn(0L);
            when(budgetRepository.countByCategory(cat)).thenReturn(2L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.delete(1L));

            assertTrue(ex.getMessage().contains("2 budgets"));
        }

        @Test
        @DisplayName("Should throw when category has subscriptions")
        void shouldThrowWhenHasSubscriptions() {
            Category cat = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
            when(invoiceItemRepository.countByCategory(cat)).thenReturn(0L);
            when(ruleRepository.countByCategory(cat)).thenReturn(0L);
            when(budgetRepository.countByCategory(cat)).thenReturn(0L);
            when(subscriptionRepository.countByCategory(cat)).thenReturn(1L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> categoryService.delete(1L));

            assertTrue(ex.getMessage().contains("1 subscriptions"));
        }

        @Test
        @DisplayName("Should throw when category not found")
        void shouldThrowWhenNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> categoryService.delete(99L));
        }
    }

    @Nested
    @DisplayName("merge")
    class MergeTests {

        @Test
        @DisplayName("Should merge source into target and delete source")
        void shouldMergeCategories() {
            Category source = createCategory(1L, "Comida", "#FF0000");
            Category target = createCategory(2L, "Alimentação", "#00FF00");

            User testUser = User.createLocalUser("Test", "test@test.com", "pass123",
                    java.util.Set.of(Role.USER));
            ReflectionTestUtils.setField(testUser, "id", 1L);

            InvoiceItem item = createInvoiceItem(100L, source);
            MerchantCategoryRule rule = MerchantCategoryRule.of(testUser, "merchant_key", "Merchant", source);
            Budget budget = Budget.of(testUser, source, java.math.BigDecimal.TEN, null);
            Subscription sub = Subscription.manual(testUser, "Sub", "sub_key",
                    java.math.BigDecimal.ONE, BillingCycle.MONTHLY);
            ReflectionTestUtils.setField(sub, "category", source);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(source));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(target));
            when(invoiceItemRepository.findByCategory(source)).thenReturn(List.of(item));
            when(ruleRepository.findByCategory(source)).thenReturn(List.of(rule));
            when(budgetRepository.findByCategory(source)).thenReturn(List.of(budget));
            when(subscriptionRepository.findByCategory(source)).thenReturn(List.of(sub));
            when(invoiceItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(ruleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(budgetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(subscriptionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            Category result = categoryService.merge(1L, 2L);

            assertEquals("Alimentação", result.getName());
            verify(categoryRepository).delete(source);
            verify(invoiceItemRepository).saveAll(anyList());
            verify(ruleRepository).saveAll(anyList());
            verify(budgetRepository).saveAll(anyList());
            verify(subscriptionRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should merge even when source has no usages")
        void shouldMergeEmptySource() {
            Category source = createCategory(1L, "Empty", "#000");
            Category target = createCategory(2L, "Target", "#FFF");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(source));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(target));
            when(invoiceItemRepository.findByCategory(source)).thenReturn(List.of());
            when(ruleRepository.findByCategory(source)).thenReturn(List.of());
            when(budgetRepository.findByCategory(source)).thenReturn(List.of());
            when(subscriptionRepository.findByCategory(source)).thenReturn(List.of());
            when(invoiceItemRepository.saveAll(anyList())).thenReturn(List.of());
            when(ruleRepository.saveAll(anyList())).thenReturn(List.of());
            when(budgetRepository.saveAll(anyList())).thenReturn(List.of());
            when(subscriptionRepository.saveAll(anyList())).thenReturn(List.of());

            Category result = categoryService.merge(1L, 2L);

            assertEquals("Target", result.getName());
            verify(categoryRepository).delete(source);
        }

        @Test
        @DisplayName("Should throw when merging same category")
        void shouldThrowWhenMergingSameCategory() {
            assertThrows(IllegalArgumentException.class,
                    () -> categoryService.merge(1L, 1L));
        }

        @Test
        @DisplayName("Should throw when source not found")
        void shouldThrowWhenSourceNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> categoryService.merge(99L, 1L));
        }

        @Test
        @DisplayName("Should throw when target not found")
        void shouldThrowWhenTargetNotFound() {
            Category source = createCategory(1L, "Source", "#000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(source));
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> categoryService.merge(1L, 99L));
        }
    }

    @Nested
    @DisplayName("reorder")
    class ReorderTests {

        @Test
        @DisplayName("Should set displayOrder based on list position")
        void shouldReorderCategories() {
            Category cat1 = createCategory(1L, "A", "#000");
            Category cat2 = createCategory(2L, "B", "#111");
            Category cat3 = createCategory(3L, "C", "#222");

            when(categoryRepository.findById(3L)).thenReturn(Optional.of(cat3));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat1));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(cat2));
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            categoryService.reorder(List.of(3L, 1L, 2L));

            assertEquals(1, cat3.getDisplayOrder());
            assertEquals(2, cat1.getDisplayOrder());
            assertEquals(3, cat2.getDisplayOrder());
        }

        @Test
        @DisplayName("Should throw when category in list not found")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> categoryService.reorder(List.of(99L)));
        }
    }

    @Nested
    @DisplayName("getUsage")
    class GetUsageTests {

        @Test
        @DisplayName("Should return usage counts for category")
        void shouldReturnUsageCounts() {
            Category cat = createCategory(1L, "Food", "#FF0000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
            when(invoiceItemRepository.countByCategory(cat)).thenReturn(10L);
            when(ruleRepository.countByCategory(cat)).thenReturn(3L);
            when(budgetRepository.countByCategory(cat)).thenReturn(2L);
            when(subscriptionRepository.countByCategory(cat)).thenReturn(1L);

            CategoryUsageResponse usage = categoryService.getUsage(1L);

            assertEquals(1L, usage.categoryId());
            assertEquals("Food", usage.name());
            assertEquals(10L, usage.itemCount());
            assertEquals(3L, usage.ruleCount());
            assertEquals(2L, usage.budgetCount());
            assertEquals(1L, usage.subscriptionCount());
            assertEquals(16L, usage.totalUsage());
        }

        @Test
        @DisplayName("Should return zero counts for unused category")
        void shouldReturnZeroCounts() {
            Category cat = createCategory(1L, "Empty", "#000");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
            when(invoiceItemRepository.countByCategory(cat)).thenReturn(0L);
            when(ruleRepository.countByCategory(cat)).thenReturn(0L);
            when(budgetRepository.countByCategory(cat)).thenReturn(0L);
            when(subscriptionRepository.countByCategory(cat)).thenReturn(0L);

            CategoryUsageResponse usage = categoryService.getUsage(1L);

            assertEquals(0L, usage.totalUsage());
        }

        @Test
        @DisplayName("Should throw when category not found")
        void shouldThrowWhenNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> categoryService.getUsage(99L));
        }
    }
}
