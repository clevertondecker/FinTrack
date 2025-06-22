package com.fintrack.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.user.RegisterRequest;
import com.fintrack.dto.user.CurrentUserResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Register Endpoint Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully with valid data")
        void shouldRegisterUserSuccessfullyWithValidData() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "password123"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"));

            verify(userService).registerUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        }

        @Test
        @DisplayName("Should return 400 when name is too short")
        void shouldReturn400WhenNameIsTooShort() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "J",
                "john@example.com",
                "password123"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should return 400 when name is too long")
        void shouldReturn400WhenNameIsTooLong() throws Exception {
            String longName = "A".repeat(101);
            RegisterRequest request = new RegisterRequest(
                longName,
                "john@example.com",
                "password123"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "",
                "john@example.com",
                "password123"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "John Doe",
                "invalid-email",
                "password123"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailIsBlank() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "John Doe",
                "",
                "password123"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "12345"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void shouldReturn400WhenPasswordIsBlank() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "John Doe",
                "john@example.com",
                ""
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(userService);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create UserController with valid dependencies")
        void shouldCreateUserControllerWithValidDependencies() {
            UserController controller = new UserController(userService, userRepository);

            assert controller != null;
        }

        @Test
        @DisplayName("Should throw exception when UserService is null")
        void shouldThrowExceptionWhenUserServiceIsNull() {
            try {
                new UserController(null, userRepository);
                assert false : "Should have thrown exception";
            } catch (NullPointerException e) {
                assert e.getMessage().contains("userService cannot be null");
            }
        }

        @Test
        @DisplayName("Should throw exception when UserRepository is null")
        void shouldThrowExceptionWhenUserRepositoryIsNull() {
            try {
                new UserController(userService, null);
                assert false : "Should have thrown exception";
            } catch (NullPointerException e) {
                assert e.getMessage().contains("userRepository cannot be null");
            }
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should accept valid name formats")
        void shouldAcceptValidNameFormats() throws Exception {
            String[] validNames = {
                "John Doe",
                "Jo",
                "A".repeat(100),
                "José María",
                "O'Connor",
                "Jean-Pierre" };

            for (String name : validNames) {
                RegisterRequest request = new RegisterRequest(
                    name,
                    "user@example.com",
                    "password123");

                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("User registered successfully"));

                verify(userService).registerUser(name, "user@example.com", "password123", Set.of(Role.USER));
            }
        }

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats() throws Exception {
            String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "123@example.org",
                "user-name@example-domain.com"
            };

            for (String email : validEmails) {
                RegisterRequest request = new RegisterRequest(
                    "John Doe",
                    email,
                    "password123"
                );

                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("User registered successfully"));

                verify(userService).registerUser("John Doe", email, "password123", Set.of(Role.USER));
            }
        }

        @Test
        @DisplayName("Should accept valid password lengths")
        void shouldAcceptValidPasswordLengths() throws Exception {
            String[] validPasswords = {
                "123456",           // minimum length
                "password123",      // typical password
                "A".repeat(72),     // maximum BCrypt length
                "MySecureP@ssw0rd!",
                "123456789012345678901234567890123456789012345678901234567890123456789012"
            };

            for (String password : validPasswords) {
                RegisterRequest request = new RegisterRequest(
                    "John Doe",
                    "user@example.com",
                    password
                );

                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("User registered successfully"));

                verify(userService).registerUser(
                  "John Doe", "user@example.com", password, Set.of(Role.USER));
            }
        }

        @Test
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats() throws Exception {
            String[] invalidEmails = {
                "invalid-email",
                "@example.com",
                "user@",
                "user.example.com",
                "user@.com",
                "user@example.",
                "user name@example.com",
                "user@example..com"
            };

            for (String email : invalidEmails) {
                RegisterRequest request = new RegisterRequest(
                    "John Doe",
                    email,
                    "password123"
                );

                mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());

                verifyNoMoreInteractions(userService);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle whitespace in name")
        void shouldHandleWhitespaceInName() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "   John Doe   ",
                "john@example.com",
                "password123"
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"));

            verify(userService).registerUser("   John Doe   ", "john@example.com", "password123", Set.of(Role.USER));
        }

        @Test
        @DisplayName("Should handle whitespace in password")
        void shouldHandleWhitespaceInPassword() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "   password123   "
            );

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"));

            verify(userService).registerUser(
              "John Doe", "john@example.com", "   password123   ", Set.of(Role.USER));
        }
    }

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user successfully")
        void shouldGetCurrentUserSuccessfully() {
            // Given
            User testUser = User.of("John Doe", "john@example.com", "password123", Set.of(Role.USER));
            // Set ID and timestamps using reflection to avoid null values
            setUserId(testUser, 1L);
            setUserTimestamps(testUser);
            
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("john@example.com")
                .password("password123")
                .authorities("USER")
                .build();

            when(userRepository.findByEmail(Email.of("john@example.com"))).thenReturn(Optional.of(testUser));

            // When
            var response = userController.getCurrentUser(userDetails);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(testUser.getId(), body.id());
            assertEquals("John Doe", body.name());
            assertEquals("john@example.com", body.email());
            assertArrayEquals(new String[]{"USER"}, body.roles());
            assertNotNull(body.createdAt());
            assertNotNull(body.updatedAt());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            // Given
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("nonexistent@example.com")
                .password("password123")
                .authorities("USER")
                .build();

            when(userRepository.findByEmail(Email.of("nonexistent@example.com"))).thenReturn(Optional.empty());

            // When
            var response = userController.getCurrentUser(userDetails);

            // Then
            assertNotNull(response);
            assertEquals(404, response.getStatusCodeValue());
        }

        @Test
        @DisplayName("Should return 500 when repository throws exception")
        void shouldReturn500WhenRepositoryThrowsException() {
            // Given
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("john@example.com")
                .password("password123")
                .authorities("USER")
                .build();

            when(userRepository.findByEmail(Email.of("john@example.com")))
                .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                userController.getCurrentUser(userDetails);
            });
        }

        @Test
        @DisplayName("Should handle user with multiple roles")
        void shouldHandleUserWithMultipleRoles() {
            // Given
            User testUser = User.of("Admin User", "admin@example.com", "password123", Set.of(Role.USER, Role.ADMIN));
            setUserId(testUser, 2L);
            setUserTimestamps(testUser);
            
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("admin@example.com")
                .password("password123")
                .authorities("USER", "ADMIN")
                .build();

            when(userRepository.findByEmail(Email.of("admin@example.com"))).thenReturn(Optional.of(testUser));

            // When
            var response = userController.getCurrentUser(userDetails);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals(testUser.getId(), body.id());
            assertEquals("Admin User", body.name());
            assertEquals("admin@example.com", body.email());
            String[] roles = body.roles();
            assertEquals(2, roles.length);
            assertTrue(java.util.Arrays.asList(roles).contains("USER"));
            assertTrue(java.util.Arrays.asList(roles).contains("ADMIN"));
        }

        @Test
        @DisplayName("Should handle special characters in user name")
        void shouldHandleSpecialCharactersInUserName() {
            // Given
            User testUser = User.of("João Silva", "joao@example.com", "password123", Set.of(Role.USER));
            setUserId(testUser, 3L);
            setUserTimestamps(testUser);
            
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("joao@example.com")
                .password("password123")
                .authorities("USER")
                .build();

            when(userRepository.findByEmail(Email.of("joao@example.com"))).thenReturn(Optional.of(testUser));

            // When
            var response = userController.getCurrentUser(userDetails);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());
            var body = response.getBody();
            assertNotNull(body);
            assertEquals("João Silva", body.name());
            assertEquals("joao@example.com", body.email());
        }

        // Helper methods to set user fields for testing
        private void setUserId(User user, Long id) {
            try {
                java.lang.reflect.Field idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(user, id);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user ID for testing", e);
            }
        }

        private void setUserTimestamps(User user) {
            try {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.lang.reflect.Field createdAtField = User.class.getDeclaredField("createdAt");
                java.lang.reflect.Field updatedAtField = User.class.getDeclaredField("updatedAt");
                
                createdAtField.setAccessible(true);
                updatedAtField.setAccessible(true);
                
                createdAtField.set(user, now);
                updatedAtField.set(user, now);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set user timestamps for testing", e);
            }
        }
    }
}