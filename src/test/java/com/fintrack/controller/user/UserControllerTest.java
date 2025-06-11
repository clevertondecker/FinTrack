package com.fintrack.controller.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.user.UserService;
import com.fintrack.controller.user.dtos.RegisterRequest;
import com.fintrack.domain.user.Role;

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

            verify(userService).registerUser(
                "John Doe",
                "john@example.com",
                "password123",
                Set.of(Role.USER)
            );
        }

        @Test
        @DisplayName("Should return 400 when name is too short")
        void shouldReturn400WhenNameIsTooShort() throws Exception {
            RegisterRequest request = new RegisterRequest(
                "A",
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
                "");

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
        @DisplayName("Should create UserController with valid UserService")
        void shouldCreateUserControllerWithValidUserService() {
            UserController controller = new UserController(userService);

            assert controller != null;
        }

        @Test
        @DisplayName("Should throw exception when UserService is null")
        void shouldThrowExceptionWhenUserServiceIsNull() {
            try {
                new UserController(null);
                assert false : "Should have thrown exception";
            } catch (NullPointerException e) {
                assert e.getMessage().contains("userService cannot be null");
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
}