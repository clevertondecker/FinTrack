package com.fintrack.controller.creditcard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;

@WebMvcTest(BankController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("BankController Tests")
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankJpaRepository bankRepository;

    private Bank nubank;
    private Bank itau;
    private Bank santander;

    @BeforeEach
    void setUp() {
        nubank = Bank.of("NU", "Nubank");
        itau = Bank.of("ITAU", "Itaú Unibanco");
        santander = Bank.of("SAN", "Santander");
    }

    @Nested
    @DisplayName("Create Bank Tests")
    class CreateBankTests {

        @Test
        @DisplayName("Should create bank successfully")
        void shouldCreateBankSuccessfully() throws Exception {
            // Given
            Bank savedBank = Bank.of("NU", "Nubank");
            when(bankRepository.existsByCode("NU")).thenReturn(false);
            when(bankRepository.save(any(Bank.class))).thenReturn(savedBank);

            String requestBody = """
                {
                    "code": "NU",
                    "name": "Nubank"
                }
                """;

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
            String requestBody = """
                {
                    "name": "Nubank"
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Code and name are required"));
        }

        @Test
        @DisplayName("Should return bad request when name is missing")
        void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
            String requestBody = """
                {
                    "code": "NU"
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Code and name are required"));
        }

        @Test
        @DisplayName("Should return bad request when bank code already exists")
        void shouldReturnBadRequestWhenBankCodeAlreadyExists() throws Exception {
            // Given
            when(bankRepository.existsByCode("NU")).thenReturn(true);

            String requestBody = """
                {
                    "code": "NU",
                    "name": "Nubank"
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bank with this code already exists"));
        }

        @Test
        @DisplayName("Should handle special characters in bank name")
        void shouldHandleSpecialCharactersInBankName() throws Exception {
            // Given
            Bank savedBank = Bank.of("ITAU", "Itaú Unibanco S.A.");
            when(bankRepository.existsByCode("ITAU")).thenReturn(false);
            when(bankRepository.save(any(Bank.class))).thenReturn(savedBank);

            String requestBody = """
                {
                    "code": "ITAU",
                    "name": "Itaú Unibanco S.A."
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Bank created successfully"))
                    .andExpect(jsonPath("$.code").value("ITAU"))
                    .andExpect(jsonPath("$.name").value("Itaú Unibanco S.A."));
        }

        @Test
        @DisplayName("Should handle empty request body")
        void shouldHandleEmptyRequestBody() throws Exception {
            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Code and name are required"));
        }

        @Test
        @DisplayName("Should handle null values in request")
        void shouldHandleNullValuesInRequest() throws Exception {
            String requestBody = """
                {
                    "code": null,
                    "name": null
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Code and name are required"));
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
            when(bankRepository.findAll()).thenReturn(banks);

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
            when(bankRepository.findAll()).thenReturn(Arrays.asList());

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
            List<Bank> banks = Arrays.asList(nubank);
            when(bankRepository.findAll()).thenReturn(banks);

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
            when(bankRepository.findById(bankId)).thenReturn(Optional.of(nubank));

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
            when(bankRepository.findById(bankId)).thenReturn(Optional.empty());

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
            when(bankRepository.existsByCode("NU")).thenReturn(false);
            when(bankRepository.save(any(Bank.class))).thenThrow(new RuntimeException("Database error"));

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
            when(bankRepository.existsByCode("LONG")).thenReturn(false);
            when(bankRepository.save(any(Bank.class))).thenReturn(savedBank);

            String requestBody = String.format("""
                {
                    "code": "LONG",
                    "name": "%s"
                }
                """, longName);

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(longName));
        }

        @Test
        @DisplayName("Should handle special characters in code")
        void shouldHandleSpecialCharactersInCode() throws Exception {
            // Given
            Bank savedBank = Bank.of("ITAU-银行", "Itaú Bank");
            when(bankRepository.existsByCode("ITAU-银行")).thenReturn(false);
            when(bankRepository.save(any(Bank.class))).thenReturn(savedBank);

            String requestBody = """
                {
                    "code": "ITAU-银行",
                    "name": "Itaú Bank"
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ITAU-银行"));
        }

        @Test
        @DisplayName("Should handle emoji in bank name")
        void shouldHandleEmojiInBankName() throws Exception {
            // Given
            Bank savedBank = Bank.of("DIGI", "Digital Bank 🏦");
            when(bankRepository.existsByCode("DIGI")).thenReturn(false);
            when(bankRepository.save(any(Bank.class))).thenReturn(savedBank);

            String requestBody = """
                {
                    "code": "DIGI",
                    "name": "Digital Bank 🏦"
                }
                """;

            mockMvc.perform(post("/api/banks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Digital Bank 🏦"));
        }
    }
} 