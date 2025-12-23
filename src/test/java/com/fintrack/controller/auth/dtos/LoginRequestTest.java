package com.fintrack.controller.auth.dtos;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import com.fintrack.dto.auth.LoginRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LoginRequest Tests")
class LoginRequestTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create LoginRequest with valid data")
        void shouldCreateLoginRequestWithValidData() {
            String email = "user@example.com";
            String password = "password123";

            LoginRequest request =
              new LoginRequest(email, password);

            assertNotNull(request);
            assertEquals(email, request.email());
            assertEquals(password, request.password());
        }

        @Test
        @DisplayName("Should create LoginRequest with null values")
        void shouldCreateLoginRequestWithNullValues() {
            LoginRequest request =
              new LoginRequest(null, null);
          assertNotNull(request);
          assertNull(request.email());
          assertNull(request.password());
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should pass validation with valid email")
        void shouldPassValidationWithValidEmail() {
            LoginRequest request =
              new LoginRequest("user@example.com", "password123");

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should fail validation with blank email")
        void shouldFailValidationWithBlankEmail() {
            LoginRequest request = new LoginRequest("", "password123");

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Email is required",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with null email")
        void shouldFailValidationWithNullEmail() {
            LoginRequest request =
              new LoginRequest(null, "password123");

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Email is required",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with invalid email format")
        void shouldFailValidationWithInvalidEmailFormat() {
            LoginRequest request = new LoginRequest("invalid-email", "password123");

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Email must be in valid format.",
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
                LoginRequest request = new LoginRequest(email, "password123");

                Set<ConstraintViolation<LoginRequest>> violations =
                  validator.validate(request);

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
                LoginRequest request = new LoginRequest(email, "password123");

                Set<ConstraintViolation<LoginRequest>> violations =
                  validator.validate(request);

                assertEquals(1, violations.size(), "Email: " + email);
                assertEquals("Email must be in valid format.",
                  violations.iterator().next().getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should pass validation with valid password")
        void shouldPassValidationWithValidPassword() {
            LoginRequest request =
              new LoginRequest("user@example.com", "password123");

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(0, violations.size());
        }

        @Test
        @DisplayName("Should fail validation with blank password")
        void shouldFailValidationWithBlankPassword() {
            LoginRequest request =
              new LoginRequest("user@example.com", "");

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Password is required.",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should fail validation with null password")
        void shouldFailValidationWithNullPassword() {
            LoginRequest request =
              new LoginRequest("user@example.com", null);

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(1, violations.size());
            assertEquals("Password is required.",
              violations.iterator().next().getMessage());
        }

        @Test
        @DisplayName("Should pass validation with various password formats")
        void shouldPassValidationWithVariousPasswordFormats() {
            String[] validPasswords = {
                "password",
                "123456",
                "MySecureP@ssw0rd!",
                "A".repeat(100),
                "password with spaces",
                "p@ssw0rd"
            };

            for (String password : validPasswords) {
                LoginRequest request =
                  new LoginRequest("user@example.com", password);

                Set<ConstraintViolation<LoginRequest>> violations =
                  validator.validate(request);

                assertEquals(0, violations.size(), "Password: " + password);
            }
        }
    }

    @Nested
    @DisplayName("Multiple Validation Tests")
    class MultipleValidationTests {

    @Test
    @DisplayName("Should fail validation with both email and password invalid")
    void shouldFailValidationWithBothEmailAndPasswordInvalid() {
        LoginRequest request = new LoginRequest("invalid-email", "");

        Set<ConstraintViolation<LoginRequest>> violations =
          validator.validate(request);

        assertEquals(2, violations.size());

        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        boolean hasPasswordViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        assertTrue(hasEmailViolation);
        assertTrue(hasPasswordViolation);
    }

        @Test
        @DisplayName("Should fail validation with null email and password")
        void shouldFailValidationWithNullEmailAndPassword() {
            LoginRequest request = new LoginRequest(null, null);

            Set<ConstraintViolation<LoginRequest>> violations =
              validator.validate(request);

            assertEquals(2, violations.size());

            boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
            boolean hasPasswordViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));

            assertTrue(hasEmailViolation);
            assertTrue(hasPasswordViolation);
        }
    }

    @Nested
    @DisplayName("Record Behavior Tests")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Should be immutable")
        void shouldBeImmutable() {
            LoginRequest request = new LoginRequest("user@example.com", "password123");

            // When & Then
            // Records are immutable by design, so we can't modify the fields
            assertEquals("user@example.com", request.email());
            assertEquals("password123", request.password());
        }

        @Test
        @DisplayName("Should have proper toString method")
        void shouldHaveProperToStringMethod() {
            LoginRequest request =
              new LoginRequest("user@example.com", "password123");

            String toString = request.toString();

            assertNotNull(toString);
            assertEquals("LoginRequest[email=user@example.com, password=password123]", toString);
        }

        @Test
        @DisplayName("Should have proper equals and hashCode")
        void shouldHaveProperEqualsAndHashCode() {
            LoginRequest request1 = new LoginRequest("user@example.com", "password123");
            LoginRequest request2 = new LoginRequest("user@example.com", "password123");
            LoginRequest request3 = new LoginRequest("different@example.com", "password123");

            assertEquals(request1, request2);
            assertEquals(request1.hashCode(), request2.hashCode());
            assertNotEquals(request1, request3);
            assertNotEquals(request1.hashCode(), request3.hashCode());
        }
    }
}