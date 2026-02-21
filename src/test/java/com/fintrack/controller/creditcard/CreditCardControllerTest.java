package com.fintrack.controller.creditcard;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import com.fintrack.application.creditcard.CreditCardService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CardType;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.dto.creditcard.CreditCardResponse;

@WebMvcTest(CreditCardController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("CreditCardController Tests")
public class CreditCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditCardService creditCardService;

    private User testUser;
    private Bank testBank;
    private CreditCard testCreditCard;
    private CreditCard inactiveCreditCard;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("NU", "Nubank");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        inactiveCreditCard = CreditCard.of("Inactive Card", "5678", new BigDecimal("3000.00"), testUser, testBank);
        inactiveCreditCard.deactivate();

        // Set IDs using reflection for testing
        setCreditCardId(testCreditCard, 1L);
        setCreditCardId(inactiveCreditCard, 2L);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should activate credit card successfully")
    void shouldActivateCreditCardSuccessfully() throws Exception {
        // Given
        Long creditCardId = 2L;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.activateCreditCard(eq(creditCardId), eq(testUser))).thenReturn(inactiveCreditCard);

        // When & Then
        mockMvc.perform(patch("/api/credit-cards/{id}/activate", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit card activated successfully"))
                .andExpect(jsonPath("$.id").value(creditCardId));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when credit card not found")
    void shouldReturn404WhenCreditCardNotFound() throws Exception {
        // Given
        Long creditCardId = 999L;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.activateCreditCard(eq(creditCardId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Credit card not found"));

        // When & Then
        mockMvc.perform(patch("/api/credit-cards/{id}/activate", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit card not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when user not found")
    void shouldReturn400WhenUserNotFound() throws Exception {
        // Given
        Long creditCardId = 1L;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(patch("/api/credit-cards/{id}/activate", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get user credit cards successfully")
    void shouldGetUserCreditCardsSuccessfully() throws Exception {
        // Given
        List<CreditCard> creditCards = List.of(testCreditCard, inactiveCreditCard);
        Map<String, Object> creditCardDto1 = new HashMap<>();
        creditCardDto1.put("id", 1L);
        creditCardDto1.put("name", "Test Card");
        creditCardDto1.put("lastFourDigits", "1234");
        creditCardDto1.put("limit", new BigDecimal("5000.00"));
        creditCardDto1.put("active", true);
        creditCardDto1.put("bankName", "Nubank");

        Map<String, Object> creditCardDto2 = new HashMap<>();
        creditCardDto2.put("id", 2L);
        creditCardDto2.put("name", "Inactive Card");
        creditCardDto2.put("lastFourDigits", "5678");
        creditCardDto2.put("limit", new BigDecimal("3000.00"));
        creditCardDto2.put("active", false);
        creditCardDto2.put("bankName", "Nubank");

        CreditCardResponse creditCardResponse1 = new CreditCardResponse(
            1L, "Test Card", "1234", new BigDecimal("5000.00"), true, "Nubank",
            CardType.PHYSICAL, null, null, "John Doe",
            null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        CreditCardResponse creditCardResponse2 = new CreditCardResponse(
            2L, "Inactive Card", "5678", new BigDecimal("3000.00"), false, "Nubank",
            CardType.PHYSICAL, null, null, "John Doe",
            null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        List<CreditCardResponse> creditCardResponses = List.of(creditCardResponse1, creditCardResponse2);

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.getUserCreditCards(eq(testUser), eq(false))).thenReturn(creditCards);
        when(creditCardService.toCreditCardResponseList(eq(creditCards))).thenReturn(creditCardResponses);
        when(creditCardService.toGroupedCreditCardResponseList(eq(creditCards))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/credit-cards")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit cards retrieved successfully"))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.creditCards").isArray())
                .andExpect(jsonPath("$.creditCards.length()").value(2))
                .andExpect(jsonPath("$.creditCards[0].id").value(1))
                .andExpect(jsonPath("$.creditCards[0].name").value("Test Card"))
                .andExpect(jsonPath("$.creditCards[0].lastFourDigits").value("1234"))
                .andExpect(jsonPath("$.creditCards[0].active").value(true))
                .andExpect(jsonPath("$.creditCards[0].bankName").value("Nubank"))
                .andExpect(jsonPath("$.creditCards[1].id").value(2))
                .andExpect(jsonPath("$.creditCards[1].name").value("Inactive Card"))
                .andExpect(jsonPath("$.creditCards[1].lastFourDigits").value("5678"))
                .andExpect(jsonPath("$.creditCards[1].active").value(false))
                .andExpect(jsonPath("$.creditCards[1].bankName").value("Nubank"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get specific credit card successfully")
    void shouldGetSpecificCreditCardSuccessfully() throws Exception {
        // Given
        Long creditCardId = 1L;
        CreditCardResponse creditCardResponse = new CreditCardResponse(
            1L, "Test Card", "1234", new BigDecimal("5000.00"), true, "Nubank",
            CardType.PHYSICAL, null, null, "John Doe",
            null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.getCreditCard(eq(creditCardId), eq(testUser))).thenReturn(testCreditCard);
        when(creditCardService.toCreditCardResponse(eq(testCreditCard))).thenReturn(creditCardResponse);

        // When & Then
        mockMvc.perform(get("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit card retrieved successfully"))
                .andExpect(jsonPath("$.creditCard.id").value(1))
                .andExpect(jsonPath("$.creditCard.name").value("Test Card"))
                .andExpect(jsonPath("$.creditCard.lastFourDigits").value("1234"))
                .andExpect(jsonPath("$.creditCard.active").value(true))
                .andExpect(jsonPath("$.creditCard.bankName").value("Nubank"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when specific credit card not found")
    void shouldReturn404WhenSpecificCreditCardNotFound() throws Exception {
        // Given
        Long creditCardId = 999L;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.getCreditCard(eq(creditCardId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Credit card not found"));

        // When & Then
        mockMvc.perform(get("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit card not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should deactivate credit card successfully")
    void shouldDeactivateCreditCardSuccessfully() throws Exception {
        Long creditCardId = 1L;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.deactivateCreditCard(eq(creditCardId), eq(testUser))).thenReturn(testCreditCard);

        mockMvc.perform(delete("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit card deactivated successfully"))
                .andExpect(jsonPath("$.id").value(creditCardId));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when deactivating non-existent credit card")
    void shouldReturn404WhenDeactivatingNonExistentCreditCard() throws Exception {
        Long creditCardId = 999L;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.deactivateCreditCard(eq(creditCardId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Credit card not found"));

        mockMvc.perform(delete("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit card not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when user not found on deactivate")
    void shouldReturn400WhenUserNotFoundOnDeactivate() throws Exception {
        Long creditCardId = 1L;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should update credit card successfully")
    void shouldUpdateCreditCardSuccessfully() throws Exception {
        Long creditCardId = 1L;
        String requestBody = """
            {
                "name": "Updated Card",
                "lastFourDigits": "5678",
                "limit": 10000.00,
                "bankId": 1,
                "cardType": "PHYSICAL"
            }
            """;

        CreditCard updatedCreditCard = CreditCard.of("Updated Card", "5678",
            new BigDecimal("10000.00"), testUser, testBank);
        setCreditCardId(updatedCreditCard, creditCardId);

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.updateCreditCard(eq(creditCardId), any(), eq(testUser))).thenReturn(updatedCreditCard);

        mockMvc.perform(put("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit card updated successfully"))
                .andExpect(jsonPath("$.id").value(creditCardId))
                .andExpect(jsonPath("$.name").value("Updated Card"))
                .andExpect(jsonPath("$.lastFourDigits").value("5678"))
                .andExpect(jsonPath("$.limit").value(10000.00))
                .andExpect(jsonPath("$.bankName").value("Nubank"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when updating non-existent credit card")
    void shouldReturn404WhenUpdatingNonExistentCreditCard() throws Exception {
        Long creditCardId = 999L;
        String requestBody = """
            {
                "name": "Updated Card",
                "lastFourDigits": "5678",
                "limit": 10000.00,
                "bankId": 1,
                "cardType": "PHYSICAL"
            }
            """;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.updateCreditCard(eq(creditCardId), any(), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Credit card not found"));

        mockMvc.perform(put("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit card not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when user not found on update")
    void shouldReturn400WhenUserNotFoundOnUpdate() throws Exception {
        Long creditCardId = 1L;
        String requestBody = """
            {
                "name": "Updated Card",
                "lastFourDigits": "5678",
                "limit": 10000.00,
                "bankId": 1,
                "cardType": "PHYSICAL"
            }
            """;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when bank not found on update")
    void shouldReturn400WhenBankNotFoundOnUpdate() throws Exception {
        Long creditCardId = 1L;
        String requestBody = """
            {
                "name": "Updated Card",
                "lastFourDigits": "5678",
                "limit": 10000.00,
                "bankId": 999,
                "cardType": "PHYSICAL"
            }
            """;

        when(creditCardService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(creditCardService.updateCreditCard(eq(creditCardId), any(), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Bank not found"));

        mockMvc.perform(put("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bank not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
        Long creditCardId = 1L;
        String invalidRequestBody = """
            {
                "name": "",
                "lastFourDigits": "123",
                "limit": -1000.00,
                "bankId": 0
            }
            """;

        mockMvc.perform(put("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when request body is malformed JSON")
    void shouldReturn400WhenRequestBodyIsMalformedJson() throws Exception {
        Long creditCardId = 1L;
        String malformedJson = "{ invalid json }";

        mockMvc.perform(put("/api/credit-cards/{id}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    // Helper method to set credit card ID for testing
    private void setCreditCardId(CreditCard creditCard, Long id) {
        try {
            java.lang.reflect.Field idField = CreditCard.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(creditCard, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set credit card ID for testing", e);
        }
    }
}