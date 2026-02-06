package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CategorizationSource;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.MerchantCategoryRule;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.creditcard.MerchantCategoryRuleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MerchantCategorizationService.
 * Tests the merchant categorization rule application and management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantCategorizationService Tests")
class MerchantCategorizationServiceTest {

    @Mock
    private MerchantCategoryRuleJpaRepository ruleRepository;

    private MerchantNormalizationService normalizationService;
    private MerchantCategorizationService categorizationService;

    private User testUser;
    private Category foodCategory;
    private Category transportCategory;
    private Bank testBank;
    private CreditCard testCard;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() throws Exception {
        normalizationService = new MerchantNormalizationService();
        categorizationService = new MerchantCategorizationService(ruleRepository, normalizationService);

        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        ReflectionTestUtils.setField(testUser, "id", 1L);

        foodCategory = Category.of("Food", "#FF0000");
        ReflectionTestUtils.setField(foodCategory, "id", 1L);

        transportCategory = Category.of("Transport", "#0000FF");
        ReflectionTestUtils.setField(transportCategory, "id", 2L);

        testBank = Bank.of("NU", "Nubank");
        testCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        testInvoice = Invoice.of(testCard, YearMonth.of(2024, 11), LocalDate.of(2024, 11, 10));
    }

    @Nested
    @DisplayName("applyRule Tests")
    class ApplyRuleTests {

        @Test
        @DisplayName("Should not apply rule when no rule exists")
        void shouldNotApplyWhenNoRuleExists() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.empty());

            // When
            MerchantCategorizationService.CategorizationResult result =
                categorizationService.applyRule(item, testUser);

            // Then
            assertThat(result.applied()).isFalse();
            assertThat(result.rule()).isNull();
            assertThat(item.getCategory()).isNull();
            assertThat(item.getMerchantKey()).isEqualTo("UBER");
        }

        @Test
        @DisplayName("Should auto-apply rule when autoApply is enabled")
        void shouldAutoApplyWhenEnabled() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            // Simulate 3 confirmations to enable auto-apply
            rule.recordConfirmation();
            rule.recordConfirmation();
            ReflectionTestUtils.setField(rule, "id", 1L);

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.of(rule));
            when(ruleRepository.save(any(MerchantCategoryRule.class))).thenReturn(rule);

            // When
            MerchantCategorizationService.CategorizationResult result =
                categorizationService.applyRule(item, testUser);

            // Then
            assertThat(result.applied()).isTrue();
            assertThat(result.source()).isEqualTo(CategorizationSource.AUTO_RULE);
            assertThat(result.rule()).isEqualTo(rule);
            assertThat(item.getCategory()).isEqualTo(transportCategory);
            assertThat(item.getCategorizationSource()).isEqualTo(CategorizationSource.AUTO_RULE);
            assertThat(item.getAppliedRule()).isEqualTo(rule);
        }

        @Test
        @DisplayName("Should auto-apply rule when rule exists (threshold = 1)")
        void shouldAutoApplyWhenRuleExists() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            // With threshold = 1, rule is auto-apply from creation
            ReflectionTestUtils.setField(rule, "id", 1L);

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.of(rule));

            // When
            MerchantCategorizationService.CategorizationResult result =
                categorizationService.applyRule(item, testUser);

            // Then
            assertThat(result.applied()).isTrue();
            assertThat(result.source()).isEqualTo(CategorizationSource.AUTO_RULE);
            assertThat(result.rule()).isEqualTo(rule);
            // Item category should be set
            assertThat(item.getCategory()).isEqualTo(transportCategory);
        }

        @Test
        @DisplayName("Should return not applied when description cannot be normalized")
        void shouldReturnNotAppliedForInvalidDescription() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "123456789", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            // When
            MerchantCategorizationService.CategorizationResult result =
                categorizationService.applyRule(item, testUser);

            // Then
            assertThat(result.applied()).isFalse();
            verify(ruleRepository, never()).findByUserAndMerchantKey(any(), any());
        }
    }

    @Nested
    @DisplayName("recordManualCategorization Tests")
    class RecordManualCategorizationTests {

        @Test
        @DisplayName("Should create new rule when none exists")
        void shouldCreateNewRuleWhenNoneExists() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.empty());
            when(ruleRepository.save(any(MerchantCategoryRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MerchantCategoryRule result = categorizationService.recordManualCategorization(
                item, transportCategory, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMerchantKey()).isEqualTo("UBER");
            assertThat(result.getCategory()).isEqualTo(transportCategory);
            assertThat(result.getTimesConfirmed()).isEqualTo(1);
            assertThat(result.isAutoApply()).isTrue(); // threshold = 1

            ArgumentCaptor<MerchantCategoryRule> captor =
                ArgumentCaptor.forClass(MerchantCategoryRule.class);
            verify(ruleRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should confirm existing rule with same category")
        void shouldConfirmExistingRuleWithSameCategory() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            MerchantCategoryRule existingRule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            ReflectionTestUtils.setField(existingRule, "id", 1L);

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.of(existingRule));
            when(ruleRepository.save(any(MerchantCategoryRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MerchantCategoryRule result = categorizationService.recordManualCategorization(
                item, transportCategory, testUser);

            // Then
            assertThat(result.getTimesConfirmed()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should enable autoApply after 3 confirmations")
        void shouldEnableAutoApplyAfterThreeConfirmations() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            MerchantCategoryRule existingRule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            existingRule.recordConfirmation(); // 2nd confirmation
            ReflectionTestUtils.setField(existingRule, "id", 1L);

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.of(existingRule));
            when(ruleRepository.save(any(MerchantCategoryRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MerchantCategoryRule result = categorizationService.recordManualCategorization(
                item, transportCategory, testUser);

            // Then - 3rd confirmation should enable auto-apply
            assertThat(result.getTimesConfirmed()).isEqualTo(3);
            assertThat(result.isAutoApply()).isTrue();
        }

        @Test
        @DisplayName("Should override existing rule with different category")
        void shouldOverrideExistingRuleWithDifferentCategory() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            MerchantCategoryRule existingRule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            existingRule.recordConfirmation();
            existingRule.recordConfirmation(); // Now at 3 confirmations, autoApply enabled
            ReflectionTestUtils.setField(existingRule, "id", 1L);

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.of(existingRule));
            when(ruleRepository.save(any(MerchantCategoryRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When - Override with different category (Food instead of Transport)
            MerchantCategoryRule result = categorizationService.recordManualCategorization(
                item, foodCategory, testUser);

            // Then
            assertThat(result.getCategory()).isEqualTo(foodCategory);
            assertThat(result.getTimesOverridden()).isEqualTo(1);
            assertThat(result.getTimesConfirmed()).isEqualTo(1); // Reset after override
            assertThat(result.isAutoApply()).isTrue(); // Re-enabled because threshold = 1
        }

        @Test
        @DisplayName("Should return null when description cannot be normalized")
        void shouldReturnNullForInvalidDescription() {
            // Given
            InvoiceItem item = InvoiceItem.of(
                testInvoice, "BR SP", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));

            // When
            MerchantCategoryRule result = categorizationService.recordManualCategorization(
                item, transportCategory, testUser);

            // Then
            assertThat(result).isNull();
            verify(ruleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("applyRulesToItems Tests")
    class ApplyRulesToItemsTests {

        @Test
        @DisplayName("Should apply rules to multiple items")
        void shouldApplyRulesToMultipleItems() {
            // Given
            InvoiceItem uberItem = InvoiceItem.of(
                testInvoice, "UBER *TRIP", new BigDecimal("50.00"),
                null, LocalDate.of(2024, 10, 15));
            InvoiceItem netflixItem = InvoiceItem.of(
                testInvoice, "NETFLIX.COM", new BigDecimal("30.00"),
                null, LocalDate.of(2024, 10, 16));

            MerchantCategoryRule uberRule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            uberRule.recordConfirmation();
            uberRule.recordConfirmation();
            ReflectionTestUtils.setField(uberRule, "id", 1L);

            when(ruleRepository.findByUserAndMerchantKey(testUser, "UBER"))
                .thenReturn(Optional.of(uberRule));
            when(ruleRepository.findByUserAndMerchantKey(testUser, "NETFLIX"))
                .thenReturn(Optional.empty());
            when(ruleRepository.save(any(MerchantCategoryRule.class))).thenReturn(uberRule);

            // When
            int categorized = categorizationService.applyRulesToItems(
                List.of(uberItem, netflixItem), testUser);

            // Then
            assertThat(categorized).isEqualTo(1);
            assertThat(uberItem.getCategory()).isEqualTo(transportCategory);
            assertThat(netflixItem.getCategory()).isNull();
        }

        @Test
        @DisplayName("Should skip already categorized items")
        void shouldSkipAlreadyCategorizedItems() {
            // Given
            InvoiceItem categorizedItem = InvoiceItem.of(
                testInvoice, "UBER *TRIP", new BigDecimal("50.00"),
                foodCategory, LocalDate.of(2024, 10, 15));

            // When
            int categorized = categorizationService.applyRulesToItems(
                List.of(categorizedItem), testUser);

            // Then
            assertThat(categorized).isEqualTo(0);
            assertThat(categorizedItem.getCategory()).isEqualTo(foodCategory);
            verify(ruleRepository, never()).findByUserAndMerchantKey(any(), any());
        }
    }
}
