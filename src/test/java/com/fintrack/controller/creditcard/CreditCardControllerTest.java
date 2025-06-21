package com.fintrack.controller.creditcard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
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

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.user.UserJpaRepository;
import org.springframework.context.annotation.Import;

@WebMvcTest(CreditCardController.class)
@AutoConfigureMockMvc(addFilters = true)
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

        // Create a mock Authentication
        org.springframework.security.core.Authentication mockAuth = new org.springframework.security.core.Authentication() {
            @Override
            public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return List.of(() -> "ROLE_USER");
            }

            @Override
            public Object getCredentials() {
                return "password";
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return new org.springframework.security.core.userdetails.User("john@example.com", "password", getAuthorities());
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }

            @Override
            public String getName() {
                return "john@example.com";
            }
        };

        // When & Then
        mockMvc.perform(patch("/api/credit-cards/{id}/activate", creditCardId)
                .with(request -> {
                    request.setUserPrincipal(mockAuth);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credit card activated successfully"))
                .andExpect(jsonPath("$.id").value(creditCardId));
    }
} 