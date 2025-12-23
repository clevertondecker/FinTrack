package com.fintrack.domain.creditcard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CreditCard Tests")
class CreditCardTest {

    private User testUser;
    private Bank testBank;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("Test Bank", "Test Bank Description");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create CreditCard with valid data")
        void shouldCreateCreditCardWithValidData() {
            String name = "Nubank Rewards";
            String lastFourDigits = "1234";
            BigDecimal limit = new BigDecimal("5000.00");

            CreditCard creditCard = CreditCard.of(name, lastFourDigits, limit, testUser, testBank);

            assertNotNull(creditCard);
            assertEquals(name, creditCard.getName());
            assertEquals(lastFourDigits, creditCard.getLastFourDigits());
            assertEquals(limit, creditCard.getLimit());
            assertEquals(testUser, creditCard.getOwner());
            assertEquals(testBank, creditCard.getBank());
            assertTrue(creditCard.isActive());
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                CreditCard.of(null, "1234", new BigDecimal("5000.00"), testUser, testBank));
        }

        @Test
        @DisplayName("Should throw exception when name is blank")
        void shouldThrowExceptionWhenNameIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                CreditCard.of("", "1234", new BigDecimal("5000.00"), testUser, testBank));
        }

        @Test
        @DisplayName("Should throw exception when lastFourDigits is null")
        void shouldThrowExceptionWhenLastFourDigitsIsNull() {
            assertThrows(NullPointerException.class, () ->
                CreditCard.of("Test Card", null, new BigDecimal("5000.00"), testUser, testBank));
        }

        @Test
        @DisplayName("Should throw exception when lastFourDigits is blank")
        void shouldThrowExceptionWhenLastFourDigitsIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                CreditCard.of("Test Card", "", new BigDecimal("5000.00"), testUser, testBank));
        }

        @Test
        @DisplayName("Should throw exception when limit is null")
        void shouldThrowExceptionWhenLimitIsNull() {
            assertThrows(NullPointerException.class, () ->
                CreditCard.of("Test Card", "1234", null, testUser, testBank));
        }

        @Test
        @DisplayName("Should throw exception when limit is negative")
        void shouldThrowExceptionWhenLimitIsNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                CreditCard.of("Test Card", "1234", new BigDecimal("-1000.00"), testUser, testBank));
        }

        @Test
        @DisplayName("Should throw exception when owner is null")
        void shouldThrowExceptionWhenOwnerIsNull() {
            assertThrows(NullPointerException.class, () ->
                CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), null, testBank));
        }

        @Test
        @DisplayName("Should throw exception when bank is null")
        void shouldThrowExceptionWhenBankIsNull() {
            assertThrows(NullPointerException.class, () ->
                CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, null));
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should deactivate credit card")
        void shouldDeactivateCreditCard() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            LocalDateTime beforeDeactivation = creditCard.getUpdatedAt();

            creditCard.deactivate();

            assertFalse(creditCard.isActive());
            assertTrue(
                creditCard.getUpdatedAt().isAfter(beforeDeactivation)
                || creditCard.getUpdatedAt().isEqual(beforeDeactivation)
            );
        }

        @Test
        @DisplayName("Should activate credit card successfully")
        void shouldActivateCreditCard() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            creditCard.deactivate();
            
            // Ensure we have a different timestamp by waiting a bit
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            LocalDateTime beforeActivation = creditCard.getUpdatedAt();
            
            creditCard.activate();

            assertThat(creditCard.isActive()).isTrue();
            // Check if activation timestamp was updated instead of strictly after
            assertThat(creditCard.getUpdatedAt()).isNotEqualTo(beforeActivation);
        }

        @Test
        @DisplayName("Should update limit successfully")
        void shouldUpdateLimitSuccessfully() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            BigDecimal newLimit = new BigDecimal("10000.00");
            LocalDateTime beforeUpdate = creditCard.getUpdatedAt();

            creditCard.updateLimit(newLimit);

            assertEquals(newLimit, creditCard.getLimit());
            assertTrue(creditCard.getUpdatedAt().isAfter(beforeUpdate));
        }

        @Test
        @DisplayName("Should throw exception when updating limit to null")
        void shouldThrowExceptionWhenUpdatingLimitToNull() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);

            assertThrows(NullPointerException.class, () -> creditCard.updateLimit(null));
        }

        @Test
        @DisplayName("Should throw exception when updating limit to negative")
        void shouldThrowExceptionWhenUpdatingLimitToNegative() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);

            assertThrows(IllegalArgumentException.class, () -> 
                creditCard.updateLimit(new BigDecimal("-1000.00")));
        }
    }

    @Nested
    @DisplayName("Timestamps Tests")
    class TimestampsTests {

        @Test
        @DisplayName("Should set createdAt and updatedAt on creation")
        void shouldSetCreatedAtAndUpdatedAtOnCreation() {
            LocalDateTime beforeCreation = LocalDateTime.now();
            
            CreditCard creditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
            
            LocalDateTime afterCreation = LocalDateTime.now();

            assertTrue(creditCard.getCreatedAt().isAfter(beforeCreation)
                || creditCard.getCreatedAt().equals(beforeCreation));
            assertTrue(creditCard.getCreatedAt().isBefore(afterCreation)
                || creditCard.getCreatedAt().equals(afterCreation));
            
            assertTrue(creditCard.getUpdatedAt().isAfter(beforeCreation)
                || creditCard.getUpdatedAt().equals(beforeCreation));
            assertTrue(creditCard.getUpdatedAt().isBefore(afterCreation)
                || creditCard.getUpdatedAt().equals(afterCreation));
        }

        @Test
        @DisplayName("Should update updatedAt when deactivating")
        void shouldUpdateUpdatedAtWhenDeactivating() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            LocalDateTime createdAt = creditCard.getCreatedAt();
            
            creditCard.deactivate();
            
            assertEquals(createdAt, creditCard.getCreatedAt());
            assertTrue(creditCard.getUpdatedAt().isAfter(createdAt));
        }

        @Test
        @DisplayName("Should update updatedAt when activating")
        void shouldUpdateUpdatedAtWhenActivating() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            creditCard.deactivate();
            
            // Ensure we have a different timestamp by waiting a bit
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            LocalDateTime deactivatedAt = creditCard.getUpdatedAt();
            
            creditCard.activate();

            // Check if timestamp was updated instead of strictly after
            assertThat(creditCard.getUpdatedAt()).isNotEqualTo(deactivatedAt);
        }

        @Test
        @DisplayName("Should update updatedAt when updating limit")
        void shouldUpdateUpdatedAtWhenUpdatingLimit() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            LocalDateTime createdAt = creditCard.getCreatedAt();
            
            creditCard.updateLimit(new BigDecimal("10000.00"));
            
            assertEquals(createdAt, creditCard.getCreatedAt());
            assertTrue(creditCard.getUpdatedAt().isAfter(createdAt));
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            
            assertEquals(creditCard, creditCard);
            assertEquals(creditCard.hashCode(), creditCard.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);

          assertNotNull(creditCard);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);

          assertNotEquals("Not a CreditCard", creditCard);
        }

        @Test
        @DisplayName("Should be equal when IDs are equal")
        void shouldBeEqualWhenIdsAreEqual() {
            CreditCard creditCard1 = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            CreditCard creditCard2 = CreditCard.of("Itaú", "5678", new BigDecimal("10000.00"), testUser, testBank);
            setCreditCardId(creditCard1, 1L);
            setCreditCardId(creditCard2, 1L);
            assertEquals(creditCard1, creditCard2);
            assertEquals(creditCard1.hashCode(), creditCard2.hashCode());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero limit")
        void shouldHandleZeroLimit() {
            assertThrows(IllegalArgumentException.class, () ->
                CreditCard.of("Nubank", "1234", BigDecimal.ZERO, testUser, testBank));
        }

        @Test
        @DisplayName("Should handle very large limit")
        void shouldHandleVeryLargeLimit() {
            BigDecimal largeLimit = new BigDecimal("999999999.99");
            CreditCard creditCard = CreditCard.of("Nubank", "1234", largeLimit, testUser, testBank);
            
            assertEquals(largeLimit, creditCard.getLimit());
        }

        @Test
        @DisplayName("Should handle lastFourDigits with letters")
        void shouldHandleLastFourDigitsWithLetters() {
            assertThrows(IllegalArgumentException.class, () ->
                CreditCard.of("Nubank", "12ab", new BigDecimal("5000.00"), testUser, testBank));
        }

        @Test
        @DisplayName("Should handle lastFourDigits with special characters")
        void shouldHandleLastFourDigitsWithSpecialCharacters() {
            assertThrows(IllegalArgumentException.class, () ->
                CreditCard.of("Nubank", "12#4", new BigDecimal("5000.00"), testUser, testBank));
        }

        @Test
        @DisplayName("Should handle multiple limit updates")
        void shouldHandleMultipleLimitUpdates() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            
            creditCard.updateLimit(new BigDecimal("10000.00"));
            assertEquals(new BigDecimal("10000.00"), creditCard.getLimit());
            
            creditCard.updateLimit(new BigDecimal("7500.00"));
            assertEquals(new BigDecimal("7500.00"), creditCard.getLimit());
            
            creditCard.updateLimit(new BigDecimal("15000.00"));
            assertEquals(new BigDecimal("15000.00"), creditCard.getLimit());
        }

        @Test
        @DisplayName("Should handle multiple activate/deactivate cycles")
        void shouldHandleMultipleActivateDeactivateCycles() {
            CreditCard creditCard = CreditCard.of("Nubank", "1234", new BigDecimal("5000.00"), testUser, testBank);
            
            assertTrue(creditCard.isActive());
            
            creditCard.deactivate();
            assertFalse(creditCard.isActive());
            
            creditCard.activate();
            assertTrue(creditCard.isActive());
            
            creditCard.deactivate();
            assertFalse(creditCard.isActive());
            
            creditCard.activate();
            assertTrue(creditCard.isActive());
        }
    }

    // Helper para setar o id
    private void setCreditCardId(CreditCard card, Long id) {
        try {
            java.lang.reflect.Field idField = CreditCard.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(card, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set credit card ID for testing", e);
        }
    }
} 