package com.fintrack.controller.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.ImportInvoiceRequest;
import com.fintrack.dto.invoice.ImportInvoiceResponse;
import com.fintrack.dto.invoice.ImportProgressResponse;
import com.fintrack.application.invoice.InvoiceImportService;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.domain.user.Email;
import jakarta.validation.Valid;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST controller for invoice import operations.
 */
@RestController
@RequestMapping("/api/invoice-imports")
@CrossOrigin(origins = "*")
public class InvoiceImportController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceImportController.class);

    private final InvoiceImportService invoiceImportService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public InvoiceImportController(InvoiceImportService invoiceImportService, 
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper) {
        this.invoiceImportService = invoiceImportService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        logger.info("InvoiceImportController initialized with service: {}", invoiceImportService != null);
    }

    /**
     * Test endpoint to verify the controller is working.
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        logger.info("Test endpoint called");
        return ResponseEntity.ok("InvoiceImportController is working!");
    }

    /**
     * Uploads and imports an invoice from a file.
     *
     * @param file the file to import. Cannot be null.
     * @param requestJson the import request JSON. Cannot be null.
     * @param userDetails the authenticated user details. Cannot be null.
     * @return the import response. Never null.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportInvoiceResponse> importInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("request") String requestJson,
            @AuthenticationPrincipal UserDetails userDetails) {

        Validate.notNull(file, "File must not be null.");
        if (userDetails == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        try {
            // Parse the request JSON using ObjectMapper
            ImportInvoiceRequest request = objectMapper.readValue(requestJson, ImportInvoiceRequest.class);
            
            User user = userRepository.findByEmail(Email.of(userDetails.getUsername()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
            
            ImportInvoiceResponse response = invoiceImportService.importInvoice(file, request, user);
            return ResponseEntity.accepted().body(response);

        } catch (IOException e) {
            logger.error("Error importing invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ImportInvoiceResponse(
                    "Erro ao processar arquivo: " + e.getMessage(),
                    null, null, null, null, e.getMessage(), null, null, null, null, null
                ));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid import request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ImportInvoiceResponse(
                    "Dados inválidos: " + e.getMessage(),
                    null, null, null, null, e.getMessage(), null, null, null, null, null
                ));
        } catch (Exception e) {
            logger.error("Unexpected error during import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ImportInvoiceResponse(
                    "Erro inesperado: " + e.getMessage(),
                    null, null, null, null, e.getMessage(), null, null, null, null, null
                ));
        }
    }

    /**
     * Gets the progress of an import.
     *
     * @param importId the import ID. Cannot be null.
     * @param userDetails the authenticated user details. Cannot be null.
     * @return the import progress response. Never null.
     */
    @GetMapping("/{importId}/progress")
    public ResponseEntity<ImportProgressResponse> getImportProgress(
            @PathVariable Long importId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        
        try {
            User user = userRepository.findByEmail(Email.of(userDetails.getUsername()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
            ImportProgressResponse progress = invoiceImportService.getImportProgress(importId, user);
            return ResponseEntity.ok(progress);
        } catch (IllegalArgumentException e) {
            logger.warn("Import not found or access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Gets all imports for the authenticated user.
     *
     * @param userDetails the authenticated user details. Cannot be null.
     * @return a list of import responses. Never null, may be empty.
     */
    @GetMapping
    public ResponseEntity<List<ImportInvoiceResponse>> getUserImports(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        
        User user = userRepository.findByEmail(Email.of(userDetails.getUsername()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        List<ImportInvoiceResponse> imports = invoiceImportService.getUserImports(user);
        return ResponseEntity.ok(imports);
    }

    /**
     * Gets imports by status for the authenticated user.
     *
     * @param status the status to filter by. Cannot be null.
     * @param userDetails the authenticated user details. Cannot be null.
     * @return a list of import responses. Never null, may be empty.
     */
    @GetMapping(params = "status")
    public ResponseEntity<List<ImportInvoiceResponse>> getUserImportsByStatus(
            @RequestParam ImportStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        
        User user = userRepository.findByEmail(Email.of(userDetails.getUsername()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        List<ImportInvoiceResponse> imports = invoiceImportService.getUserImportsByStatus(user, status);
        return ResponseEntity.ok(imports);
    }

    /**
     * Gets failed imports for the authenticated user.
     *
     * @param userDetails the authenticated user details. Cannot be null.
     * @return a list of failed import responses. Never null, may be empty.
     */
    @GetMapping("/failed")
    public ResponseEntity<List<ImportInvoiceResponse>> getFailedImports(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        
        User user = userRepository.findByEmail(Email.of(userDetails.getUsername()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        List<ImportInvoiceResponse> failedImports = invoiceImportService.getUserImportsByStatus(user, ImportStatus.FAILED);
        return ResponseEntity.ok(failedImports);
    }

    /**
     * Gets imports requiring manual review for the authenticated user.
     *
     * @param userDetails the authenticated user details. Cannot be null.
     * @return a list of manual review import responses. Never null, may be empty.
     */
    @GetMapping("/manual-review")
    public ResponseEntity<List<ImportInvoiceResponse>> getManualReviewImports(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        
        User user = userRepository.findByEmail(Email.of(userDetails.getUsername()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
        List<ImportInvoiceResponse> imports = invoiceImportService.getUserImportsByStatus(user, ImportStatus.MANUAL_REVIEW);
        return ResponseEntity.ok(imports);
    }
} 