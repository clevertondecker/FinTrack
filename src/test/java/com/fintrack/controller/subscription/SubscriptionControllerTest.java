package com.fintrack.controller.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.subscription.SubscriptionService;
import com.fintrack.application.user.UserService;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.domain.subscription.BillingCycle;
import com.fintrack.domain.subscription.SubscriptionSource;
import com.fintrack.domain.subscription.SubscriptionStatus;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.subscription.CreateSubscriptionRequest;
import com.fintrack.dto.subscription.SubscriptionResponse;
import com.fintrack.dto.subscription.SubscriptionSuggestion;
import com.fintrack.dto.subscription.UpdateSubscriptionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("SubscriptionController Tests")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John", "john@test.com", "pass123", Set.of(Role.USER));
        when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("GET /api/subscriptions should list user subscriptions")
    void shouldListSubscriptions() throws Exception {
        SubscriptionResponse response = new SubscriptionResponse(
                1L, "Netflix", "netflix", new BigDecimal("29.90"),
                new CategoryResponse(1L, "Entertainment", "#8b5cf6", null, null),
                "Nubank", 1L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, SubscriptionSource.MANUAL,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1),
                new BigDecimal("29.90"), false, "DETECTED"
        );

        when(subscriptionService.getSubscriptions(eq(testUser), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/subscriptions")
                .param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Netflix"))
                .andExpect(jsonPath("$[0].expectedAmount").value(29.90))
                .andExpect(jsonPath("$[0].monthStatus").value("DETECTED"));
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("POST /api/subscriptions should create subscription")
    void shouldCreateSubscription() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "Spotify", "spotify", new BigDecimal("19.90"),
                BillingCycle.MONTHLY, null, null
        );

        SubscriptionResponse response = new SubscriptionResponse(
                2L, "Spotify", "spotify", new BigDecimal("19.90"),
                null, null, null, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, SubscriptionSource.MANUAL,
                null, null, null, false, "ACTIVE"
        );

        when(subscriptionService.create(eq(testUser), any())).thenReturn(response);

        mockMvc.perform(post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spotify"));
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("PUT /api/subscriptions/{id} should update subscription")
    void shouldUpdateSubscription() throws Exception {
        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
                "Netflix Premium", new BigDecimal("55.90"),
                BillingCycle.MONTHLY, null, null
        );

        SubscriptionResponse response = new SubscriptionResponse(
                1L, "Netflix Premium", "netflix", new BigDecimal("55.90"),
                null, null, null, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, SubscriptionSource.MANUAL,
                null, null, null, false, "ACTIVE"
        );

        when(subscriptionService.update(eq(testUser), eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/subscriptions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Netflix Premium"))
                .andExpect(jsonPath("$.expectedAmount").value(55.90));
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("DELETE /api/subscriptions/{id} should cancel subscription")
    void shouldCancelSubscription() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/1"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).cancel(testUser, 1L);
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("GET /api/subscriptions/suggestions should return suggestions")
    void shouldReturnSuggestions() throws Exception {
        SubscriptionSuggestion suggestion = new SubscriptionSuggestion(
                "spotify", "Spotify", new BigDecimal("19.90"),
                4, LocalDate.of(2025, 1, 5), LocalDate.of(2025, 4, 5),
                "Entertainment", "#8b5cf6", "Nubank"
        );

        when(subscriptionService.getSuggestions(testUser)).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/subscriptions/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantKey").value("spotify"))
                .andExpect(jsonPath("$[0].averageAmount").value(19.90))
                .andExpect(jsonPath("$[0].occurrences").value(4));
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("POST /api/subscriptions/suggestions/confirm should confirm a suggestion")
    void shouldConfirmSuggestion() throws Exception {
        SubscriptionResponse response = new SubscriptionResponse(
                3L, "Spotify", "spotify", new BigDecimal("19.90"),
                null, "Nubank", 1L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, SubscriptionSource.AUTO_DETECTED,
                LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 5),
                new BigDecimal("19.90"), false, "ACTIVE"
        );

        when(subscriptionService.confirmSuggestion(testUser, "spotify")).thenReturn(response);

        mockMvc.perform(post("/api/subscriptions/suggestions/confirm")
                .param("merchantKey", "spotify"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("AUTO_DETECTED"))
                .andExpect(jsonPath("$.merchantKey").value("spotify"));
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("POST /api/subscriptions should reject invalid request")
    void shouldRejectInvalidRequest() throws Exception {
        String invalidJson = "{\"name\":\"\",\"merchantKey\":\"\",\"expectedAmount\":-10}";

        mockMvc.perform(post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "john@test.com")
    @DisplayName("GET /api/subscriptions without month uses current month")
    void shouldDefaultToCurrentMonth() throws Exception {
        when(subscriptionService.getSubscriptions(eq(testUser), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
