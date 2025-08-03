package com.fintrack.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the AuthProvider enum.
 */
@DisplayName("AuthProvider Tests")
class AuthProviderTest {

    private static final int EXPECTED_ENUM_COUNT = 2;

    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {

        @Test
        @DisplayName("Should have LOCAL and GOOGLE values")
        void shouldHaveLocalAndGoogleValues() {
            AuthProvider[] values = AuthProvider.values();

            assertThat(values)
                .hasSize(EXPECTED_ENUM_COUNT)
                .contains(AuthProvider.LOCAL, AuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("Should be able to get value by name")
        void shouldBeAbleToGetValueByName() {
            assertThat(AuthProvider.valueOf("LOCAL")).isEqualTo(AuthProvider.LOCAL);
            assertThat(AuthProvider.valueOf("GOOGLE")).isEqualTo(AuthProvider.GOOGLE);
        }
    }

    @Nested
    @DisplayName("Provider Type Tests")
    class ProviderTypeTests {

        @Test
        @DisplayName("Should correctly identify OAuth2 providers")
        void shouldCorrectlyIdentifyOAuth2Providers() {
            assertThat(AuthProvider.LOCAL.isOAuth2()).isFalse();
            assertThat(AuthProvider.GOOGLE.isOAuth2()).isTrue();
        }

        @Test
        @DisplayName("Should correctly identify password requirements")
        void shouldCorrectlyIdentifyPasswordRequirements() {
            assertThat(AuthProvider.LOCAL.requiresPassword()).isTrue();
            assertThat(AuthProvider.GOOGLE.requiresPassword()).isFalse();
        }
    }

    @Nested
    @DisplayName("Consistency Tests")
    class ConsistencyTests {

        @Test
        @DisplayName("Should have consistent behavior between isOAuth2 and requiresPassword")
        void shouldHaveConsistentBehaviorBetweenIsOAuth2AndRequiresPassword() {
            for (AuthProvider provider : AuthProvider.values()) {
                boolean isOAuth2 = provider.isOAuth2();
                boolean requiresPassword = provider.requiresPassword();
                
                // OAuth2 providers should not require password
                assertThat(isOAuth2).isNotEqualTo(requiresPassword);
            }
        }

        @Test
        @DisplayName("Should handle all enum values in provider type methods")
        void shouldHandleAllEnumValuesInProviderTypeMethods() {
            for (AuthProvider provider : AuthProvider.values()) {
                assertThat(provider.isOAuth2()).isInstanceOf(Boolean.class);
                assertThat(provider.requiresPassword()).isInstanceOf(Boolean.class);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle enum comparison correctly")
        void shouldHandleEnumComparisonCorrectly() {
            AuthProvider local = AuthProvider.LOCAL;
            AuthProvider google = AuthProvider.GOOGLE;

            assertThat(local).isNotEqualTo(google);
            assertThat(local).isEqualTo(AuthProvider.LOCAL);
            assertThat(google).isEqualTo(AuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("Should handle enum ordinal values")
        void shouldHandleEnumOrdinalValues() {
            assertThat(AuthProvider.LOCAL.ordinal()).isEqualTo(0);
            assertThat(AuthProvider.GOOGLE.ordinal()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle enum name values")
        void shouldHandleEnumNameValues() {
            assertThat(AuthProvider.LOCAL.name()).isEqualTo("LOCAL");
            assertThat(AuthProvider.GOOGLE.name()).isEqualTo("GOOGLE");
        }
    }
} 