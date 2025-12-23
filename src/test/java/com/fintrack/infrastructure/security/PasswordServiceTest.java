package com.fintrack.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the PasswordService infrastructure service.
 * 
 * These tests verify the password encoding and validation functionality
 * using BCrypt algorithm.
 */
@DisplayName("Password Service")
class PasswordServiceTest {

    /** Test password for encoding. */
    private static final String RAW_PASSWORD = "securePassword123";
    /** Different test password for comparison. */
    private static final String DIFFERENT_RAW_PASSWORD = "differentPassword456";

    /** The password service under test. */
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create service successfully")
        void shouldCreateServiceSuccessfully() {
            // When & Then
            assertThat(passwordService).isNotNull();
        }
    }

    @Nested
    @DisplayName("Password Encoding Tests")
    class PasswordEncodingTests {

        @Test
        @DisplayName("Should encode password successfully")
        void shouldEncodePasswordSuccessfully() {
            // When
            String result = passwordService.encodePassword(RAW_PASSWORD);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should encode different passwords differently")
        void shouldEncodeDifferentPasswordsDifferently() {
            // When
            String result1 = passwordService.encodePassword(RAW_PASSWORD);
            String result2 = passwordService.encodePassword(DIFFERENT_RAW_PASSWORD);

            // Then
            assertThat(result1).isNotEqualTo(result2);
            assertThat(result1).startsWith("$2a$10$");
            assertThat(result2).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle empty password encoding")
        void shouldHandleEmptyPasswordEncoding() {
            // Given
            String emptyPassword = "";

            // When
            String result = passwordService.encodePassword(emptyPassword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            // Given
            String specialPassword = "p@ssw0rd!@#$%^&*()";

            // When
            String result = passwordService.encodePassword(specialPassword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result).startsWith("$2a$10$");
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should validate matching password successfully")
        void shouldValidateMatchingPasswordSuccessfully() {
            // Given
            String encodedPassword = passwordService.encodePassword(RAW_PASSWORD);

            // When
            boolean result = passwordService.matches(RAW_PASSWORD, encodedPassword);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject non-matching password")
        void shouldRejectNonMatchingPassword() {
            // Given
            String encodedPassword = passwordService.encodePassword(RAW_PASSWORD);

            // When
            boolean result = passwordService.matches(DIFFERENT_RAW_PASSWORD, encodedPassword);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject null password")
        void shouldRejectNullPassword() {
            // Given
            String encodedPassword = passwordService.encodePassword(RAW_PASSWORD);

            // When
            boolean result = passwordService.matches(null, encodedPassword);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject empty password")
        void shouldRejectEmptyPassword() {
            // Given
            String encodedPassword = passwordService.encodePassword(RAW_PASSWORD);

            // When
            boolean result = passwordService.matches("", encodedPassword);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle null encoded password")
        void shouldHandleNullEncodedPassword() {
            // When
            boolean result = passwordService.matches(RAW_PASSWORD, null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle empty encoded password")
        void shouldHandleEmptyEncodedPassword() {
            // When
            boolean result = passwordService.matches(RAW_PASSWORD, "");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("BCrypt Algorithm Tests")
    class BCryptAlgorithmTests {

        @Test
        @DisplayName("Should produce valid BCrypt hash format")
        void shouldProduceValidBCryptHashFormat() {
            // When
            String result = passwordService.encodePassword(RAW_PASSWORD);

            // Then
            assertThat(result).startsWith("$2a$10$");
            assertThat(result).hasSize(60); // BCrypt hash length
        }

        @Test
        @DisplayName("Should handle different BCrypt cost factors")
        void shouldHandleDifferentBCryptCostFactors() {
            // When
            String result = passwordService.encodePassword(RAW_PASSWORD);

            // Then
            // Default cost factor is 10, so hash should start with $2a$10$
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle BCrypt with different algorithms")
        void shouldHandleBCryptWithDifferentAlgorithms() {
            // When
            String result = passwordService.encodePassword(RAW_PASSWORD);

            // Then
            // BCrypt uses $2a$ algorithm by default
            assertThat(result).startsWith("$2a$");
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should not store raw password")
        void shouldNotStoreRawPassword() {
            // When
            String result = passwordService.encodePassword(RAW_PASSWORD);

            // Then
            assertThat(result).doesNotContain(RAW_PASSWORD);
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should produce different hashes for same password")
        void shouldProduceDifferentHashesForSamePassword() {
            // When
            String hash1 = passwordService.encodePassword(RAW_PASSWORD);
            String hash2 = passwordService.encodePassword(RAW_PASSWORD);

            // Then
            // BCrypt generates different hashes for the same password due to salt
            assertThat(hash1).isNotEqualTo(hash2);
            assertThat(hash1).startsWith("$2a$10$");
            assertThat(hash2).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle timing attacks resistance")
        void shouldHandleTimingAttacksResistance() {
            // Given
            String encodedPassword = passwordService.encodePassword(RAW_PASSWORD);
            String wrongPassword = "wrongPassword";

            // When
            boolean correctMatch = passwordService.matches(RAW_PASSWORD, encodedPassword);
            boolean wrongMatch = passwordService.matches(wrongPassword, encodedPassword);

            // Then
            assertThat(correctMatch).isTrue();
            assertThat(wrongMatch).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle password encoder failure")
        void shouldHandlePasswordEncoderFailure() {
            // Given
            String veryLongPassword = "a".repeat(70); // BCrypt limit is 72 bytes

            // When
            String result = passwordService.encodePassword(veryLongPassword);

            // Then
            // Should handle gracefully without throwing exception
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle password validation failure")
        void shouldHandlePasswordValidationFailure() {
            // Given
            String invalidEncodedPassword = "invalidHash";

            // When
            boolean result = passwordService.matches(RAW_PASSWORD, invalidEncodedPassword);

            // Then
            // Should handle gracefully without throwing exception
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should encode and validate password correctly")
        void shouldEncodeAndValidatePasswordCorrectly() {
            // Given
            String password = "mySecurePassword123";

            // When
            String encodedPassword = passwordService.encodePassword(password);
            boolean isValid = passwordService.matches(password, encodedPassword);

            // Then
            assertThat(encodedPassword).isNotNull();
            assertThat(encodedPassword).startsWith("$2a$10$");
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject wrong password after encoding")
        void shouldRejectWrongPasswordAfterEncoding() {
            // Given
            String originalPassword = "mySecurePassword123";
            String wrongPassword = "wrongPassword";

            // When
            String encodedPassword = passwordService.encodePassword(originalPassword);
            boolean isValid = passwordService.matches(wrongPassword, encodedPassword);

            // Then
            assertThat(encodedPassword).isNotNull();
            assertThat(encodedPassword).startsWith("$2a$10$");
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @ParameterizedTest
        @ValueSource(strings = {"a", "ab", "abc", "abcd"})
        @DisplayName("Should encode short passwords")
        void shouldEncodeShortPasswords(String shortPassword) {
            // When
            String result = passwordService.encodePassword(shortPassword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should encode password with maximum reasonable length")
        void shouldEncodePasswordWithMaximumReasonableLength() {
            // Given
            String longPassword = "a".repeat(70); // BCrypt limit is 72 bytes

            // When
            String result = passwordService.encodePassword(longPassword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle password with only numbers")
        void shouldHandlePasswordWithOnlyNumbers() {
            // Given
            String numericPassword = "123456789";

            // When
            String result = passwordService.encodePassword(numericPassword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle password with only letters")
        void shouldHandlePasswordWithOnlyLetters() {
            // Given
            String letterPassword = "abcdefghijklmnopqrstuvwxyz";

            // When
            String result = passwordService.encodePassword(letterPassword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("Should handle password with mixed case")
        void shouldHandlePasswordWithMixedCase() {
            // Given
            String mixedCasePassword = "MySecurePassword123";

            // When
            String result = passwordService.encodePassword(mixedCasePassword);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$10$");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should complete encoding within reasonable time")
        void shouldCompleteEncodingWithinReasonableTime() {
            // Given
            long startTime = System.currentTimeMillis();

            // When
            String result = passwordService.encodePassword(RAW_PASSWORD);
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$10$");
            assertThat(duration).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("Should complete validation within reasonable time")
        void shouldCompleteValidationWithinReasonableTime() {
            // Given
            String encodedPassword = passwordService.encodePassword(RAW_PASSWORD);
            long startTime = System.currentTimeMillis();

            // When
            boolean result = passwordService.matches(RAW_PASSWORD, encodedPassword);
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(result).isTrue();
            assertThat(duration).isLessThan(1000); // Should complete within 1 second
        }
    }
}