package com.fintrack.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the User domain entity.
 *
 * These tests verify the business rules, validation logic, and behavior
 * of the User entity following Domain-Driven Design principles.
 */
@DisplayName("User Domain Entity")
public class UserTest {

    private static final String VALID_NAME = "John Doe";
    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String VALID_PASSWORD = "securePassword123";
    private static final Set<Role> VALID_ROLES = Set.of(Role.USER);

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create user with valid parameters")
        void shouldCreateUserWithValidParameters() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            assertThat(user).isNotNull();
            assertThat(user.getName()).isEqualTo(VALID_NAME);
            assertThat(user.getEmail().getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getRoles()).isEqualTo(VALID_ROLES);
        }

        @Test
        @DisplayName("Should create user with multiple roles")
        void shouldCreateUserWithMultipleRoles() {
            Set<Role> multipleRoles = Set.of(Role.USER, Role.ADMIN);

            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, multipleRoles);

            assertThat(user.getName()).isEqualTo(VALID_NAME);
            assertThat(user.getEmail().getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getRoles()).isEqualTo(multipleRoles);
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            String mixedCaseEmail = "John.Doe@EXAMPLE.COM";

            User user = User.of(VALID_NAME, mixedCaseEmail, VALID_PASSWORD, VALID_ROLES);

            assertThat(user.getEmail().getEmail()).isEqualTo("john.doe@example.com");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should throw exception when name is blank")
        void shouldThrowExceptionWhenNameIsBlank(String blankName) {
            assertThatThrownBy(() -> User.of(blankName, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name must not be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThatThrownBy(() -> User.of(null, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Name must not be null or blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should throw exception when password is blank")
        void shouldThrowExceptionWhenPasswordIsBlank(String blankPassword) {
            assertThatThrownBy(() -> User.of(VALID_NAME, VALID_EMAIL, blankPassword, VALID_ROLES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must not be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when password is null")
        void shouldThrowExceptionWhenPasswordIsNull() {
            assertThatThrownBy(() -> User.of(VALID_NAME, VALID_EMAIL, null, VALID_ROLES))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Password must not be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when roles is null")
        void shouldThrowExceptionWhenRolesIsNull() {
            assertThatThrownBy(() -> User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Roles must not be null");
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> User.of(VALID_NAME, null, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Email cannot be null or blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should throw exception when email is blank")
        void shouldThrowExceptionWhenEmailIsBlank(String blankEmail) {
            assertThatThrownBy(() -> User.of(VALID_NAME, blankEmail, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(IllegalArgumentException.class)
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
            assertThatThrownBy(() -> User.of(VALID_NAME, invalidEmail, VALID_PASSWORD, VALID_ROLES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct values from getters")
        void shouldReturnCorrectValuesFromGetters() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            assertThat(user.getName()).isEqualTo(VALID_NAME);
            assertThat(user.getEmail().getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getRoles()).isEqualTo(VALID_ROLES);
        }

        @Test
        @DisplayName("Should return non-null timestamps")
        void shouldReturnNonNullTimestamps() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getCreatedAt()).isInstanceOf(LocalDateTime.class);
            assertThat(user.getUpdatedAt()).isInstanceOf(LocalDateTime.class);
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when users have same ID")
        void shouldBeEqualWhenUsersHaveSameId() {
            User user1 = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);
            User user2 = User.of("Different Name", "different@email.com", "differentPassword", Set.of(Role.ADMIN));

            // Set same ID using reflection
            setUserId(user1, 1L);
            setUserId(user2, 1L);

            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            assertThat(user).isEqualTo(user);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            assertThat(user).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);
            String differentType = "not a user";

            assertThat(user).isNotEqualTo(differentType);
        }

        @Test
        @DisplayName("Should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            int hashCode1 = user.hashCode();
            int hashCode2 = user.hashCode();

            assertThat(hashCode1).isEqualTo(hashCode2);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should create user with current timestamps")
        void shouldCreateUserWithCurrentTimestamps() {
            LocalDateTime beforeCreation = LocalDateTime.now();

            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);
            LocalDateTime afterCreation = LocalDateTime.now();

            assertThat(user.getCreatedAt()).isBetween(beforeCreation, afterCreation);
            assertThat(user.getUpdatedAt()).isBetween(beforeCreation, afterCreation);
        }

        @Test
        @DisplayName("Should have same created and updated time initially")
        void shouldHaveSameCreatedAndUpdatedTimeInitially() {
            User user = User.of(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, VALID_ROLES);

            // Allow for small time differences due to execution time
            long timeDifference = Math.abs(Duration.between(user.getCreatedAt(), user.getUpdatedAt()).toMillis());
            assertThat(timeDifference).isLessThan(1000); // Less than 1 second
        }
    }

    /**
     * Helper method to set user ID for testing purposes.
     */
    private void setUserId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID for testing", e);
        }
    }
}