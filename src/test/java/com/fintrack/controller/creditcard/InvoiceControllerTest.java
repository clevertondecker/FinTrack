package com.fintrack.controller.creditcard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.List;

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

import com.fintrack.application.creditcard.InstallmentProjectionService;
import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.application.user.UserService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.dto.creditcard.InvoiceResponse;

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

    @MockBean
    private InstallmentProjectionService installmentProjectionService;

    @MockBean
    private UserService userService;

    private User testUser;
    private Bank testBank;
    private CreditCard testCreditCard;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
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

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
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

        when(userService.getCurrentUser("john@example.com")).thenThrow(new IllegalArgumentException("User not found"));

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

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
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
        List<InvoiceResponse> invoiceResponses = getInvoiceResponses();

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getUserInvoices(eq(testUser))).thenReturn(invoices);
        when(invoiceService.toInvoiceResponse(any(Invoice.class), eq(testUser)))
            .thenReturn(invoiceResponses.get(0));

        // When & Then
        mockMvc.perform(get("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoices retrieved successfully"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.invoices").isArray())
                .andExpect(jsonPath("$.invoices.length()").value(1));
    }

    private List<InvoiceResponse> getInvoiceResponses() {
        InvoiceResponse invoiceResponse = new InvoiceResponse(
            testInvoice.getId(),
            testInvoice.getCreditCard().getId(),
            testInvoice.getCreditCard().getName(),
            testInvoice.getDueDate(),
            testInvoice.getMonth().toString(),
            testInvoice.getTotalAmount(),
            testInvoice.getPaidAmount(),
            testInvoice.getStatus().name(),
            testInvoice.getCreatedAt(),
            testInvoice.getUpdatedAt(),
            testInvoice.getTotalAmount(),
            List.of(),
            null
        );
      return List.of(invoiceResponse);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should get invoices by credit card successfully")
    void shouldGetInvoicesByCreditCardSuccessfully() throws Exception {
        // Given
        Long creditCardId = 1L;
        List<Invoice> invoices = List.of(testInvoice);
        List<InvoiceResponse> invoiceResponses = getInvoiceResponses();

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoicesByCreditCard(eq(creditCardId), eq(testUser))).thenReturn(invoices);
        when(invoiceService.toInvoiceResponse(any(Invoice.class), eq(testUser)))
            .thenReturn(invoiceResponses.get(0));

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

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
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
        InvoiceResponse invoiceResponse = new InvoiceResponse(
            testInvoice.getId(),
            testInvoice.getCreditCard().getId(),
            testInvoice.getCreditCard().getName(),
            testInvoice.getDueDate(),
            testInvoice.getMonth().toString(),
            testInvoice.getTotalAmount(),
            testInvoice.getPaidAmount(),
            testInvoice.getStatus().name(),
            testInvoice.getCreatedAt(),
            testInvoice.getUpdatedAt(),
            testInvoice.getTotalAmount(),
            List.of(),
            null
        );

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser))).thenReturn(testInvoice);
        when(invoiceService.toInvoiceResponse(eq(testInvoice), eq(testUser))).thenReturn(invoiceResponse);

        // When & Then
        mockMvc.perform(get("/api/invoices/{id}", invoiceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice retrieved successfully"))
                .andExpect(jsonPath("$.invoice").exists())
                .andExpect(jsonPath("$.invoice.invoiceMonth").value(testInvoice.getMonth().toString()));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 404 when specific invoice not found")
    void shouldReturn404WhenSpecificInvoiceNotFound() throws Exception {
        // Given
        Long invoiceId = 999L;

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice not found"));

        // When & Then
        mockMvc.perform(get("/api/invoices/{id}", invoiceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice not found"));
    }

    // --- Delete Invoice Tests ---

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should delete invoice successfully as owner")
    void shouldDeleteInvoiceSuccessfully() throws Exception {
        Long invoiceId = 1L;

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser))).thenReturn(testInvoice);
        doNothing().when(invoiceService).deleteInvoice(eq(invoiceId));

        mockMvc.perform(delete("/api/invoices/{id}", invoiceId))
                .andExpect(status().isNoContent());

        verify(invoiceService).deleteInvoice(invoiceId);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when deleting invoice not owned by user")
    void shouldReturn400WhenDeletingInvoiceNotOwned() throws Exception {
        Long invoiceId = 999L;

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice not found"));

        mockMvc.perform(delete("/api/invoices/{id}", invoiceId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice not found"));

        verify(invoiceService, never()).deleteInvoice(any());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when user not found for delete")
    void shouldReturn400WhenUserNotFoundForDelete() throws Exception {
        when(userService.getCurrentUser("john@example.com")).thenThrow(new IllegalArgumentException("User not found"));

        mockMvc.perform(delete("/api/invoices/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    // --- Delete Info Tests ---

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return delete info with no shared items")
    void shouldReturnDeleteInfoNoShares() throws Exception {
        Long invoiceId = 1L;

        Category cat = Category.of("Food", "#FF0000");
        InvoiceItem item = InvoiceItem.of(testInvoice, "Simple Item",
            new BigDecimal("50.00"), cat, LocalDate.of(2024, 1, 10));
        testInvoice.addItem(item);

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser))).thenReturn(testInvoice);

        mockMvc.perform(get("/api/invoices/{id}/delete-info", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.sharedItems").value(0))
                .andExpect(jsonPath("$.totalShares").value(0))
                .andExpect(jsonPath("$.paidShares").value(0));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return delete info with shared items")
    void shouldReturnDeleteInfoWithShares() throws Exception {
        Long invoiceId = 1L;

        User otherUser = User.createLocalUser("Jane Doe", "jane@example.com", "pass123", Set.of(Role.USER));
        Category cat = Category.of("Food", "#FF0000");
        InvoiceItem itemWithShares = InvoiceItem.of(testInvoice, "Shared Dinner",
            new BigDecimal("200.00"), cat, LocalDate.of(2024, 1, 20));

        ItemShare share = ItemShare.of(otherUser, itemWithShares,
            new BigDecimal("0.50"), new BigDecimal("100.00"), false);
        itemWithShares.addShare(share);

        Invoice invoiceWithShares = Invoice.of(testCreditCard, YearMonth.of(2024, 2), LocalDate.of(2024, 2, 10));
        setInvoiceId(invoiceWithShares, 1L);
        invoiceWithShares.addItem(itemWithShares);

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser))).thenReturn(invoiceWithShares);

        mockMvc.perform(get("/api/invoices/{id}/delete-info", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.sharedItems").value(1))
                .andExpect(jsonPath("$.totalShares").value(1))
                .andExpect(jsonPath("$.paidShares").value(0));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return delete info with paid shares")
    void shouldReturnDeleteInfoWithPaidShares() throws Exception {
        Long invoiceId = 1L;

        User otherUser = User.createLocalUser("Jane Doe", "jane@example.com", "pass123", Set.of(Role.USER));
        Category cat = Category.of("Food", "#FF0000");
        InvoiceItem itemWithShares = InvoiceItem.of(testInvoice, "Shared Dinner",
            new BigDecimal("200.00"), cat, LocalDate.of(2024, 1, 20));

        ItemShare share = ItemShare.of(otherUser, itemWithShares,
            new BigDecimal("0.50"), new BigDecimal("100.00"), false);
        share.markAsPaid("PIX", java.time.LocalDateTime.now());
        itemWithShares.addShare(share);

        Invoice invoiceWithShares = Invoice.of(testCreditCard, YearMonth.of(2024, 2), LocalDate.of(2024, 2, 10));
        setInvoiceId(invoiceWithShares, 1L);
        invoiceWithShares.addItem(itemWithShares);

        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(invoiceId), eq(testUser))).thenReturn(invoiceWithShares);

        mockMvc.perform(get("/api/invoices/{id}/delete-info", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidShares").value(1));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    @DisplayName("Should return 400 when invoice not found for delete info")
    void shouldReturn400WhenInvoiceNotFoundForDeleteInfo() throws Exception {
        when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
        when(invoiceService.getInvoice(eq(999L), eq(testUser)))
            .thenThrow(new IllegalArgumentException("Invoice not found"));

        mockMvc.perform(get("/api/invoices/{id}/delete-info", 999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invoice not found"));
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