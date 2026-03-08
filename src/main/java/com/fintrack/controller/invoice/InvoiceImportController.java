package com.fintrack.controller.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.dto.invoice.ConfirmImportRequest;
import com.fintrack.dto.invoice.ConfirmImportResponse;
import com.fintrack.dto.invoice.ImportInvoiceRequest;
import com.fintrack.dto.invoice.ImportInvoiceResponse;
import com.fintrack.dto.invoice.ImportPreviewResponse;
import com.fintrack.dto.invoice.ImportProgressResponse;
import com.fintrack.application.invoice.InvoiceImportService;
import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.User;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for invoice import operations.
 */
@RestController
@RequestMapping("/api/invoice-imports")
@CrossOrigin(origins = "*")
public class InvoiceImportController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceImportController.class);

    private final InvoiceImportService invoiceImportService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public InvoiceImportController(InvoiceImportService invoiceImportService,
                                  UserService userService,
                                  ObjectMapper objectMapper) {
        this.invoiceImportService = invoiceImportService;
        this.userService = userService;
        this.objectMapper = objectMapper;
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
        requireAuthentication(userDetails);

        try {
            ImportInvoiceRequest request =
                objectMapper.readValue(requestJson, ImportInvoiceRequest.class);

            User user = userService.getCurrentUser(userDetails.getUsername());
            ImportInvoiceResponse response = invoiceImportService.importInvoice(file, request, user);
            return ResponseEntity.accepted().body(response);

        } catch (IOException e) {
            logger.error("Error importing invoice file='{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ImportInvoiceResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid import request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ImportInvoiceResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during import file='{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ImportInvoiceResponse.error(e.getMessage()));
        }
    }

    /**
     * Uploads a file and returns a preview with detected cards and auto-match results.
     * No credit card selection is required; cards are detected from the PDF.
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> previewImport(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Validate.notNull(file, "File must not be null.");
        requireAuthentication(userDetails);

        try {
            logger.info("Preview import request: file='{}', size={}, user='{}'",
                    file.getOriginalFilename(), file.getSize(), userDetails.getUsername());
            User user = userService.getCurrentUser(userDetails.getUsername());
            ImportPreviewResponse response = invoiceImportService.previewImport(file, user);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error previewing import file='{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "type", "IO_ERROR"));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid preview request file='{}': {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "type", "VALIDATION_ERROR"));
        } catch (Exception e) {
            logger.error("Unexpected error previewing import file='{}': {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
        }
    }

    /**
     * Confirms a previewed import with user-verified card mappings, creating invoices per card.
     */
    @PostMapping("/{importId}/confirm")
    public ResponseEntity<?> confirmImport(
            @PathVariable Long importId,
            @Valid @RequestBody ConfirmImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        requireAuthentication(userDetails);

        try {
            logger.info("Confirm import request: importId={}, mappings={}, user='{}'",
                    importId, request.cardMappings().size(), userDetails.getUsername());
            User user = userService.getCurrentUser(userDetails.getUsername());
            ConfirmImportResponse response =
                    invoiceImportService.confirmImport(importId, request, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid confirm request importId={}: {}", importId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "type", "VALIDATION_ERROR"));
        } catch (IllegalStateException e) {
            logger.warn("Invalid import state importId={}: {}", importId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage(), "type", "STATE_ERROR"));
        } catch (Exception e) {
            logger.error("Unexpected error confirming import importId={}: {}", importId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
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
        requireAuthentication(userDetails);
        
        try {
            User user = userService.getCurrentUser(userDetails.getUsername());
            ImportProgressResponse progress =
                invoiceImportService.getImportProgress(importId, user);
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
    public ResponseEntity<List<ImportInvoiceResponse>> getUserImports(
            @AuthenticationPrincipal UserDetails userDetails) {
        requireAuthentication(userDetails);
        User user = userService.getCurrentUser(userDetails.getUsername());
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
        requireAuthentication(userDetails);
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<ImportInvoiceResponse> imports = invoiceImportService.getUserImportsByStatus(user, status);
        return ResponseEntity.ok(imports);
    }

    private void requireAuthentication(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
    }

    /**
     * Gets failed imports for the authenticated user.
     *
     * @param userDetails the authenticated user details. Cannot be null.
     * @return a list of failed import responses. Never null, may be empty.
     */
    @GetMapping("/failed")
    public ResponseEntity<List<ImportInvoiceResponse>> getFailedImports(
            @AuthenticationPrincipal UserDetails userDetails) {
        requireAuthentication(userDetails);
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<ImportInvoiceResponse> failedImports =
            invoiceImportService.getUserImportsByStatus(user, ImportStatus.FAILED);
        return ResponseEntity.ok(failedImports);
    }

    /**
     * Gets imports requiring manual review for the authenticated user.
     *
     * @param userDetails the authenticated user details. Cannot be null.
     * @return a list of manual review import responses. Never null, may be empty.
     */
    @GetMapping("/manual-review")
    public ResponseEntity<List<ImportInvoiceResponse>> getManualReviewImports(
            @AuthenticationPrincipal UserDetails userDetails) {
        requireAuthentication(userDetails);
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<ImportInvoiceResponse> imports =
            invoiceImportService.getUserImportsByStatus(user, ImportStatus.MANUAL_REVIEW);
        return ResponseEntity.ok(imports);
    }
} 