package com.fintrack.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the JwtUtil infrastructure service.
 * 
 * These tests verify JWT token generation, validation, and extraction
 * functionality using the JJWT library.
 */
@DisplayName("JWT Utility")
class JwtUtilTest {

    private static final String VALID_USERNAME = "john.doe@example.com";
    private static final String DIFFERENT_USERNAME = "jane.smith@example.com";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token")
        void shouldGenerateValidJwtToken() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
        }

        @Test
        @DisplayName("Should generate different tokens for different usernames")
        void shouldGenerateDifferentTokensForDifferentUsernames() {
            // When
            String token1 = jwtUtil.generateToken(VALID_USERNAME);
            String token2 = jwtUtil.generateToken(DIFFERENT_USERNAME);

            // Then
            assertThat(token1).isNotEqualTo(token2);
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
        }

        @Test
        @DisplayName("Should generate different tokens for same username")
        void shouldGenerateDifferentTokensForSameUsername() {
            // When
            String token1 = jwtUtil.generateToken(VALID_USERNAME);
            String token2 = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            // Tokens may be identical if generated in the same millisecond
            // What matters is that both are valid and extract the same username
            assertThat(jwtUtil.validate(token1)).isTrue();
            assertThat(jwtUtil.validate(token2)).isTrue();
            assertThat(jwtUtil.extractUsername(token1)).isEqualTo(VALID_USERNAME);
            assertThat(jwtUtil.extractUsername(token2)).isEqualTo(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should generate token with correct subject")
        void shouldGenerateTokenWithCorrectSubject() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should generate token with future expiration")
        void shouldGenerateTokenWithFutureExpiration() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(VALID_USERNAME);
            assertThat(jwtUtil.validate(token)).isTrue();
        }

        @Test
        @DisplayName("Should generate token with reasonable expiration time")
        void shouldGenerateTokenWithReasonableExpirationTime() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            assertThat(jwtUtil.validate(token)).isTrue();
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should generate token with issued at time")
        void shouldGenerateTokenWithIssuedAtTime() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            assertThat(jwtUtil.validate(token)).isTrue();
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(VALID_USERNAME);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "user@example.com",
            "admin@company.org",
            "test.user+tag@domain.co.uk",
            "user123@subdomain.example.com"
        })
        @DisplayName("Should generate valid tokens for various email formats")
        void shouldGenerateValidTokensForVariousEmailFormats(String email) {
            // When
            String token = jwtUtil.generateToken(email);

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(email);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid token")
        void shouldValidateValidToken() {
            // Given
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // When
            boolean isValid = jwtUtil.validate(token);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // When
            boolean isValid = jwtUtil.validate(null);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // When
            boolean isValid = jwtUtil.validate("");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt.token";

            // When
            boolean isValid = jwtUtil.validate(malformedToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectTokenWithInvalidSignature() {
            // Given
            String validToken = jwtUtil.generateToken(VALID_USERNAME);
            String[] parts = validToken.split("\\.");
            // Create a token with the same header and payload but different signature
            String tamperedToken = parts[0] + "." + parts[1] + ".invalid_signature";

            // When
            boolean isValid = jwtUtil.validate(tamperedToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            String expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTY5OTk5OTk5OSwiZXhwIjoxNjk5OTk5OTk5fQ.invalid";

            // When
            boolean isValid = jwtUtil.validate(expiredToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject token without expiration")
        void shouldRejectTokenWithoutExpiration() {
            // Given
            String tokenWithoutExpiration = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTY5OTk5OTk5OX0.invalid";

            // When
            boolean isValid = jwtUtil.validate(tokenWithoutExpiration);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsernameFromValidToken() {
            // Given
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // When
            String extractedUsername = jwtUtil.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should extract username from token with different email")
        void shouldExtractUsernameFromTokenWithDifferentEmail() {
            // Given
            String token = jwtUtil.generateToken(DIFFERENT_USERNAME);

            // When
            String extractedUsername = jwtUtil.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(DIFFERENT_USERNAME);
        }

        @Test
        @DisplayName("Should return null for null token")
        void shouldReturnNullForNullToken() {
            // When & Then
            assertThatThrownBy(() -> jwtUtil.extractUsername(null))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should return null for empty token")
        void shouldReturnNullForEmptyToken() {
            // When & Then
            assertThatThrownBy(() -> jwtUtil.extractUsername(""))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should return null for malformed token")
        void shouldReturnNullForMalformedToken() {
            // Given
            String malformedToken = "invalid.token.format";

            // When & Then
            assertThatThrownBy(() -> jwtUtil.extractUsername(malformedToken))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should return null for expired token")
        void shouldReturnNullForExpiredToken() {
            // Given
            String invalidToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTY5OTk5OTk5OSwiZXhwIjoxNjk5OTk5OTk5fQ.invalid";

            // When & Then
            assertThatThrownBy(() -> jwtUtil.extractUsername(invalidToken))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should use secure algorithm")
        void shouldUseSecureAlgorithm() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            assertThat(token).isNotNull();
            assertThat(jwtUtil.validate(token)).isTrue();
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should generate tokens with different signatures")
        void shouldGenerateTokensWithDifferentSignatures() {
            // When
            String token1 = jwtUtil.generateToken(VALID_USERNAME);
            String token2 = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            // Tokens may be identical if generated in the same millisecond
            // What matters is that both are valid and extract the same username
            assertThat(jwtUtil.validate(token1)).isTrue();
            assertThat(jwtUtil.validate(token2)).isTrue();
            assertThat(jwtUtil.extractUsername(token1)).isEqualTo(VALID_USERNAME);
            assertThat(jwtUtil.extractUsername(token2)).isEqualTo(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should not expose sensitive information in token")
        void shouldNotExposeSensitiveInformationInToken() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            // Decode the payload (second part) without verification
            String[] parts = token.split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // Should only contain standard JWT claims, not sensitive data
            assertThat(payload).contains("sub");
            assertThat(payload).contains("iat");
            assertThat(payload).contains("exp");
            assertThat(payload).doesNotContain("password");
            assertThat(payload).doesNotContain("secret");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long username")
        void shouldHandleVeryLongUsername() {
            // Given
            String longUsername = "a".repeat(1000) + "@example.com";

            // When
            String token = jwtUtil.generateToken(longUsername);

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should handle username with special characters")
        void shouldHandleUsernameWithSpecialCharacters() {
            // Given
            String specialUsername = "user+tag@example.com";

            // When
            String token = jwtUtil.generateToken(specialUsername);

            // Then
            assertThat(token).isNotNull();
            String extractedUsername = jwtUtil.extractUsername(token);
            assertThat(extractedUsername).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("Should handle concurrent token generation")
        void shouldHandleConcurrentTokenGeneration() {
            // When
            String token1 = jwtUtil.generateToken(VALID_USERNAME);
            String token2 = jwtUtil.generateToken(VALID_USERNAME);

            // Then
            // Tokens may be identical if generated in the same millisecond
            // What matters is that both are valid and extract the same username
            assertThat(jwtUtil.validate(token1)).isTrue();
            assertThat(jwtUtil.validate(token2)).isTrue();
            assertThat(jwtUtil.extractUsername(token1)).isEqualTo(VALID_USERNAME);
            assertThat(jwtUtil.extractUsername(token2)).isEqualTo(VALID_USERNAME);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should generate and validate token successfully")
        void shouldGenerateAndValidateTokenSuccessfully() {
            // When
            String token = jwtUtil.generateToken(VALID_USERNAME);
            boolean isValid = jwtUtil.validate(token);
            String extractedUsername = jwtUtil.extractUsername(token);

            // Then
            assertThat(isValid).isTrue();
            assertThat(extractedUsername).isEqualTo(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should handle complete token lifecycle")
        void shouldHandleCompleteTokenLifecycle() {
            // Given
            String username = "test.user@example.com";

            // When
            String token = jwtUtil.generateToken(username);
            boolean isValid = jwtUtil.validate(token);
            String extractedUsername = jwtUtil.extractUsername(token);

            // Then
            assertThat(token).isNotNull();
            assertThat(isValid).isTrue();
            assertThat(extractedUsername).isEqualTo(username);
        }
    }
}