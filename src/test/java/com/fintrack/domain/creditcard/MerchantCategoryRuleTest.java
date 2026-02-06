package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MerchantCategoryRule entity.
 * Tests the business logic for merchant categorization rules.
 */
@DisplayName("MerchantCategoryRule Tests")
class MerchantCategoryRuleTest {

    private User testUser;
    private Category foodCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        ReflectionTestUtils.setField(testUser, "id", 1L);

        foodCategory = Category.of("Food", "#FF0000");
        ReflectionTestUtils.setField(foodCategory, "id", 1L);

        transportCategory = Category.of("Transport", "#0000FF");
        ReflectionTestUtils.setField(transportCategory, "id", 2L);
    }

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create rule with valid parameters")
        void shouldCreateRuleWithValidParameters() {
            // When
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP SAO PAULO", transportCategory);

            // Then
            assertThat(rule).isNotNull();
            assertThat(rule.getUser()).isEqualTo(testUser);
            assertThat(rule.getMerchantKey()).isEqualTo("UBER");
            assertThat(rule.getOriginalDescription()).isEqualTo("UBER *TRIP SAO PAULO");
            assertThat(rule.getCategory()).isEqualTo(transportCategory);
            assertThat(rule.getTimesConfirmed()).isEqualTo(1);
            assertThat(rule.getTimesOverridden()).isEqualTo(0);
            assertThat(rule.getTimesApplied()).isEqualTo(0);
            assertThat(rule.isAutoApply()).isTrue(); // threshold = 1, so enabled on creation
            assertThat(rule.getCreatedAt()).isNotNull();
            assertThat(rule.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            assertThatThrownBy(() ->
                MerchantCategoryRule.of(null, "UBER", "UBER *TRIP", transportCategory))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("User must not be null");
        }

        @Test
        @DisplayName("Should throw exception when merchant key is blank")
        void shouldThrowExceptionWhenMerchantKeyIsBlank() {
            assertThatThrownBy(() ->
                MerchantCategoryRule.of(testUser, "  ", "UBER *TRIP", transportCategory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merchant key must not be blank");
        }

        @Test
        @DisplayName("Should throw exception when category is null")
        void shouldThrowExceptionWhenCategoryIsNull() {
            assertThatThrownBy(() ->
                MerchantCategoryRule.of(testUser, "UBER", "UBER *TRIP", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Category must not be null");
        }

        @Test
        @DisplayName("Should allow null original description")
        void shouldAllowNullOriginalDescription() {
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", null, transportCategory);

            assertThat(rule.getOriginalDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("Confirmation Tests")
    class ConfirmationTests {

        @Test
        @DisplayName("Should increment times confirmed")
        void shouldIncrementTimesConfirmed() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);

            // When
            rule.recordConfirmation();

            // Then
            assertThat(rule.getTimesConfirmed()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should enable auto-apply after 1 confirmation (on creation)")
        void shouldEnableAutoApplyAfterOneConfirmation() {
            // Given/When - rule is created with 1 confirmation
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);

            // Then - auto-apply should be enabled immediately
            assertThat(rule.getTimesConfirmed()).isEqualTo(1);
            assertThat(rule.isAutoApply()).isTrue();
        }

        @Test
        @DisplayName("Should keep auto-apply enabled after more confirmations")
        void shouldKeepAutoApplyEnabledAfterMoreConfirmations() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            assertThat(rule.isAutoApply()).isTrue(); // enabled on creation (1 confirmation)

            // When
            rule.recordConfirmation();
            rule.recordConfirmation();
            rule.recordConfirmation();

            // Then
            assertThat(rule.getTimesConfirmed()).isEqualTo(4);
            assertThat(rule.isAutoApply()).isTrue();
        }

        @Test
        @DisplayName("Should update updatedAt on confirmation")
        void shouldUpdateUpdatedAtOnConfirmation() throws InterruptedException {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            var initialUpdatedAt = rule.getUpdatedAt();

            // Small delay to ensure time difference
            Thread.sleep(10);

            // When
            rule.recordConfirmation();

            // Then
            assertThat(rule.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Override Tests")
    class OverrideTests {

        @Test
        @DisplayName("Should change category on override")
        void shouldChangeCategoryOnOverride() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);

            // When
            rule.recordOverride(foodCategory);

            // Then
            assertThat(rule.getCategory()).isEqualTo(foodCategory);
        }

        @Test
        @DisplayName("Should increment times overridden")
        void shouldIncrementTimesOverridden() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);

            // When
            rule.recordOverride(foodCategory);

            // Then
            assertThat(rule.getTimesOverridden()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reset times confirmed on override")
        void shouldResetTimesConfirmedOnOverride() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            rule.recordConfirmation(); // 2 confirmations
            assertThat(rule.getTimesConfirmed()).isEqualTo(2);

            // When
            rule.recordOverride(foodCategory);

            // Then
            assertThat(rule.getTimesConfirmed()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should keep auto-apply enabled on override (threshold = 1)")
        void shouldKeepAutoApplyEnabledOnOverride() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            assertThat(rule.isAutoApply()).isTrue();

            // When - override resets to 1 confirmation, which meets threshold
            rule.recordOverride(foodCategory);

            // Then - auto-apply remains enabled because 1 confirmation meets threshold
            assertThat(rule.isAutoApply()).isTrue();
            assertThat(rule.getCategory()).isEqualTo(foodCategory);
        }

        @Test
        @DisplayName("Should throw exception when new category is null")
        void shouldThrowExceptionWhenNewCategoryIsNull() {
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);

            assertThatThrownBy(() -> rule.recordOverride(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("New category must not be null");
        }

        @Test
        @DisplayName("Should re-enable auto-apply immediately after override (threshold = 1)")
        void shouldReEnableAutoApplyAfterOverride() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            assertThat(rule.isAutoApply()).isTrue(); // enabled on creation

            // When - override resets confirmations to 1, which meets threshold
            rule.recordOverride(foodCategory);

            // Then - auto-apply should be re-enabled immediately (1 confirmation meets threshold)
            assertThat(rule.getTimesConfirmed()).isEqualTo(1);
            assertThat(rule.isAutoApply()).isTrue();
            assertThat(rule.getCategory()).isEqualTo(foodCategory);
        }
    }

    @Nested
    @DisplayName("Application Tracking Tests")
    class ApplicationTrackingTests {

        @Test
        @DisplayName("Should increment times applied")
        void shouldIncrementTimesApplied() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);

            // When
            rule.recordApplication();
            rule.recordApplication();
            rule.recordApplication();

            // Then
            assertThat(rule.getTimesApplied()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Confidence Score Tests")
    class ConfidenceScoreTests {

        @Test
        @DisplayName("Should return 0 when no interactions")
        void shouldReturnZeroWhenNoInteractions() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            // Initial confirmation = 1, but let's set to 0 for testing edge case
            ReflectionTestUtils.setField(rule, "timesConfirmed", 0);
            ReflectionTestUtils.setField(rule, "timesOverridden", 0);

            // When
            double score = rule.getConfidenceScore();

            // Then
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 1.0 when only confirmations")
        void shouldReturnOneWhenOnlyConfirmations() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            rule.recordConfirmation();
            rule.recordConfirmation();
            // 3 confirmations, 0 overrides

            // When
            double score = rule.getConfidenceScore();

            // Then
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should calculate correct ratio with mixed interactions")
        void shouldCalculateCorrectRatioWithMixedInteractions() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            rule.recordConfirmation(); // 2 confirmations
            rule.recordOverride(foodCategory); // 1 override, 1 confirmation
            // Total: 1 confirmation + 1 override = 2 interactions, score = 1/2 = 0.5

            // When
            double score = rule.getConfidenceScore();

            // Then
            assertThat(score).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("shouldAutoApply Tests")
    class ShouldAutoApplyTests {

        @Test
        @DisplayName("Should return true when autoApply is enabled (on creation with threshold = 1)")
        void shouldReturnTrueWhenAutoApplyEnabled() {
            // Given - rule is created with autoApply = true (threshold = 1)
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);

            // When/Then
            assertThat(rule.shouldAutoApply()).isTrue();
        }

        @Test
        @DisplayName("Should return true when autoApply is enabled and more confirmations")
        void shouldReturnTrueWhenAutoApplyEnabledAndMoreConfirmations() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            rule.recordConfirmation();
            rule.recordConfirmation();

            // When/Then
            assertThat(rule.shouldAutoApply()).isTrue();
        }

        @Test
        @DisplayName("Should return false when overrides exceed confirmations")
        void shouldReturnFalseWhenOverridesExceedConfirmations() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            rule.recordConfirmation();
            rule.recordConfirmation(); // autoApply = true
            
            // Override multiple times
            rule.recordOverride(foodCategory); // confirmations reset to 1
            ReflectionTestUtils.setField(rule, "autoApply", true); // Force enable for test
            ReflectionTestUtils.setField(rule, "timesOverridden", 5);
            ReflectionTestUtils.setField(rule, "timesConfirmed", 2);

            // When/Then
            assertThat(rule.shouldAutoApply()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when same ID")
        void shouldBeEqualWhenSameId() {
            // Given
            MerchantCategoryRule rule1 = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            ReflectionTestUtils.setField(rule1, "id", 1L);

            MerchantCategoryRule rule2 = MerchantCategoryRule.of(
                testUser, "NETFLIX", "NETFLIX.COM", foodCategory);
            ReflectionTestUtils.setField(rule2, "id", 1L);

            // When/Then
            assertThat(rule1).isEqualTo(rule2);
            assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different ID")
        void shouldNotBeEqualWhenDifferentId() {
            // Given
            MerchantCategoryRule rule1 = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            ReflectionTestUtils.setField(rule1, "id", 1L);

            MerchantCategoryRule rule2 = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            ReflectionTestUtils.setField(rule2, "id", 2L);

            // When/Then
            assertThat(rule1).isNotEqualTo(rule2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should include relevant fields in toString")
        void shouldIncludeRelevantFieldsInToString() {
            // Given
            MerchantCategoryRule rule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            ReflectionTestUtils.setField(rule, "id", 1L);

            // When
            String result = rule.toString();

            // Then
            assertThat(result).contains("id=1");
            assertThat(result).contains("merchantKey='UBER'");
            assertThat(result).contains("Transport");
            assertThat(result).contains("timesConfirmed=1");
            assertThat(result).contains("autoApply=true");
        }
    }
}
