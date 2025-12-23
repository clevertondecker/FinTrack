package com.fintrack.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the OAuth2UserInfo record.
 */
@DisplayName("OAuth2UserInfo Tests")
class OAuth2UserInfoTest {

    /** Valid email for testing. */
    private static final String VALID_EMAIL = "test@example.com";
    /** Valid name for testing. */
    private static final String VALID_NAME = "Test User";
    /** Blank name for testing. */
    private static final String BLANK_NAME = "   ";
    /** Empty name for testing. */
    private static final String EMPTY_NAME = "";
    /** Long email for testing. */
    private static final String LONG_EMAIL = "very.long.email.address.with.many.subdomains@example.com";
    /** Long name for testing. */
    private static final String LONG_NAME =
        "Very Long Name With Many Words And Special Characters Including Numbers 123";
    /** Special email with tag for testing. */
    private static final String SPECIAL_EMAIL = "test+tag@example.com";
    /** Special name with accents for testing. */
    private static final String SPECIAL_NAME = "José María O'Connor-Smith";

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create OAuth2UserInfo with valid data")
        void shouldCreateOAuth2UserInfoWithValidData() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);

            assertThat(userInfo.email()).isEqualTo(VALID_EMAIL);
            assertThat(userInfo.name()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("Should create OAuth2UserInfo with email as name when name is null")
        void shouldCreateOAuth2UserInfoWithEmailAsNameWhenNameIsNull() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, null);

            assertThat(userInfo.email()).isEqualTo(VALID_EMAIL);
            assertThat(userInfo.name()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("Should create OAuth2UserInfo with email as name when name is empty")
        void shouldCreateOAuth2UserInfoWithEmailAsNameWhenNameIsEmpty() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, EMPTY_NAME);

            assertThat(userInfo.email()).isEqualTo(VALID_EMAIL);
            assertThat(userInfo.name()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("Should create OAuth2UserInfo with email as name when name is blank")
        void shouldCreateOAuth2UserInfoWithEmailAsNameWhenNameIsBlank() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, BLANK_NAME);

            assertThat(userInfo.email()).isEqualTo(VALID_EMAIL);
            assertThat(userInfo.name()).isEqualTo(VALID_EMAIL);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> new OAuth2UserInfo(null, VALID_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when email is empty")
        void shouldThrowExceptionWhenEmailIsEmpty() {
            assertThatThrownBy(() -> new OAuth2UserInfo(EMPTY_NAME, VALID_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when email is blank")
        void shouldThrowExceptionWhenEmailIsBlank() {
            assertThatThrownBy(() -> new OAuth2UserInfo(BLANK_NAME, VALID_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            assertThat(userInfo).isEqualTo(userInfo);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            assertThat(userInfo).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            assertThat(userInfo).isNotEqualTo("Not an OAuth2UserInfo");
        }

        @Test
        @DisplayName("Should be equal when email and name are equal")
        void shouldBeEqualWhenEmailAndNameAreEqual() {
            OAuth2UserInfo userInfo1 = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            OAuth2UserInfo userInfo2 = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);

            assertThat(userInfo1).isEqualTo(userInfo2);
        }

        @Test
        @DisplayName("Should not be equal when email is different")
        void shouldNotBeEqualWhenEmailIsDifferent() {
            OAuth2UserInfo userInfo1 = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            OAuth2UserInfo userInfo2 = new OAuth2UserInfo("different@example.com", VALID_NAME);

            assertThat(userInfo1).isNotEqualTo(userInfo2);
        }

        @Test
        @DisplayName("Should not be equal when name is different")
        void shouldNotBeEqualWhenNameIsDifferent() {
            OAuth2UserInfo userInfo1 = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            OAuth2UserInfo userInfo2 = new OAuth2UserInfo(VALID_EMAIL, "Different Name");

            assertThat(userInfo1).isNotEqualTo(userInfo2);
        }
    }

    @Nested
    @DisplayName("HashCode Tests")
    class HashCodeTests {

        @Test
        @DisplayName("Should have same hash code for equal objects")
        void shouldHaveSameHashCodeForEqualObjects() {
            OAuth2UserInfo userInfo1 = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            OAuth2UserInfo userInfo2 = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);

            assertThat(userInfo1.hashCode()).isEqualTo(userInfo2.hashCode());
        }

        @Test
        @DisplayName("Should have different hash codes for different objects")
        void shouldHaveDifferentHashCodesForDifferentObjects() {
            OAuth2UserInfo userInfo1 = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            OAuth2UserInfo userInfo2 = new OAuth2UserInfo("different@example.com", VALID_NAME);

            assertThat(userInfo1.hashCode()).isNotEqualTo(userInfo2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should include email and name in toString")
        void shouldIncludeEmailAndNameInToString() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, VALID_NAME);
            String toString = userInfo.toString();

            assertThat(toString)
                .contains(VALID_EMAIL)
                .contains(VALID_NAME);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle email with special characters")
        void shouldHandleEmailWithSpecialCharacters() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(SPECIAL_EMAIL, VALID_NAME);

            assertThat(userInfo.email()).isEqualTo(SPECIAL_EMAIL);
            assertThat(userInfo.name()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("Should handle name with special characters")
        void shouldHandleNameWithSpecialCharacters() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, SPECIAL_NAME);

            assertThat(userInfo.email()).isEqualTo(VALID_EMAIL);
            assertThat(userInfo.name()).isEqualTo(SPECIAL_NAME);
        }

        @Test
        @DisplayName("Should handle very long email")
        void shouldHandleVeryLongEmail() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(LONG_EMAIL, VALID_NAME);

            assertThat(userInfo.email()).isEqualTo(LONG_EMAIL);
            assertThat(userInfo.name()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("Should handle very long name")
        void shouldHandleVeryLongName() {
            OAuth2UserInfo userInfo = new OAuth2UserInfo(VALID_EMAIL, LONG_NAME);

            assertThat(userInfo.email()).isEqualTo(VALID_EMAIL);
            assertThat(userInfo.name()).isEqualTo(LONG_NAME);
        }
    }
} 