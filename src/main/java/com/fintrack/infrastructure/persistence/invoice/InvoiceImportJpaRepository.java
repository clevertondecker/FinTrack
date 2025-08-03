package com.fintrack.infrastructure.persistence.invoice;

import com.fintrack.domain.invoice.InvoiceImport;
import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for InvoiceImport entities.
 */
@Repository
public interface InvoiceImportJpaRepository extends JpaRepository<InvoiceImport, Long> {

    /**
     * Finds all imports for a specific user.
     *
     * @param user the user to find imports for. Cannot be null.
     * @return a list of imports for the user. Never null, may be empty.
     */
    List<InvoiceImport> findByUserOrderByImportedAtDesc(User user);

    /**
     * Finds all imports for a specific user with a given status.
     *
     * @param user the user to find imports for. Cannot be null.
     * @param status the status to filter by. Cannot be null.
     * @return a list of imports for the user with the given status. Never null, may be empty.
     */
    List<InvoiceImport> findByUserAndStatusOrderByImportedAtDesc(User user, ImportStatus status);

    /**
     * Finds an import by ID and user.
     *
     * @param id the import ID. Cannot be null.
     * @param user the user. Cannot be null.
     * @return an Optional containing the import if found, empty otherwise.
     */
    Optional<InvoiceImport> findByIdAndUser(Long id, User user);

    /**
     * Finds all pending imports that need processing.
     *
     * @return a list of pending imports. Never null, may be empty.
     */
    @Query("SELECT ii FROM InvoiceImport ii WHERE ii.status = 'PENDING' ORDER BY ii.importedAt ASC")
    List<InvoiceImport> findPendingImports();

    /**
     * Finds all imports created within a date range.
     *
     * @param user the user to find imports for. Cannot be null.
     * @param startDate the start date. Cannot be null.
     * @param endDate the end date. Cannot be null.
     * @return a list of imports within the date range. Never null, may be empty.
     */
    @Query("SELECT ii FROM InvoiceImport ii WHERE ii.user = :user " +
           "AND ii.importedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ii.importedAt DESC")
    List<InvoiceImport> findByUserAndDateRange(@Param("user") User user,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Counts imports by status for a specific user.
     *
     * @param user the user to count imports for. Cannot be null.
     * @param status the status to count. Cannot be null.
     * @return the count of imports with the given status.
     */
    long countByUserAndStatus(User user, ImportStatus status);

    /**
     * Finds all failed imports for a specific user.
     *
     * @param user the user to find failed imports for. Cannot be null.
     * @return a list of failed imports. Never null, may be empty.
     */
    @Query("SELECT ii FROM InvoiceImport ii WHERE ii.user = :user AND ii.status = 'FAILED' ORDER BY ii.importedAt DESC")
    List<InvoiceImport> findFailedImportsByUser(@Param("user") User user);

    /**
     * Finds all imports that require manual review.
     *
     * @param user the user to find imports for. Cannot be null.
     * @return a list of imports requiring manual review. Never null, may be empty.
     */
    @Query("SELECT ii FROM InvoiceImport ii WHERE ii.user = :user AND ii.status = 'MANUAL_REVIEW' ORDER BY ii.importedAt DESC")
    List<InvoiceImport> findManualReviewImportsByUser(@Param("user") User user);

    /**
     * Finds all imports that reference a specific invoice.
     * This is used to clear references before deleting invoices.
     *
     * @param invoiceId the invoice ID to find references for. Cannot be null.
     * @return a list of imports that reference the invoice. Never null, may be empty.
     */
    @Query("SELECT ii FROM InvoiceImport ii WHERE ii.createdInvoice.id = :invoiceId")
    List<InvoiceImport> findByCreatedInvoiceId(@Param("invoiceId") Long invoiceId);
} 