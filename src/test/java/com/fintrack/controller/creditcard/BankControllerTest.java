package com.fintrack.controller.creditcard;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fintrack.application.creditcard.BankService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Role;

@WebMvcTest(BankController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("BankController Tests")
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankService bankService;

    private Bank nubank;
    private Bank itau;
    private Bank santander;

  @BeforeEach
    void setUp() {
        nubank = Bank.of("NU", "Nubank");
        itau = Bank.of("ITAU", "Itaú Unibanco");
        santander = Bank.of("SAN", "Santander");
        User.createLocalUser("Test User", "test@example.com", "password123", Set.of(Role.USER));
  }

    @Nested
    @DisplayName("Create Bank Tests")
    class CreateBankTests {

        @Test
        @DisplayName("Should create bank successfully")
        void shouldCreateBankSuccessfully() throws Exception {
            // Given
            Bank savedBank = Bank.of("NU", "Nubank");
            when(bankService.create(eq("NU"), eq("Nubank"))).thenReturn(savedBank);

            String requestBody = "{\"code\": \"NU\", \"name\": \"Nubank\"}";

            // When & Then
            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Bank created successfully"))
                    .andExpect(jsonPath("$.code").value("NU"))
                    .andExpect(jsonPath("$.name").value("Nubank"));
        }

        @Test
        @DisplayName("Should return bad request when code is missing")
        void shouldReturnBadRequestWhenCodeIsMissing() throws Exception {
            String requestBody = "{\"name\": \"Nubank\"}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank code is required"));
        }

        @Test
        @DisplayName("Should return bad request when name is missing")
        void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
            String requestBody = "{\"code\": \"NU\"}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank name is required"));
        }

        @Test
        @DisplayName("Should return bad request when bank code already exists")
        void shouldReturnBadRequestWhenBankCodeAlreadyExists() throws Exception {
            // Given
            when(bankService.create(eq("NU"), eq("Nubank")))
                    .thenThrow(new IllegalArgumentException("Bank with this code already exists"));

            String requestBody = "{\"code\": \"NU\", \"name\": \"Nubank\"}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank with this code already exists"));
        }

        @Test
        @DisplayName("Should handle empty request body")
        void shouldHandleEmptyRequestBody() throws Exception {
            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(anyOf(
                        equalTo("Bank code is required"),
                        equalTo("Bank name is required")
                    )));
        }

        @Test
        @DisplayName("Should handle null values in request")
        void shouldHandleNullValuesInRequest() throws Exception {
            String requestBody = "{\"code\": null, \"name\": null}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(anyOf(
                        equalTo("Bank code is required"),
                        equalTo("Bank name is required")
                    )));
        }

        @Test
        @DisplayName("Should not allow special characters in bank name")
        void shouldNotAllowSpecialCharactersInBankName() throws Exception {
            String requestBody = "{\"code\": \"ITAU\", \"name\": \"Itaú Unibanco S.A.\"}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank name must contain only letters, numbers and spaces"));
        }
    }

    @Nested
    @DisplayName("Get All Banks Tests")
    class GetAllBanksTests {

        @Test
        @DisplayName("Should return all banks successfully")
        void shouldReturnAllBanksSuccessfully() throws Exception {
            // Given
            List<Bank> banks = Arrays.asList(nubank, itau, santander);
            when(bankService.findAll()).thenReturn(banks);

            mockMvc.perform(get("/api/banks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Banks retrieved successfully"))
                    .andExpect(jsonPath("$.count").value(3))
                    .andExpect(jsonPath("$.banks").isArray())
                    .andExpect(jsonPath("$.banks[0].code").value("NU"))
                    .andExpect(jsonPath("$.banks[0].name").value("Nubank"))
                    .andExpect(jsonPath("$.banks[1].code").value("ITAU"))
                    .andExpect(jsonPath("$.banks[1].name").value("Itaú Unibanco"))
                    .andExpect(jsonPath("$.banks[2].code").value("SAN"))
                    .andExpect(jsonPath("$.banks[2].name").value("Santander"));
        }

        @Test
        @DisplayName("Should return empty list when no banks exist")
        void shouldReturnEmptyListWhenNoBanksExist() throws Exception {
            // Given
            when(bankService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/banks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Banks retrieved successfully"))
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.banks").isArray())
                    .andExpect(jsonPath("$.banks").isEmpty());
        }

        @Test
        @DisplayName("Should handle single bank")
        void shouldHandleSingleBank() throws Exception {
            // Given
            List<Bank> banks = Collections.singletonList(nubank);
            when(bankService.findAll()).thenReturn(banks);

            mockMvc.perform(get("/api/banks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Banks retrieved successfully"))
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.banks").isArray())
                    .andExpect(jsonPath("$.banks[0].code").value("NU"))
                    .andExpect(jsonPath("$.banks[0].name").value("Nubank"));
        }
    }

    @Nested
    @DisplayName("Get Bank By ID Tests")
    class GetBankByIdTests {

        @Test
        @DisplayName("Should return bank by ID successfully")
        void shouldReturnBankByIdSuccessfully() throws Exception {
            // Given
            Long bankId = 1L;
            when(bankService.findById(bankId)).thenReturn(Optional.of(nubank));

            mockMvc.perform(get("/api/banks/{id}", bankId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Bank retrieved successfully"))
                    .andExpect(jsonPath("$.bank.code").value("NU"))
                    .andExpect(jsonPath("$.bank.name").value("Nubank"));
        }

        @Test
        @DisplayName("Should return 404 when bank not found")
        void shouldReturn404WhenBankNotFound() throws Exception {
            // Given
            Long bankId = 999L;
            when(bankService.findById(bankId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/banks/{id}", bankId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should handle invalid ID format")
        void shouldHandleInvalidIdFormat() throws Exception {
            mockMvc.perform(get("/api/banks/invalid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            String malformedJson = """
                {
                    "code": "NU",
                    "name": "Nubank"
                """; // Missing closing brace

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle wrong content type")
        void shouldHandleWrongContentType() throws Exception {
            String requestBody = "code=NU&name=Nubank";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .content(requestBody))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should handle repository exception")
        void shouldHandleRepositoryException() throws Exception {
            // Given
            when(bankService.create(eq("NU"), eq("Nubank"))).thenThrow(new RuntimeException("Database error"));

            String requestBody = """
                {
                    "code": "NU",
                    "name": "Nubank"
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long bank name")
        void shouldHandleVeryLongBankName() throws Exception {
            // Given
            String longName = "A".repeat(255);
            Bank savedBank = Bank.of("LONG", longName);
            when(bankService.create(eq("LONG"), eq(longName))).thenReturn(savedBank);

            String requestBody = "{\"code\": \"LONG\", \"name\": \"" + longName + "\"}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank name must be between 2 and 100 characters"));
        }

        @Test
        @DisplayName("Should handle special characters in code")
        void shouldHandleSpecialCharactersInCode() throws Exception {
            String requestBody = "{\"code\": \"NU@\", \"name\": \"Nubank\"}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank code must contain only uppercase letters and numbers"));
        }

        @Test
        @DisplayName("Should not allow emoji in bank name")
        void shouldNotAllowEmojiInBankName() throws Exception {
            String requestBody = "{\"code\": \"EMOJI\", \"name\": \"🏦 Bank\"}";

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank name must contain only letters, numbers and spaces"));
        }
    }
} 