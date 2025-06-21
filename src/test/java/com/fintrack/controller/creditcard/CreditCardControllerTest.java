package com.fintrack.controller.creditcard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.user.UserJpaRepository;
import org.springframework.context.annotation.Import;
import com.fintrack.config.TestSecurityConfig;

@WebMvcTest(CreditCardController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("CreditCardController Tests")
class CreditCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditCardJpaRepository creditCardRepository;

    @MockBean
    private BankJpaRepository bankRepository;

    @MockBean
    private UserJpaRepository userRepository;

    private User testUser;
    private Bank testBank;
    private CreditCard inactiveCreditCard;

    @BeforeEach
    void setUp() {
        testUser = User.of("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("NU", "Nubank");
        inactiveCreditCard = CreditCard.of("Inactive Card", "5678", new BigDecimal("3000.00"), testUser, testBank);
        inactiveCreditCard.deactivate();

        // Simple mock for userRepository
        when(userRepository.findByEmail(any(Email.class)))
            .thenAnswer(invocation -> {
                Email email = invocation.getArgument(0);
                if (email != null && email.getEmail().equalsIgnoreCase("john@example.com")) {
                    return Optional.of(testUser);
                }
                return Optional.empty();
            });
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should activate credit card successfully")
    void shouldActivateCreditCardSuccessfully() throws Exception {
        // Given
        Long creditCardId = 1L;

        // Set ID on the inactive credit card using reflection
        try {
            java.lang.reflect.Field idField = CreditCard.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(inactiveCreditCard, creditCardId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID on credit card", e);
        }

        when(creditCardRepository.findByIdAndOwner(creditCardId, testUser)).thenReturn(Optional.of(inactiveCreditCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(inactiveCreditCard);

        // When & Then
        mockMvc.perform(patch("/api/credit-cards/{id}/activate", creditCardId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit card activated successfully"))
                .andExpect(jsonPath("$.id").value(creditCardId));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should update credit card name, limit and bank, but not lastFourDigits")
    void shouldUpdateCreditCardSuccessfully() throws Exception {
        // Given
        Long creditCardId = 2L;
        Bank newBank = Bank.of("ITAU", "Itaú");
        CreditCard card = CreditCard.of("Old Name", "1234", new BigDecimal("5000.00"), testUser, testBank);
        // Set ID on the card using reflection
        java.lang.reflect.Field idField = CreditCard.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(card, creditCardId);

        when(creditCardRepository.findByIdAndOwner(creditCardId, testUser)).thenReturn(Optional.of(card));
        when(bankRepository.findById(99L)).thenReturn(Optional.of(newBank));
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> inv.getArgument(0));

        String updateJson = """
        {
          "name": "Updated Card",
          "lastFourDigits": "9999",
          "limit": 10000.00,
          "bankId": 99
        }
        """;

        // When & Then
        mockMvc.perform(put("/api/credit-cards/{id}", creditCardId)
                .contentType("application/json")
                .content(updateJson)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit card updated successfully"))
                .andExpect(jsonPath("$.id").value(creditCardId))
                .andExpect(jsonPath("$.name").value("Updated Card"))
                .andExpect(jsonPath("$.lastFourDigits").value("1234")) // lastFourDigits não muda
                .andExpect(jsonPath("$.limit").value(10000.00))
                .andExpect(jsonPath("$.bankName").value("Itaú"));
    }
}