package com.fintrack.controller.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.invoice.InvoiceImportService;
import com.fintrack.domain.invoice.ImportSource;
import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.invoice.ImportInvoiceRequest;
import com.fintrack.dto.invoice.ImportInvoiceResponse;
import com.fintrack.dto.invoice.ImportProgressResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceImportController.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceImportControllerTest {

    @Mock
    private InvoiceImportService invoiceImportService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InvoiceImportController invoiceImportController;

    private User testUser;
    private UserDetails testUserDetails;
    private MockMultipartFile testFile;
    private String testRequestJson;
    private ImportInvoiceRequest testRequest;

    @BeforeEach
    void setUp() throws Exception {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        testUser = User.createLocalUser("Test User", "test@example.com", "password123", roles);
        testUserDetails = org.springframework.security.core.userdetails.User
            .withUsername("test@example.com")
            .password("password123")
            .authorities(roles.stream().map(role -> new SimpleGrantedAuthority(role.name())).toList())
            .build();
        
        testFile = new MockMultipartFile(
            "file", 
            "test-invoice.pdf", 
            "application/pdf", 
            "test content".getBytes()
        );
        testRequestJson = "{\"creditCardId\": 1}";
        testRequest = new ImportInvoiceRequest(1L);
        
        // Mock ObjectMapper behavior - using lenient to avoid unnecessary stubbing errors
        lenient().when(objectMapper.readValue(anyString(), eq(ImportInvoiceRequest.class)))
            .thenReturn(testRequest);
        
        // Mock UserRepository behavior
        lenient().when(userRepository.findByEmail(Email.of("test@example.com")))
            .thenReturn(Optional.of(testUser));
    }

    @Test
    void importInvoice_WithValidData_ShouldReturnAcceptedResponse() throws IOException {
        // Given
        ImportInvoiceResponse expectedResponse = createTestImportResponse();
        when(invoiceImportService.importInvoice(any(), any(), any())).thenReturn(expectedResponse);

        // When
        ResponseEntity<ImportInvoiceResponse> response = invoiceImportController.importInvoice(
            testFile, testRequestJson, testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(invoiceImportService).importInvoice(eq(testFile), eq(testRequest), eq(testUser));
        verify(objectMapper).readValue(eq(testRequestJson), eq(ImportInvoiceRequest.class));
    }

    @Test
    void importInvoice_WithServiceException_ShouldReturnBadRequest() throws IOException {
        // Given
        when(invoiceImportService.importInvoice(any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Invalid credit card"));

        // When
        ResponseEntity<ImportInvoiceResponse> response = invoiceImportController.importInvoice(
            testFile, testRequestJson, testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Dados inválidos");
        verify(invoiceImportService).importInvoice(eq(testFile), eq(testRequest), eq(testUser));
        verify(objectMapper).readValue(eq(testRequestJson), eq(ImportInvoiceRequest.class));
    }

    @Test
    void importInvoice_WithIOException_ShouldReturnInternalServerError() throws IOException {
        // Given
        when(invoiceImportService.importInvoice(any(), any(), any()))
            .thenThrow(new IOException("File processing error"));

        // When
        ResponseEntity<ImportInvoiceResponse> response = invoiceImportController.importInvoice(
            testFile, testRequestJson, testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Erro ao processar arquivo");
        verify(invoiceImportService).importInvoice(eq(testFile), eq(testRequest), eq(testUser));
        verify(objectMapper).readValue(eq(testRequestJson), eq(ImportInvoiceRequest.class));
    }

    @Test
    void importInvoice_WithObjectMapperException_ShouldReturnInternalServerError() throws IOException {
        // Given
        lenient().when(objectMapper.readValue(anyString(), eq(ImportInvoiceRequest.class)))
            .thenThrow(new RuntimeException("JSON parsing error"));

        // When
        ResponseEntity<ImportInvoiceResponse> response = invoiceImportController.importInvoice(
            testFile, testRequestJson, testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Erro inesperado");
        verify(objectMapper).readValue(eq(testRequestJson), eq(ImportInvoiceRequest.class));
        verify(invoiceImportService, never()).importInvoice(any(), any(), any());
    }

    @Test
    void getImportProgress_WithValidImport_ShouldReturnOkResponse() {
        // Given
        ImportProgressResponse expectedResponse = createTestProgressResponse();
        when(invoiceImportService.getImportProgress(1L, testUser)).thenReturn(expectedResponse);

        // When
        ResponseEntity<ImportProgressResponse> response =
            invoiceImportController.getImportProgress(1L, testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(invoiceImportService).getImportProgress(1L, testUser);
    }

    @Test
    void getImportProgress_WithImportNotFound_ShouldReturnNotFound() {
        // Given
        when(invoiceImportService.getImportProgress(1L, testUser))
            .thenThrow(new IllegalArgumentException("Import not found"));

        // When
        ResponseEntity<ImportProgressResponse> response =
            invoiceImportController.getImportProgress(1L, testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(invoiceImportService).getImportProgress(1L, testUser);
    }

    @Test
    void getUserImports_ShouldReturnOkResponse() {
        // Given
        List<ImportInvoiceResponse> expectedResponses = List.of(createTestImportResponse());
        when(invoiceImportService.getUserImports(testUser)).thenReturn(expectedResponses);

        // When
        ResponseEntity<List<ImportInvoiceResponse>> response = invoiceImportController.getUserImports(testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponses);
        verify(invoiceImportService).getUserImports(testUser);
    }

    @Test
    void getUserImportsByStatus_ShouldReturnOkResponse() {
        // Given
        List<ImportInvoiceResponse> expectedResponses = List.of(createTestImportResponse());
        when(invoiceImportService.getUserImportsByStatus(testUser, ImportStatus.PENDING))
            .thenReturn(expectedResponses);

        // When
        ResponseEntity<List<ImportInvoiceResponse>> response = invoiceImportController.getUserImportsByStatus(
            ImportStatus.PENDING, testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponses);
        verify(invoiceImportService).getUserImportsByStatus(testUser, ImportStatus.PENDING);
    }

    @Test
    void getFailedImports_ShouldReturnOkResponse() {
        // Given
        List<ImportInvoiceResponse> expectedResponses = List.of(createTestImportResponse());
        when(invoiceImportService.getUserImportsByStatus(testUser, ImportStatus.FAILED))
            .thenReturn(expectedResponses);

        // When
        ResponseEntity<List<ImportInvoiceResponse>> response =
            invoiceImportController.getFailedImports(testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponses);
        verify(invoiceImportService).getUserImportsByStatus(testUser, ImportStatus.FAILED);
    }

    @Test
    void getManualReviewImports_ShouldReturnOkResponse() {
        // Given
        List<ImportInvoiceResponse> expectedResponses = List.of(createTestImportResponse());
        when(invoiceImportService.getUserImportsByStatus(testUser, ImportStatus.MANUAL_REVIEW))
            .thenReturn(expectedResponses);

        // When
        ResponseEntity<List<ImportInvoiceResponse>> response =
            invoiceImportController.getManualReviewImports(testUserDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponses);
        verify(invoiceImportService).getUserImportsByStatus(testUser, ImportStatus.MANUAL_REVIEW);
    }

    // Helper methods
    private ImportInvoiceResponse createTestImportResponse() {
        return new ImportInvoiceResponse(
            "Import successful",
            1L,
            ImportStatus.PENDING,
            ImportSource.PDF,
            "test-invoice.pdf",
            null,
            LocalDateTime.now(),
            null,
            BigDecimal.valueOf(1500.00),
            "Test Bank",
            "1234"
        );
    }

    private ImportProgressResponse createTestProgressResponse() {
        return new ImportProgressResponse(
            1L,
            ImportStatus.PENDING,
            "Aguardando processamento",
            LocalDateTime.now(),
            null,
            null,
            null,
            BigDecimal.valueOf(1500.00),
            "Test Bank",
            "1234",
            false
        );
    }
} 