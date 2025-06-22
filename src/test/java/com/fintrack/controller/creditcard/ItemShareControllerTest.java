package com.fintrack.controller.creditcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.domain.creditcard.*;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CreateItemShareRequest;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemShareController.class)
@AutoConfigureMockMvc
@Import(com.fintrack.config.TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("ItemShareController Tests")
class ItemShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.fintrack.application.creditcard.ExpenseSharingServiceImpl expenseSharingService;

    @MockBean
    private com.fintrack.application.creditcard.InvoiceService invoiceService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private InvoiceItem invoiceItem;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        user1 = User.of("User 1", "user1@example.com", "password", Set.of(Role.USER));
        user2 = User.of("User 2", "user2@example.com", "password", Set.of(Role.USER));
        invoice = Invoice.of(
                CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), user1, Bank.of("NU", "Nubank")),
                YearMonth.of(2024, 2),
                LocalDate.of(2024, 2, 10)
        );
        invoiceItem = InvoiceItem.of(invoice, "Test Item", new BigDecimal("100.00"), null, LocalDate.of(2024, 1, 15));
        setInvoiceId(invoice, 1L);
        setInvoiceItemId(invoiceItem, 1L);
    }

    @Test
    @WithMockUser(username = "user1@example.com")
    @DisplayName("Should save and return shares correctly")
    void shouldSaveAndReturnSharesCorrectly() throws Exception {
        // Arrange
        CreateItemShareRequest.UserShare share1 = new CreateItemShareRequest.UserShare(user1.getId(), new BigDecimal("0.6"), true);
        CreateItemShareRequest.UserShare share2 = new CreateItemShareRequest.UserShare(user2.getId(), new BigDecimal("0.4"), false);
        CreateItemShareRequest request = new CreateItemShareRequest(List.of(share1, share2));

        // Mock user and item
        when(invoiceService.findUserByUsername(anyString())).thenReturn(Optional.of(user1));
        when(invoiceService.getInvoiceItem(eq(1L), eq(1L), eq(user1))).thenReturn(invoiceItem);

        // Mock saving shares
        ItemShare itemShare1 = ItemShare.of(user1, invoiceItem, new BigDecimal("0.6"), new BigDecimal("60.00"), true);
        ItemShare itemShare2 = ItemShare.of(user2, invoiceItem, new BigDecimal("0.4"), new BigDecimal("40.00"), false);
        when(expenseSharingService.createSharesFromUserIds(eq(invoiceItem), anyList()))
                .thenReturn(List.of(itemShare1, itemShare2));
        when(expenseSharingService.getSharesForItem(invoiceItem))
                .thenReturn(List.of(itemShare1, itemShare2));

        // POST shares
        mockMvc.perform(post("/api/invoices/{invoiceId}/items/{itemId}/shares", 1L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // GET shares
        mockMvc.perform(get("/api/invoices/{invoiceId}/items/{itemId}/shares", 1L, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shares.length()").value(2))
                .andExpect(jsonPath("$.shares[0].userId").value(user1.getId()))
                .andExpect(jsonPath("$.shares[0].percentage").value(0.6))
                .andExpect(jsonPath("$.shares[0].amount").value(60.00))
                .andExpect(jsonPath("$.shares[0].responsible").value(true))
                .andExpect(jsonPath("$.shares[1].userId").value(user2.getId()))
                .andExpect(jsonPath("$.shares[1].percentage").value(0.4))
                .andExpect(jsonPath("$.shares[1].amount").value(40.00))
                .andExpect(jsonPath("$.shares[1].responsible").value(false));
    }

    // Métodos utilitários para setar IDs via reflexão
    private void setInvoiceId(Invoice invoice, Long id) {
        try {
            java.lang.reflect.Field idField = Invoice.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invoice, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setInvoiceItemId(InvoiceItem invoiceItem, Long id) {
        try {
            java.lang.reflect.Field idField = InvoiceItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invoiceItem, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
} 