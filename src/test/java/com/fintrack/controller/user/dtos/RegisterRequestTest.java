package com.fintrack.controller.user.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import com.fintrack.dto.user.RegisterRequest;

@DisplayName("RegisterRequest Tests")
class RegisterRequestTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create RegisterRequest with valid data")
        void shouldCreateRegisterRequestWithValidData() {
            String name = "John Doe";
            String email = "john@example.com";
            String password = "password123";

            RegisterRequest request = new RegisterRequest(name, email, password);

            assertNotNull(request);
            assertEquals(name, request.name());
            assertEquals(email, request.email());
            assertEquals(password, request.password());
        }

        @Test
        @DisplayName("Should create RegisterRequest with null values")
        void shouldCreateRegisterRequestWithNullValues() {
            RegisterRequest request = new RegisterRequest(null, null, null);

            assertNotNull(request);
            assertEquals(null, request.name());
            assertEquals(null, request.email());
            assertEquals(null, request.password());
        }
    }

    @Nested
    @DisplayName("Name Validation Tests")
    class NameValidationTests {

        @Test
        @DisplayName("Should pass validation with valid name")
        void shouldPassValidationWithValidName() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations =
              validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should fail validation with blank name")
        void shouldFailValidationWithBlankName() {
            RegisterRequest request =
              new RegisterRequest("", "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations
              = validator.validate(request);

            // NotBlank + Size violations
            assertEquals(2, violations.size());
        }

        @Test
        @DisplayName("Should fail validation with null name")
        void shouldFailValidationWithNullName() {
            RegisterRequest request =
              new RegisterRequest(null, "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Name is required.", violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with name too short")
        void shouldFailValidationWithNameTooShort() {
            RegisterRequest request =
              new RegisterRequest("A", "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Name must be between 2 and 100 characters",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with name too long")
        void shouldFailValidationWithNameTooLong() {
            String longName = "A".repeat(101);
            RegisterRequest request =
              new RegisterRequest(longName, "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Name must be between 2 and 100 characters",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should pass validation with minimum name length")
        void shouldPassValidationWithMinimumNameLength() {
            RegisterRequest request =
              new RegisterRequest("Jo", "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should pass validation with maximum name length")
        void shouldPassValidationWithMaximumNameLength() {
            String maxName = "A".repeat(100);
            RegisterRequest request =
              new RegisterRequest(maxName, "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations =
              validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should pass validation with various valid name formats")
        void shouldPassValidationWithVariousValidNameFormats() {
            String[] validNames = {
                "John Doe",
                "José María",
                "O'Connor",
                "Jean-Pierre",
                "Mary Jane",
                "Dr. Smith",
                "A".repeat(50)
            };

            for (String name : validNames) {
                RegisterRequest request = new RegisterRequest(name, "user@example.com", "password123");

                Set<ConstraintViolation<RegisterRequest>> violations =
                  validator.validate(request);

                assertEquals(0, violations.size(), "Name: " + name);
            }
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should pass validation with valid email")
        void shouldPassValidationWithValidEmail() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should fail validation with blank email")
        void shouldFailValidationWithBlankEmail() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Email is required.", violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with null email")
        void shouldFailValidationWithNullEmail() {
            RegisterRequest request =
              new RegisterRequest("John Doe", null, "password123");

            Set<ConstraintViolation<RegisterRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Email is required.", violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with invalid email format")
        void shouldFailValidationWithInvalidEmailFormat() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "invalid-email", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Email must be a valid format.",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should pass validation with various valid email formats")
        void shouldPassValidationWithVariousValidEmailFormats() {
            String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "123@example.org",
                "user-name@example-domain.com",
                "user@subdomain.example.com"
            };

            for (String email : validEmails) {
                RegisterRequest request =
                  new RegisterRequest("John Doe", email, "password123");

                Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

                assertEquals(0, violations.size(), "Email: " + email);
            }
        }

        @Test
        @DisplayName("Should fail validation with various invalid email formats")
        void shouldFailValidationWithVariousInvalidEmailFormats() {
            String[] invalidEmails = {
                "@example.com",
                "user@",
                "user.example.com",
                "user@.com",
                "user@example.",
                "user name@example.com",
                "user@example..com",
                "user@@example.com"
            };

            for (String email : invalidEmails) {
                RegisterRequest request =
                  new RegisterRequest("John Doe", email, "password123");

                Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

                assertEquals(1, violations.size(), "Email: " + email);
                assertEquals("Email must be a valid format.", violations.iterator().next().getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should pass validation with valid password")
        void shouldPassValidationWithValidPassword() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", "password123");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should fail validation with blank password")
        void shouldFailValidationWithBlankPassword() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", "");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // NotBlank + Size violations
            assertEquals(2, violations.size());
        }

        @Test
        @DisplayName("Should fail validation with null password")
        void shouldFailValidationWithNullPassword() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", null);

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Password is required.", violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with password too short")
        void shouldFailValidationWithPasswordTooShort() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", "12345");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Password must be at least 6 characters long.",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should pass validation with minimum password length")
        void shouldPassValidationWithMinimumPasswordLength() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", "123456");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should pass validation with various valid password formats")
        void shouldPassValidationWithVariousValidPasswordFormats() {
            String[] validPasswords = {
                "123456",
                "password",
                "MySecureP@ssw0rd!",
                "A".repeat(72), // maximum BCrypt length
                "password with spaces",
                "p@ssw0rd"
            };

            for (String password : validPasswords) {
                RegisterRequest request =
                  new RegisterRequest("John Doe", "user@example.com", password);

                Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

                assertEquals(0, violations.size(), "Password: " + password);
            }
        }
    }

    @Nested
    @DisplayName("Multiple Validation Tests")
    class MultipleValidationTests {

        @Test
        @DisplayName("Should fail validation with multiple invalid fields")
        void shouldFailValidationWithMultipleInvalidFields() {
            RegisterRequest request =
              new RegisterRequest("A", "invalid-email", "12345");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(3, violations.size());

            boolean hasNameViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
            boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
            boolean hasPasswordViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));

            assertEquals(true, hasNameViolation);
            assertEquals(true, hasEmailViolation);
            assertEquals(true, hasPasswordViolation);
        }

        @Test
        @DisplayName("Should fail validation with all null fields")
        void shouldFailValidationWithAllNullFields() {
            RegisterRequest request =
              new RegisterRequest(null, null, null);

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertEquals(3, violations.size());

            boolean hasNameViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
            boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
            boolean hasPasswordViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));

            assertEquals(true, hasNameViolation);
            assertEquals(true, hasEmailViolation);
            assertEquals(true, hasPasswordViolation);
        }
    }

    @Nested
    @DisplayName("Record Behavior Tests")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Should be immutable")
        void shouldBeImmutable() {
            RegisterRequest request =
              new RegisterRequest("John Doe", "john@example.com", "password123");

            // When & Then
            // Records are immutable by design, so we can't modify the fields
            assertEquals("John Doe", request.name());
            assertEquals("john@example.com", request.email());
            assertEquals("password123", request.password());
        }

        @Test
        @DisplayName("Should have proper toString method")
        void shouldHaveProperToStringMethod() {
            // Given
            RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");

            // When
            String toString = request.toString();

            // Then
            assertNotNull(toString);
            assertEquals("RegisterRequest[name=John Doe, email=john@example.com, password=password123]", toString);
        }

        @Test
        @DisplayName("Should have proper equals and hashCode")
        void shouldHaveProperEqualsAndHashCode() {
            // Given
            RegisterRequest request1 = new RegisterRequest("John Doe", "john@example.com", "password123");
            RegisterRequest request2 = new RegisterRequest("John Doe", "john@example.com", "password123");
            RegisterRequest request3 = new RegisterRequest("Jane Doe", "jane@example.com", "password123");

            // When & Then
            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
            assertEquals(false, request1.equals(request3));
            assertEquals(false, request1.hashCode() == request3.hashCode());
        }
    }
}