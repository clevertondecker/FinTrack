package com.fintrack.domain.subscription;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SubscriptionTest {

    private final User testUser = mock(User.class);

    @Nested
    class FactoryMethodTests {

        @Test
        void shouldCreateManualSubscription() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix",
                    new BigDecimal("29.90"), BillingCycle.MONTHLY);

            assertThat(sub.getName()).isEqualTo("Netflix");
            assertThat(sub.getMerchantKey()).isEqualTo("netflix");
            assertThat(sub.getExpectedAmount()).isEqualByComparingTo("29.90");
            assertThat(sub.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
            assertThat(sub.getSource()).isEqualTo(SubscriptionSource.MANUAL);
            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(sub.isActive()).isTrue();
        }

        @Test
        void shouldCreateAutoDetectedSubscription() {
            LocalDate firstSeen = LocalDate.of(2025, 1, 15);
            Subscription sub = Subscription.autoDetected(
                    testUser, "Spotify", "spotify",
                    new BigDecimal("19.90"), firstSeen);

            assertThat(sub.getSource()).isEqualTo(SubscriptionSource.AUTO_DETECTED);
            assertThat(sub.getStartDate()).isEqualTo(firstSeen);
            assertThat(sub.getLastDetectedDate()).isEqualTo(firstSeen);
            assertThat(sub.getLastDetectedAmount()).isEqualByComparingTo("19.90");
        }

        @Test
        void shouldRejectNullOwner() {
            assertThatThrownBy(() -> Subscription.manual(
                    null, "Test", "test", BigDecimal.TEN, BillingCycle.MONTHLY))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> Subscription.manual(
                    testUser, " ", "test", BigDecimal.TEN, BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectZeroAmount() {
            assertThatThrownBy(() -> Subscription.manual(
                    testUser, "Test", "test", BigDecimal.ZERO, BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Subscription.manual(
                    testUser, "Test", "test", new BigDecimal("-10"), BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class LifecycleTests {

        @Test
        void shouldPauseSubscription() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            sub.pause();

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
            assertThat(sub.isActive()).isTrue();
        }

        @Test
        void shouldActivateSubscription() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            sub.pause();
            sub.activate();

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void shouldCancelSubscription() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            sub.cancel();

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(sub.isActive()).isFalse();
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void shouldUpdateDetails() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            sub.updateDetails("Netflix Premium", new BigDecimal("55.90"), BillingCycle.ANNUAL);

            assertThat(sub.getName()).isEqualTo("Netflix Premium");
            assertThat(sub.getExpectedAmount()).isEqualByComparingTo("55.90");
            assertThat(sub.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
        }

        @Test
        void shouldRejectBlankNameOnUpdate() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            assertThatThrownBy(() -> sub.updateDetails("", BigDecimal.TEN, BillingCycle.MONTHLY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldAssignCategory() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            Category cat = mock(Category.class);
            sub.assignCategory(cat);

            assertThat(sub.getCategory()).isEqualTo(cat);
        }

        @Test
        void shouldClearCategory() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            sub.assignCategory(mock(Category.class));
            sub.assignCategory(null);

            assertThat(sub.getCategory()).isNull();
        }
    }

    @Nested
    class DetectionTests {

        @Test
        void shouldRecordDetection() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", BigDecimal.TEN, BillingCycle.MONTHLY);
            LocalDate detectedDate = LocalDate.of(2025, 3, 5);
            BigDecimal detectedAmount = new BigDecimal("29.90");

            sub.recordDetection(detectedDate, detectedAmount);

            assertThat(sub.getLastDetectedDate()).isEqualTo(detectedDate);
            assertThat(sub.getLastDetectedAmount()).isEqualByComparingTo("29.90");
            assertThat(sub.getStartDate()).isEqualTo(detectedDate);
        }

        @Test
        void shouldNotOverwriteStartDate() {
            LocalDate firstSeen = LocalDate.of(2025, 1, 1);
            Subscription sub = Subscription.autoDetected(
                    testUser, "Test", "test", BigDecimal.TEN, firstSeen);

            sub.recordDetection(LocalDate.of(2025, 2, 1), BigDecimal.TEN);

            assertThat(sub.getStartDate()).isEqualTo(firstSeen);
        }

        @Test
        void shouldDetectPriceChange() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", new BigDecimal("29.90"), BillingCycle.MONTHLY);
            sub.recordDetection(LocalDate.now(), new BigDecimal("39.90"));

            assertThat(sub.hasPriceChanged()).isTrue();
        }

        @Test
        void shouldNotDetectPriceChangeWhenSame() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", new BigDecimal("29.90"), BillingCycle.MONTHLY);
            sub.recordDetection(LocalDate.now(), new BigDecimal("29.90"));

            assertThat(sub.hasPriceChanged()).isFalse();
        }

        @Test
        void shouldNotDetectPriceChangeWithoutDetection() {
            Subscription sub = Subscription.manual(
                    testUser, "Netflix", "netflix", new BigDecimal("29.90"), BillingCycle.MONTHLY);

            assertThat(sub.hasPriceChanged()).isFalse();
        }
    }

    @Nested
    class EqualityTests {

        @Test
        void shouldBeEqualWithSameId() {
            Subscription sub1 = Subscription.manual(
                    testUser, "Test1", "test1", BigDecimal.TEN, BillingCycle.MONTHLY);
            Subscription sub2 = Subscription.manual(
                    testUser, "Test2", "test2", BigDecimal.ONE, BillingCycle.ANNUAL);

            assertThat(sub1).isNotEqualTo(sub2);
        }

        @Test
        void shouldNotEqualNull() {
            Subscription sub = Subscription.manual(
                    testUser, "Test", "test", BigDecimal.TEN, BillingCycle.MONTHLY);
            assertThat(sub).isNotEqualTo(null);
        }
    }
}
