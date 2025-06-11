package com.fintrack.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the Email value object.
 *
 * These tests verify the validation logic, immutability, and behavior
 * of the Email value object following Value Object patterns.
 */
@DisplayName("Email Value Object")
public class EmailTest {

    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String VALID_EMAIL_WITH_SUBDOMAIN = "user@subdomain.example.com";
    private static final String VALID_EMAIL_WITH_PLUS = "user+tag@example.com";
    private static final String VALID_EMAIL_WITH_DASH = "user-name@example.com";

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create email with valid format")
        void shouldCreateEmailWithValidFormat() {
            Email email = Email.of(VALID_EMAIL);

            assertThat(email).isNotNull();
            assertThat(email.getEmail()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("Should create email with subdomain")
        void shouldCreateEmailWithSubdomain() {
            Email email = Email.of(VALID_EMAIL_WITH_SUBDOMAIN);

            assertThat(email.getEmail()).isEqualTo(VALID_EMAIL_WITH_SUBDOMAIN);
        }

        @Test
        @DisplayName("Should create email with plus sign")
        void shouldCreateEmailWithPlusSign() {
            Email email = Email.of(VALID_EMAIL_WITH_PLUS);

            assertThat(email.getEmail()).isEqualTo(VALID_EMAIL_WITH_PLUS);
        }

        @Test
        @DisplayName("Should create email with dash")
        void shouldCreateEmailWithDash() {
            Email email = Email.of(VALID_EMAIL_WITH_DASH);

            assertThat(email.getEmail()).isEqualTo(VALID_EMAIL_WITH_DASH);
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            String mixedCaseEmail = "John.Doe@EXAMPLE.COM";

            Email email = Email.of(mixedCaseEmail);

            assertThat(email.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should trim whitespace from email")
        void shouldTrimWhitespaceFromEmail() {
            String emailWithWhitespace = "  john.doe@example.com  ";

            Email email = Email.of(emailWithWhitespace);

            assertThat(email.getEmail()).isEqualTo("john.doe@example.com");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should throw exception when email is blank")
        void shouldThrowExceptionWhenEmailIsBlank(String blankEmail) {
            assertThatThrownBy(() -> Email.of(blankEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Email cannot be null or blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "invalid-email",
            "missing@",
            "@missing-domain",
            "no-at-sign",
            "multiple@@signs.com",
            "space in@email.com",
            "email@with space.com",
            "email@domain",
            "email@.com",
            "@.com",
            "email@domain.",
            "email@domain..com"
        })
        @DisplayName("Should throw exception when email format is invalid")
        void shouldThrowExceptionWhenEmailFormatIsInvalid(String invalidEmail) {
            assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("Should throw exception when email is too short")
        void shouldThrowExceptionWhenEmailIsTooShort() {
            String shortEmail = "a@b";

            assertThatThrownBy(() -> Email.of(shortEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("Should throw exception when email does not contain @ symbol")
        void shouldThrowExceptionWhenEmailDoesNotContainAtSymbol() {
            String emailWithoutAt = "invalid.email.com";

            assertThatThrownBy(() -> Email.of(emailWithoutAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when emails have same value")
        void shouldBeEqualWhenEmailsHaveSameValue() {
            Email email1 = Email.of(VALID_EMAIL);
            Email email2 = Email.of(VALID_EMAIL);

            assertThat(email1).isEqualTo(email2);
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when emails have same value with different case")
        void shouldBeEqualWhenEmailsHaveSameValueWithDifferentCase() {
            Email email1 = Email.of("John.Doe@EXAMPLE.COM");
            Email email2 = Email.of("john.doe@example.com");

            assertThat(email1).isEqualTo(email2);
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when emails have different values")
        void shouldNotBeEqualWhenEmailsHaveDifferentValues() {
            Email email1 = Email.of("user1@example.com");
            Email email2 = Email.of("user2@example.com");

            assertThat(email1).isNotEqualTo(email2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            Email email = Email.of(VALID_EMAIL);

            assertThat(email).isEqualTo(email);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Email email = Email.of(VALID_EMAIL);

            assertThat(email).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Email email = Email.of(VALID_EMAIL);
            String differentType = "not an email";

            assertThat(email).isNotEqualTo(differentType);
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should return same value on multiple calls")
        void shouldReturnSameValueOnMultipleCalls() {
            Email email = Email.of(VALID_EMAIL);

            String value1 = email.getEmail();
            String value2 = email.getEmail();
            String value3 = email.getEmail();

            assertThat(value1).isEqualTo(value2).isEqualTo(value3);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return email value as string")
        void shouldReturnEmailValueAsString() {
            Email email = Email.of(VALID_EMAIL);

            String stringRepresentation = email.toString();

            assertThat(stringRepresentation).isEqualTo(VALID_EMAIL);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle email with numbers")
        void shouldHandleEmailWithNumbers() {
            String emailWithNumbers = "user123@example.com";

            Email email = Email.of(emailWithNumbers);

            assertThat(email.getEmail()).isEqualTo(emailWithNumbers);
        }

        @Test
        @DisplayName("Should handle email with underscores")
        void shouldHandleEmailWithUnderscores() {
            String emailWithUnderscores = "user_name@example.com";

            Email email = Email.of(emailWithUnderscores);

            assertThat(email.getEmail()).isEqualTo(emailWithUnderscores);
        }

        @Test
        @DisplayName("Should handle email with dots in local part")
        void shouldHandleEmailWithDotsInLocalPart() {
            String emailWithDots = "user.name@example.com";

            Email email = Email.of(emailWithDots);

            assertThat(email.getEmail()).isEqualTo(emailWithDots);
        }

        @Test
        @DisplayName("Should handle email with multiple dots in domain")
        void shouldHandleEmailWithMultipleDotsInDomain() {
            String emailWithMultipleDots = "user@sub.domain.example.com";

            Email email = Email.of(emailWithMultipleDots);

            assertThat(email.getEmail()).isEqualTo(emailWithMultipleDots);
        }
    }
}