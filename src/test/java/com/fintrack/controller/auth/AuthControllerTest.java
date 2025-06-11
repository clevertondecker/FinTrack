package com.fintrack.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.controller.auth.dtos.LoginRequest;
import com.fintrack.infrastructure.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginTests {

        @Test
        @DisplayName("Should return JWT token when login is successful")
        void shouldReturnJwtTokenWhenLoginIsSuccessful() throws Exception {
            LoginRequest loginRequest =
              new LoginRequest("user@example.com", "password123");
            String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

            when(jwtUtil.generateToken("user@example.com")).thenReturn(expectedToken);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(expectedToken))
                    .andExpect(jsonPath("$.type").value("Bearer"));

            verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil).generateToken("user@example.com");
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            LoginRequest loginRequest =
              new LoginRequest("invalid-email", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailIsBlank() throws Exception {
            LoginRequest loginRequest = new LoginRequest("", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void shouldReturn400WhenPasswordIsBlank() throws Exception {
            LoginRequest loginRequest =
              new LoginRequest("user@example.com", "");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create AuthController with valid dependencies")
        void shouldCreateAuthControllerWithValidDependencies() {
            AuthController controller = new AuthController(authManager, jwtUtil);

            assert controller != null;
        }

        @Test
        @DisplayName("Should throw exception when AuthenticationManager is null")
        void shouldThrowExceptionWhenAuthenticationManagerIsNull() {
            try {
                new AuthController(null, jwtUtil);
                assert false : "Should have thrown exception";
            } catch (NullPointerException e) {
                assert e.getMessage().contains("authManager cannot be null");
            }
        }

        @Test
        @DisplayName("Should throw exception when JwtUtil is null")
        void shouldThrowExceptionWhenJwtUtilIsNull() {
            try {
                new AuthController(authManager, null);
                assert false : "Should have thrown exception";
            } catch (NullPointerException e) {
                assert e.getMessage().contains("jwtUtil cannot be null");
            }
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats() throws Exception {
            String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "123@example.org"
            };

            for (String email : validEmails) {
                LoginRequest loginRequest = new LoginRequest(email, "password123");
                String expectedToken = "token-" + email;

                when(jwtUtil.generateToken(email)).thenReturn(expectedToken);

                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").value(expectedToken));
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
                ""
            };

            for (String email : invalidEmails) {
                LoginRequest loginRequest = new LoginRequest(email, "password123");

                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isBadRequest());
            }
        }
    }
}