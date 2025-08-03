package com.fintrack.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OAuth2-related functionality in User.
 */
@DisplayName("User OAuth2 Tests")
class UserOAuth2Test {

    private static final String VALID_NAME = "Test User";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String ADMIN_NAME = "Admin User";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String MULTI_ROLE_NAME = "Multi Role User";
    private static final String MULTI_ROLE_EMAIL = "multi@example.com";
    private static final String LONG_NAME = "Very Long Name With Many Words And Special Characters Including Numbers 123";
    private static final String SPECIAL_EMAIL = "test+tag@example.com";
    private static final Set<Role> USER_ROLES = Set.of(Role.USER);
    private static final Set<Role> ADMIN_ROLES = Set.of(Role.ADMIN);
    private static final Set<Role> MULTI_ROLES = Set.of(Role.USER, Role.ADMIN);

    @Nested
    @DisplayName("createOAuth2User Tests")
    class CreateOAuth2UserTests {

        @Test
        @DisplayName("Should create OAuth2 user with valid data")
        void shouldCreateOAuth2UserWithValidData() {
            User user = createOAuth2User(VALID_NAME, VALID_EMAIL, USER_ROLES);

            assertOAuth2UserProperties(user, VALID_NAME, VALID_EMAIL, USER_ROLES);
        }

        @Test
        @DisplayName("Should create OAuth2 user with admin role")
        void shouldCreateOAuth2UserWithAdminRole() {
            User user = createOAuth2User(ADMIN_NAME, ADMIN_EMAIL, ADMIN_ROLES);

            assertOAuth2UserProperties(user, ADMIN_NAME, ADMIN_EMAIL, ADMIN_ROLES);
        }

        @Test
        @DisplayName("Should create OAuth2 user with multiple roles")
        void shouldCreateOAuth2UserWithMultipleRoles() {
            User user = createOAuth2User(MULTI_ROLE_NAME, MULTI_ROLE_EMAIL, MULTI_ROLES);

            assertOAuth2UserProperties(user, MULTI_ROLE_NAME, MULTI_ROLE_EMAIL, MULTI_ROLES);
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThatThrownBy(() -> User.createOAuth2User(null, VALID_EMAIL, USER_ROLES, AuthProvider.GOOGLE))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> User.createOAuth2User(VALID_NAME, null, USER_ROLES, AuthProvider.GOOGLE))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw exception when roles is null")
        void shouldThrowExceptionWhenRolesIsNull() {
            assertThatThrownBy(() -> User.createOAuth2User(VALID_NAME, VALID_EMAIL, null, AuthProvider.GOOGLE))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Provider Tests")
    class ProviderTests {

        @Test
        @DisplayName("Should set and get provider correctly")
        void shouldSetAndGetProviderCorrectly() {
            User user = User.createLocalUser(VALID_NAME, VALID_EMAIL, "password", USER_ROLES);

            user.setProvider(AuthProvider.GOOGLE);
            assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);

            user.setProvider(AuthProvider.LOCAL);
            assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
        }

        @Test
        @DisplayName("Should have LOCAL provider by default")
        void shouldHaveLocalProviderByDefault() {
            User user = User.createLocalUser(VALID_NAME, VALID_EMAIL, "password", USER_ROLES);

            assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
        }

        @Test
        @DisplayName("Should have GOOGLE provider for OAuth2 users")
        void shouldHaveGoogleProviderForOAuth2Users() {
            User user = createOAuth2User(VALID_NAME, VALID_EMAIL, USER_ROLES);

            assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        }
    }

    @Nested
    @DisplayName("Provider Type Tests")
    class ProviderTypeTests {

        @Test
        @DisplayName("Should identify OAuth2 user correctly")
        void shouldIdentifyOAuth2UserCorrectly() {
            User oauth2User = createOAuth2User(VALID_NAME, VALID_EMAIL, USER_ROLES);
            User localUser = User.createLocalUser(VALID_NAME, VALID_EMAIL, "password", USER_ROLES);

            assertThat(oauth2User.isOAuth2User()).isTrue();
            assertThat(localUser.isOAuth2User()).isFalse();
        }

        @Test
        @DisplayName("Should identify local user correctly")
        void shouldIdentifyLocalUserCorrectly() {
            User oauth2User = createOAuth2User(VALID_NAME, VALID_EMAIL, USER_ROLES);
            User localUser = User.createLocalUser(VALID_NAME, VALID_EMAIL, "password", USER_ROLES);

            assertThat(oauth2User.isLocalUser()).isFalse();
            assertThat(localUser.isLocalUser()).isTrue();
        }

        @Test
        @DisplayName("Should handle provider changes correctly")
        void shouldHandleProviderChangesCorrectly() {
            User user = User.createLocalUser(VALID_NAME, VALID_EMAIL, "password", USER_ROLES);

            assertThat(user.isLocalUser()).isTrue();
            assertThat(user.isOAuth2User()).isFalse();

            user.setProvider(AuthProvider.GOOGLE);

            assertThat(user.isLocalUser()).isFalse();
            assertThat(user.isOAuth2User()).isTrue();

            user.setProvider(AuthProvider.LOCAL);

            assertThat(user.isLocalUser()).isTrue();
            assertThat(user.isOAuth2User()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle OAuth2 user with empty name")
        void shouldHandleOAuth2UserWithEmptyName() {
            assertThatThrownBy(() -> User.createOAuth2User("", VALID_EMAIL, USER_ROLES, AuthProvider.GOOGLE))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle OAuth2 user with blank name")
        void shouldHandleOAuth2UserWithBlankName() {
            assertThatThrownBy(() -> User.createOAuth2User("   ", VALID_EMAIL, USER_ROLES, AuthProvider.GOOGLE))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle OAuth2 user with empty roles")
        void shouldHandleOAuth2UserWithEmptyRoles() {
            User user = User.createOAuth2User(VALID_NAME, VALID_EMAIL, Set.of(), AuthProvider.GOOGLE);

            assertThat(user.getRoles()).isEmpty();
            assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("Should handle very long name for OAuth2 user")
        void shouldHandleVeryLongNameForOAuth2User() {
            User user = User.createOAuth2User(LONG_NAME, VALID_EMAIL, USER_ROLES, AuthProvider.GOOGLE);

            assertThat(user.getName()).isEqualTo(LONG_NAME);
            assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("Should handle email with special characters for OAuth2 user")
        void shouldHandleEmailWithSpecialCharactersForOAuth2User() {
            User user = User.createOAuth2User(VALID_NAME, SPECIAL_EMAIL, USER_ROLES, AuthProvider.GOOGLE);

            assertThat(user.getEmail().toString()).isEqualTo(SPECIAL_EMAIL);
            assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        }
    }

    // Helper methods
    private User createOAuth2User(String name, String email, Set<Role> roles) {
        return User.createOAuth2User(name, email, roles, AuthProvider.GOOGLE);
    }

    private void assertOAuth2UserProperties(User user, String expectedName, String expectedEmail, Set<Role> expectedRoles) {
        assertThat(user.getName()).isEqualTo(expectedName);
        assertThat(user.getEmail().toString()).isEqualTo(expectedEmail);
        assertThat(user.getRoles()).isEqualTo(expectedRoles);
        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.isOAuth2User()).isTrue();
        assertThat(user.isLocalUser()).isFalse();
    }
} 