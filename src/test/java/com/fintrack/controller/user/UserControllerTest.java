package com.fintrack.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.user.ChangePasswordRequest;
import com.fintrack.dto.user.ConnectUserRequest;
import com.fintrack.dto.user.RegisterRequest;

/**
 * Unit tests for UserController covering registration, current user,
 * Circle of Trust, search, and password change endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    private static User createTestUser(String name, String email, Long id) {
        User user = User.createLocalUser(name, email, "encoded", Set.of(Role.USER));
        ReflectionTestUtils.setField(user, "id", id);
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(user, "createdAt", now);
        ReflectionTestUtils.setField(user, "updatedAt", now);
        return user;
    }

    private static UserDetails buildUserDetails(String email) {
        return org.springframework.security.core.userdetails.User
            .withUsername(email)
            .password("p")
            .authorities("USER")
            .build();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should create controller with valid dependencies")
        void shouldCreateWithValidDependencies() {
            assertThat(new UserController(userService)).isNotNull();
        }

        @Test
        @DisplayName("Should reject null UserService")
        void shouldRejectNullUserService() {
            assertThatThrownBy(() -> new UserController(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userService cannot be null");
        }
    }

    @Nested
    @DisplayName("POST /api/users/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterSuccessfully() throws Exception {
            var request = new RegisterRequest("John Doe", "john@example.com", "password123");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

            verify(userService).registerUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        }

        @Test
        @DisplayName("Should reject short name")
        void shouldRejectShortName() throws Exception {
            var request = new RegisterRequest("J", "john@example.com", "password123");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should reject long name")
        void shouldRejectLongName() throws Exception {
            var request = new RegisterRequest("A".repeat(101), "john@example.com", "password123");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should reject blank name")
        void shouldRejectBlankName() throws Exception {
            var request = new RegisterRequest("", "john@example.com", "password123");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should reject invalid email")
        void shouldRejectInvalidEmail() throws Exception {
            var request = new RegisterRequest("John Doe", "invalid-email", "password123");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should reject blank email")
        void shouldRejectBlankEmail() throws Exception {
            var request = new RegisterRequest("John Doe", "", "password123");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should reject short password")
        void shouldRejectShortPassword() throws Exception {
            var request = new RegisterRequest("John Doe", "john@example.com", "12345");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should reject blank password")
        void shouldRejectBlankPassword() throws Exception {
            var request = new RegisterRequest("John Doe", "john@example.com", "");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should reject missing body")
        void shouldRejectMissingBody() throws Exception {
            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should accept valid name formats")
        void shouldAcceptValidNameFormats() throws Exception {
            String[] validNames = {"John Doe", "Jo", "A".repeat(100), "José María", "O'Connor", "Jean-Pierre"};

            for (String name : validNames) {
                var request = new RegisterRequest(name, "user@example.com", "password123");
                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

                verify(userService).registerUser(name, "user@example.com", "password123", Set.of(Role.USER));
            }
        }

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats() throws Exception {
            String[] validEmails = {
                "user@example.com", "user.name@example.com",
                "user+tag@example.co.uk", "123@example.org", "user-name@example-domain.com"
            };

            for (String email : validEmails) {
                var request = new RegisterRequest("John Doe", email, "password123");
                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

                verify(userService).registerUser("John Doe", email, "password123", Set.of(Role.USER));
            }
        }

        @Test
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats() throws Exception {
            String[] invalidEmails = {
                "invalid-email", "@example.com", "user@", "user.example.com",
                "user@.com", "user@example.", "user name@example.com", "user@example..com"
            };

            for (String email : invalidEmails) {
                var request = new RegisterRequest("John Doe", email, "password123");
                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

                verifyNoMoreInteractions(userService);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/users/current-user")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user with provider")
        void shouldReturnCurrentUserWithProvider() {
            User testUser = createTestUser("John Doe", "john@example.com", 1L);
            UserDetails userDetails = buildUserDetails("john@example.com");

            when(userService.findUserByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            var response = userController.getCurrentUser(userDetails);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            var body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.id()).isEqualTo(1L);
            assertThat(body.name()).isEqualTo("John Doe");
            assertThat(body.email()).isEqualTo("john@example.com");
            assertThat(body.roles()).containsExactly("USER");
            assertThat(body.provider()).isEqualTo("LOCAL");
            assertThat(body.createdAt()).isNotNull();
            assertThat(body.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenNotFound() {
            UserDetails userDetails = buildUserDetails("nobody@example.com");
            when(userService.findUserByEmail("nobody@example.com")).thenReturn(Optional.empty());

            var response = userController.getCurrentUser(userDetails);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            UserDetails userDetails = buildUserDetails("john@example.com");
            when(userService.findUserByEmail("john@example.com"))
                .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> userController.getCurrentUser(userDetails))
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should handle user with multiple roles")
        void shouldHandleMultipleRoles() {
            User testUser = User.createLocalUser("Admin", "admin@example.com", "p",
                Set.of(Role.USER, Role.ADMIN));
            ReflectionTestUtils.setField(testUser, "id", 2L);
            ReflectionTestUtils.setField(testUser, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(testUser, "updatedAt", LocalDateTime.now());

            UserDetails userDetails = buildUserDetails("admin@example.com");
            when(userService.findUserByEmail("admin@example.com")).thenReturn(Optional.of(testUser));

            var body = userController.getCurrentUser(userDetails).getBody();

            assertThat(body).isNotNull();
            assertThat(body.roles()).hasSize(2);
            assertThat(Arrays.asList(body.roles())).contains("USER", "ADMIN");
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            User testUser = createTestUser("João Silva", "joao@example.com", 3L);
            UserDetails userDetails = buildUserDetails("joao@example.com");
            when(userService.findUserByEmail("joao@example.com")).thenReturn(Optional.of(testUser));

            var body = userController.getCurrentUser(userDetails).getBody();

            assertThat(body).isNotNull();
            assertThat(body.name()).isEqualTo("João Silva");
        }
    }

    @Nested
    @DisplayName("Circle of Trust")
    class CircleOfTrustTests {

        private static final String ALICE_EMAIL = "alice@example.com";
        private static final String BOB_EMAIL = "bob@example.com";

        @Test
        @DisplayName("GET /api/users returns connected users plus current user")
        void shouldReturnConnectedUsersList() {
            User alice = createTestUser("Alice", ALICE_EMAIL, 1L);
            User bob = createTestUser("Bob", BOB_EMAIL, 2L);

            UserDetails userDetails = buildUserDetails(ALICE_EMAIL);
            when(userService.getCurrentUser(ALICE_EMAIL)).thenReturn(alice);
            when(userService.getConnectedUsers(alice)).thenReturn(List.of(bob));

            var response = userController.getConnectedUsers(userDetails);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            var body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.users()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(body.users().stream().anyMatch(u -> BOB_EMAIL.equals(u.email()))).isTrue();
        }

        @Test
        @DisplayName("POST /api/users/connect succeeds with valid email")
        void shouldConnectUsersSuccessfully() {
            User alice = createTestUser("Alice", ALICE_EMAIL, 1L);
            UserDetails userDetails = buildUserDetails(ALICE_EMAIL);
            when(userService.getCurrentUser(ALICE_EMAIL)).thenReturn(alice);

            var response = userController.connectUser(userDetails, new ConnectUserRequest(BOB_EMAIL));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("User connected successfully");
            verify(userService).connectUsers(alice, BOB_EMAIL);
        }

        @Test
        @DisplayName("ConnectUserRequest rejects blank email")
        void shouldRejectBlankEmail() {
            var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
            assertThat(validator.validate(new ConnectUserRequest(""))).isNotEmpty();
        }

        @Test
        @DisplayName("ConnectUserRequest rejects invalid email")
        void shouldRejectInvalidEmail() {
            var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
            assertThat(validator.validate(new ConnectUserRequest("not-an-email"))).isNotEmpty();
        }

        @Test
        @DisplayName("GET /api/users/search returns user when found")
        void shouldReturnUserWhenFound() throws Exception {
            User bob = createTestUser("Bob", BOB_EMAIL, 2L);
            when(userService.findUserByEmail(BOB_EMAIL)).thenReturn(Optional.of(bob));

            mockMvc.perform(get("/api/users/search").param("email", BOB_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(BOB_EMAIL))
                .andExpect(jsonPath("$.name").value("Bob"));
        }

        @Test
        @DisplayName("GET /api/users/search returns 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(userService.findUserByEmail("nobody@example.com")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/users/search").param("email", "nobody@example.com"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/users/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            User user = createTestUser("John", "john@example.com", 1L);
            UserDetails userDetails = buildUserDetails("john@example.com");
            when(userService.getCurrentUser("john@example.com")).thenReturn(user);

            var request = new ChangePasswordRequest("oldPass", "newPass123");
            var response = userController.changePassword(userDetails, request);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(userService).changePassword(eq(user), eq("oldPass"), eq("newPass123"));
        }

        @Test
        @DisplayName("Should propagate error for wrong current password")
        void shouldPropagateWrongPasswordError() {
            User user = createTestUser("John", "john@example.com", 1L);
            UserDetails userDetails = buildUserDetails("john@example.com");
            when(userService.getCurrentUser("john@example.com")).thenReturn(user);

            Mockito.doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword(eq(user), eq("wrongPass"), eq("newPass123"));

            var request = new ChangePasswordRequest("wrongPass", "newPass123");

            assertThatThrownBy(() -> userController.changePassword(userDetails, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
        }

        @Test
        @DisplayName("Should propagate error for OAuth2 user")
        void shouldPropagateOAuth2Error() {
            User user = createTestUser("John", "john@example.com", 1L);
            UserDetails userDetails = buildUserDetails("john@example.com");
            when(userService.getCurrentUser("john@example.com")).thenReturn(user);

            Mockito.doThrow(new IllegalStateException("Cannot change password for OAuth2 users"))
                .when(userService).changePassword(eq(user), eq("pass"), eq("newPass123"));

            var request = new ChangePasswordRequest("pass", "newPass123");

            assertThatThrownBy(() -> userController.changePassword(userDetails, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change password for OAuth2 users");
        }

        @Test
        @DisplayName("ChangePasswordRequest rejects blank current password")
        void shouldRejectBlankCurrentPassword() {
            var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
            assertThat(validator.validate(new ChangePasswordRequest("", "newPass123"))).isNotEmpty();
        }

        @Test
        @DisplayName("ChangePasswordRequest rejects short new password")
        void shouldRejectShortNewPassword() {
            var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
            assertThat(validator.validate(new ChangePasswordRequest("oldPass", "abc"))).isNotEmpty();
        }

        @Test
        @DisplayName("ChangePasswordRequest rejects blank new password")
        void shouldRejectBlankNewPassword() {
            var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
            assertThat(validator.validate(new ChangePasswordRequest("oldPass", ""))).isNotEmpty();
        }

        @Test
        @DisplayName("ChangePasswordRequest accepts valid request")
        void shouldAcceptValidRequest() {
            var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
            assertThat(validator.validate(new ChangePasswordRequest("current", "newPass123"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle whitespace in name")
        void shouldHandleWhitespaceInName() throws Exception {
            var request = new RegisterRequest("   John Doe   ", "john@example.com", "password123");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(userService).registerUser("   John Doe   ", "john@example.com", "password123", Set.of(Role.USER));
        }

        @Test
        @DisplayName("Should handle whitespace in password")
        void shouldHandleWhitespaceInPassword() throws Exception {
            var request = new RegisterRequest("John Doe", "john@example.com", "   password123   ");

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(userService).registerUser("John Doe", "john@example.com", "   password123   ", Set.of(Role.USER));
        }
    }
}
