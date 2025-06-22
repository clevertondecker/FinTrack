package com.fintrack.controller.creditcard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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

import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.dto.creditcard.InvoiceItemResponse;

@WebMvcTest(InvoiceItemController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("InvoiceItemController Tests")
class InvoiceItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    private User testUser;
    private Bank testBank;
    private CreditCard testCreditCard;
    private Invoice testInvoice;
    private InvoiceItem testInvoiceItem;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.of("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("NU", "Nubank");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        testInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 2), LocalDate.of(2024, 2, 10));
        testCategory = Category.of("Food", "#FF0000");
        testInvoiceItem = InvoiceItem.of(testInvoice, "Test Item", new BigDecimal("100.00"), testCategory, LocalDate.of(2024, 1, 15));

        // Set IDs using reflection for testing
        setCreditCardId(testCreditCard, 1L);
        setInvoiceId(testInvoice, 1L);
        setInvoiceItemId(testInvoiceItem, 1L);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should create invoice item successfully")
    void shouldCreateInvoiceItemSuccessfully() throws Exception {
        // Given
        Long invoiceId = 1L;
        String requestBody = """
            {
                "description": "Test Item",
                "amount": 100.00,
                "categoryId": 1,
                "purchaseDate": "2024-01-15"
            }
            """;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.createInvoiceItem(eq(invoiceId), any(), eq(testUser))).thenReturn(testInvoice);
        when(invoiceService.getInvoiceItems(eq(invoiceId), eq(testUser))).thenReturn(List.of(testInvoiceItem));

        // When & Then
        mockMvc.perform(post("/api/invoices/{invoiceId}/items", invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice item created successfully"))
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.description").value("Test Item"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when user not found")
    void shouldReturn400WhenUserNotFound() throws Exception {
        // Given
        Long invoiceId = 1L;
        String requestBody = """
            {
                "description": "Test Item",
                "amount": 100.00,
                "categoryId": 1,
                "purchaseDate": "2024-01-15"
            }
            """;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/invoices/{invoiceId}/items", invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when invoice not found")
    void shouldReturn400WhenInvoiceNotFound() throws Exception {
        // Given
        Long invoiceId = 999L;
        String requestBody = """
            {
                "description": "Test Item",
                "amount": 100.00,
                "categoryId": 1,
                "purchaseDate": "2024-01-15"
            }
            """;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.createInvoiceItem(eq(invoiceId), any(), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice not found"));

        // When & Then
        mockMvc.perform(post("/api/invoices/{invoiceId}/items", invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get invoice items successfully")
    void shouldGetInvoiceItemsSuccessfully() throws Exception {
        // Given
        Long invoiceId = 1L;
        List<InvoiceItem> invoiceItems = List.of(testInvoiceItem);
        InvoiceItemResponse invoiceItemResponse = new InvoiceItemResponse(
            testInvoiceItem.getId(),
            testInvoiceItem.getInvoice().getId(),
            testInvoiceItem.getDescription(),
            testInvoiceItem.getAmount(),
            testInvoiceItem.getCategory() != null ? testInvoiceItem.getCategory().getName() : null,
            testInvoiceItem.getPurchaseDate().toString(),
            testInvoiceItem.getCreatedAt()
        );
        List<InvoiceItemResponse> invoiceItemResponses = List.of(invoiceItemResponse);

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoiceItems(eq(invoiceId), eq(testUser))).thenReturn(invoiceItems);
        when(invoiceService.toInvoiceItemResponseList(eq(invoiceItems))).thenReturn(invoiceItemResponses);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser))).thenReturn(testInvoice);

        // When & Then
        mockMvc.perform(get("/api/invoices/{invoiceId}/items", invoiceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice items retrieved successfully"))
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when invoice not found for items")
    void shouldReturn400WhenInvoiceNotFoundForItems() throws Exception {
        // Given
        Long invoiceId = 999L;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoiceItems(eq(invoiceId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice not found"));

        // When & Then
        mockMvc.perform(get("/api/invoices/{invoiceId}/items", invoiceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get specific invoice item successfully")
    void shouldGetSpecificInvoiceItemSuccessfully() throws Exception {
        // Given
        Long invoiceId = 1L;
        Long itemId = 1L;
        InvoiceItemResponse invoiceItemResponse = new InvoiceItemResponse(
            testInvoiceItem.getId(),
            testInvoiceItem.getInvoice().getId(),
            testInvoiceItem.getDescription(),
            testInvoiceItem.getAmount(),
            testInvoiceItem.getCategory() != null ? testInvoiceItem.getCategory().getName() : null,
            testInvoiceItem.getPurchaseDate().toString(),
            testInvoiceItem.getCreatedAt()
        );

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoiceItem(eq(invoiceId), eq(itemId), eq(testUser))).thenReturn(testInvoiceItem);
        when(invoiceService.toInvoiceItemResponse(eq(testInvoiceItem))).thenReturn(invoiceItemResponse);

        // When & Then
        mockMvc.perform(get("/api/invoices/{invoiceId}/items/{itemId}", invoiceId, itemId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice item retrieved successfully"))
                .andExpect(jsonPath("$.item").exists());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when specific invoice item not found")
    void shouldReturn404WhenSpecificInvoiceItemNotFound() throws Exception {
        // Given
        Long invoiceId = 1L;
        Long itemId = 999L;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoiceItem(eq(invoiceId), eq(itemId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice item not found"));

        // When & Then
        mockMvc.perform(get("/api/invoices/{invoiceId}/items/{itemId}", invoiceId, itemId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice item not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should delete invoice item successfully")
    void shouldDeleteInvoiceItemSuccessfully() throws Exception {
        // Given
        Long invoiceId = 1L;
        Long itemId = 1L;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.deleteInvoiceItem(eq(invoiceId), eq(itemId), eq(testUser))).thenReturn(testInvoice);

        // When & Then
        mockMvc.perform(delete("/api/invoices/{invoiceId}/items/{itemId}", invoiceId, itemId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice item deleted successfully"))
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.itemId").value(itemId));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when invoice item not found for deletion")
    void shouldReturn404WhenInvoiceItemNotFoundForDeletion() throws Exception {
        // Given
        Long invoiceId = 1L;
        Long itemId = 999L;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.deleteInvoiceItem(eq(invoiceId), eq(itemId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice item not found"));

        // When & Then
        mockMvc.perform(delete("/api/invoices/{invoiceId}/items/{itemId}", invoiceId, itemId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice item not found"));
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

    // Helper method to set invoice ID for testing
    private void setInvoiceId(Invoice invoice, Long id) {
        try {
            java.lang.reflect.Field idField = Invoice.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invoice, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set invoice ID for testing", e);
        }
    }

    // Helper method to set invoice item ID for testing
    private void setInvoiceItemId(InvoiceItem invoiceItem, Long id) {
        try {
            java.lang.reflect.Field idField = InvoiceItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invoiceItem, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set invoice item ID for testing", e);
        }
    }
} 