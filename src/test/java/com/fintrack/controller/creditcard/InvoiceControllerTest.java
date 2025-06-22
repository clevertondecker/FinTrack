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
import com.fintrack.domain.creditcard.InvoiceStatus;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.config.TestSecurityConfig;

@WebMvcTest(InvoiceController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("InvoiceController Tests")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    private User testUser;
    private Bank testBank;
    private CreditCard testCreditCard;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        testUser = User.of("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("NU", "Nubank");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        testInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));

        // Set IDs using reflection for testing
        setCreditCardId(testCreditCard, 1L);
        setInvoiceId(testInvoice, 1L);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should create invoice successfully")
    void shouldCreateInvoiceSuccessfully() throws Exception {
        // Given
        String createInvoiceJson = """
        {
          "creditCardId": 1,
          "dueDate": "2024-02-10"
        }
        """;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.createInvoice(any(), eq(testUser))).thenReturn(testInvoice);

        // When & Then
        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createInvoiceJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice created successfully"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.creditCardId").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when user not found")
    void shouldReturn400WhenUserNotFound() throws Exception {
        // Given
        String createInvoiceJson = """
        {
          "creditCardId": 1,
          "dueDate": "2024-02-10"
        }
        """;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createInvoiceJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when credit card not found")
    void shouldReturn400WhenCreditCardNotFound() throws Exception {
        // Given
        String createInvoiceJson = """
        {
          "creditCardId": 999,
          "dueDate": "2024-02-10"
        }
        """;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.createInvoice(any(), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Credit card not found"));

        // When & Then
        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createInvoiceJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit card not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get user invoices successfully")
    void shouldGetUserInvoicesSuccessfully() throws Exception {
        // Given
        List<Invoice> invoices = List.of(testInvoice);
        Map<String, Object> invoiceDto = new HashMap<>();
        invoiceDto.put("id", 1L);
        invoiceDto.put("creditCardId", 1L);
        invoiceDto.put("creditCardName", "Test Card");
        invoiceDto.put("dueDate", "2024-02-10");
        invoiceDto.put("totalAmount", BigDecimal.ZERO);
        invoiceDto.put("paidAmount", BigDecimal.ZERO);
        invoiceDto.put("status", "OPEN");
        invoiceDto.put("createdAt", testInvoice.getCreatedAt());
        invoiceDto.put("updatedAt", testInvoice.getUpdatedAt());

        List<Map<String, Object>> invoiceDtos = List.of(invoiceDto);

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getUserInvoices(eq(testUser))).thenReturn(invoices);
        when(invoiceService.toInvoiceDtos(eq(invoices))).thenReturn(invoiceDtos);

        // When & Then
        mockMvc.perform(get("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoices retrieved successfully"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.invoices").isArray())
                .andExpect(jsonPath("$.invoices.length()").value(1));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get invoices by credit card successfully")
    void shouldGetInvoicesByCreditCardSuccessfully() throws Exception {
        // Given
        Long creditCardId = 1L;
        List<Invoice> invoices = List.of(testInvoice);
        Map<String, Object> invoiceDto = new HashMap<>();
        invoiceDto.put("id", 1L);
        invoiceDto.put("creditCardId", 1L);
        invoiceDto.put("creditCardName", "Test Card");
        invoiceDto.put("dueDate", "2024-02-10");
        invoiceDto.put("totalAmount", BigDecimal.ZERO);
        invoiceDto.put("paidAmount", BigDecimal.ZERO);
        invoiceDto.put("status", "OPEN");
        invoiceDto.put("createdAt", testInvoice.getCreatedAt());
        invoiceDto.put("updatedAt", testInvoice.getUpdatedAt());

        List<Map<String, Object>> invoiceDtos = List.of(invoiceDto);

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoicesByCreditCard(eq(creditCardId), eq(testUser))).thenReturn(invoices);
        when(invoiceService.toInvoiceDtos(eq(invoices))).thenReturn(invoiceDtos);

        // When & Then
        mockMvc.perform(get("/api/invoices/credit-card/{creditCardId}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoices retrieved successfully"))
                .andExpect(jsonPath("$.creditCardId").value(creditCardId))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.invoices").isArray());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when credit card not found for invoices")
    void shouldReturn400WhenCreditCardNotFoundForInvoices() throws Exception {
        // Given
        Long creditCardId = 999L;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoicesByCreditCard(eq(creditCardId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Credit card not found"));

        // When & Then
        mockMvc.perform(get("/api/invoices/credit-card/{creditCardId}", creditCardId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit card not found"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get specific invoice successfully")
    void shouldGetSpecificInvoiceSuccessfully() throws Exception {
        // Given
        Long invoiceId = 1L;
        Map<String, Object> invoiceDto = new HashMap<>();
        invoiceDto.put("id", 1L);
        invoiceDto.put("creditCardId", 1L);
        invoiceDto.put("creditCardName", "Test Card");
        invoiceDto.put("dueDate", "2024-02-10");
        invoiceDto.put("totalAmount", BigDecimal.ZERO);
        invoiceDto.put("paidAmount", BigDecimal.ZERO);
        invoiceDto.put("status", "OPEN");
        invoiceDto.put("createdAt", testInvoice.getCreatedAt());
        invoiceDto.put("updatedAt", testInvoice.getUpdatedAt());

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser))).thenReturn(testInvoice);
        when(invoiceService.toInvoiceDto(eq(testInvoice))).thenReturn(invoiceDto);

        // When & Then
        mockMvc.perform(get("/api/invoices/{id}", invoiceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice retrieved successfully"))
                .andExpect(jsonPath("$.invoice").exists());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when specific invoice not found")
    void shouldReturn404WhenSpecificInvoiceNotFound() throws Exception {
        // Given
        Long invoiceId = 999L;

        when(invoiceService.findUserByUsername(eq("john@example.com"))).thenReturn(Optional.of(testUser));
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice not found"));

        // When & Then
        mockMvc.perform(get("/api/invoices/{id}", invoiceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
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
} 