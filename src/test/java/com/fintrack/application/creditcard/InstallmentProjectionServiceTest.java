package com.fintrack.application.creditcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.ItemShareRepository;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstallmentProjectionService Tests")
class InstallmentProjectionServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ItemShareRepository itemShareRepository;

    private InstallmentProjectionService projectionService;

    private User owner;
    private Bank bank;
    private CreditCard creditCard;
    private Category category;

    @BeforeEach
    void setUp() {
        projectionService = new InstallmentProjectionService(
                invoiceRepository, itemShareRepository);

        owner = User.createLocalUser(
                "Owner", "owner@test.com", "pass",
                Set.of(Role.USER));
        ReflectionTestUtils.setField(owner, "id", 1L);

        bank = Bank.of("NU", "Nubank");
        creditCard = CreditCard.of(
                "Card", "1234", new BigDecimal("5000"), owner, bank);
        ReflectionTestUtils.setField(creditCard, "id", 10L);

        category = Category.of("Shopping", "#FF0000");
        ReflectionTestUtils.setField(category, "id", 100L);
    }

    private Invoice createInvoice(Long id, YearMonth month) {
        Invoice inv = Invoice.of(
                creditCard, month, month.atDay(15));
        ReflectionTestUtils.setField(inv, "id", id);
        return inv;
    }

    private InvoiceItem createInstallmentItem(
            Invoice invoice, Long id,
            int current, int total) {
        InvoiceItem item = InvoiceItem.of(
                invoice, "ITEM " + current + "/" + total,
                new BigDecimal("100.00"), category,
                LocalDate.of(2025, 1, 15),
                current, total);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    @Nested
    @DisplayName("projectInstallments")
    class ProjectInstallments {

        @Test
        @DisplayName("Should project remaining installments into future months")
        void shouldProjectRemainingInstallments() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem item = createInstallmentItem(source, 50L, 1, 3);
            source.addItem(item);

            when(invoiceRepository.findByCreditCardAndMonth(
                    eq(creditCard), any(YearMonth.class)))
                    .thenReturn(List.of());
            when(invoiceRepository.save(any(Invoice.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            int count = projectionService.projectInstallments(
                    source, owner);

            assertEquals(2, count);

            verify(invoiceRepository, org.mockito.Mockito.atLeast(1))
                    .save(any(Invoice.class));
        }

        @Test
        @DisplayName("Should skip single-payment items")
        void shouldSkipSinglePaymentItems() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem single = InvoiceItem.of(
                    source, "SINGLE PURCHASE",
                    new BigDecimal("50.00"), category,
                    LocalDate.of(2025, 3, 1));
            ReflectionTestUtils.setField(single, "id", 51L);
            source.addItem(single);

            int count = projectionService.projectInstallments(
                    source, owner);

            assertEquals(0, count);
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip items at last installment")
        void shouldSkipLastInstallment() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem last = createInstallmentItem(
                    source, 52L, 3, 3);
            source.addItem(last);

            int count = projectionService.projectInstallments(
                    source, owner);

            assertEquals(0, count);
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip already projected items")
        void shouldSkipAlreadyProjectedItems() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem projected = InvoiceItem.projected(
                    source, "PROJECTED",
                    new BigDecimal("100.00"), category,
                    LocalDate.of(2025, 1, 15), 2, 3, 99L);
            ReflectionTestUtils.setField(projected, "id", 53L);
            source.addItem(projected);

            int count = projectionService.projectInstallments(
                    source, owner);

            assertEquals(0, count);
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should be idempotent — not duplicate existing projections")
        void shouldBeIdempotent() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem item = createInstallmentItem(
                    source, 50L, 1, 2);
            source.addItem(item);

            Invoice futureInvoice = createInvoice(
                    2L, YearMonth.of(2025, 4));
            InvoiceItem existingProjection = InvoiceItem.projected(
                    futureInvoice, "ITEM 1/2",
                    new BigDecimal("100.00"), category,
                    LocalDate.of(2025, 1, 15), 2, 2, 50L);
            ReflectionTestUtils.setField(existingProjection, "id", 54L);
            futureInvoice.addItem(existingProjection);

            when(invoiceRepository.findByCreditCardAndMonth(
                    creditCard, YearMonth.of(2025, 4)))
                    .thenReturn(List.of(futureInvoice));

            int count = projectionService.projectInstallments(
                    source, owner);

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should copy category to projected items")
        void shouldCopyCategoryToProjectedItems() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem item = createInstallmentItem(
                    source, 50L, 1, 2);
            source.addItem(item);

            Invoice futureInvoice = createInvoice(
                    2L, YearMonth.of(2025, 4));
            when(invoiceRepository.findByCreditCardAndMonth(
                    creditCard, YearMonth.of(2025, 4)))
                    .thenReturn(List.of(futureInvoice));

            projectionService.projectInstallments(source, owner);

            List<InvoiceItem> futureItems = futureInvoice.getItems();
            assertEquals(1, futureItems.size());
            assertEquals(category, futureItems.get(0).getCategory());
            assertTrue(futureItems.get(0).isProjected());
            assertEquals(50L, futureItems.get(0).getSourceItemId());
            assertEquals(2, futureItems.get(0).getInstallments());
            assertEquals(2, futureItems.get(0).getTotalInstallments());
        }

        @Test
        @DisplayName("Should copy user shares to projected items")
        void shouldCopyUserShares() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem item = createInstallmentItem(
                    source, 50L, 1, 2);
            source.addItem(item);

            User sharedUser = User.createLocalUser(
                    "Shared", "shared@test.com", "pass",
                    Set.of(Role.USER));
            ReflectionTestUtils.setField(sharedUser, "id", 2L);

            ItemShare share = ItemShare.of(
                    sharedUser, item,
                    new BigDecimal("0.5"), new BigDecimal("50.00"),
                    false);
            item.addShare(share);

            Invoice futureInvoice = createInvoice(
                    2L, YearMonth.of(2025, 4));
            when(invoiceRepository.findByCreditCardAndMonth(
                    creditCard, YearMonth.of(2025, 4)))
                    .thenReturn(List.of(futureInvoice));
            when(itemShareRepository.save(any(ItemShare.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            projectionService.projectInstallments(source, owner);

            List<InvoiceItem> futureItems = futureInvoice.getItems();
            assertEquals(1, futureItems.size());
            List<ItemShare> copiedShares = futureItems.get(0).getShares();
            assertEquals(1, copiedShares.size());
            assertEquals(sharedUser, copiedShares.get(0).getUser());
            assertEquals(
                    new BigDecimal("0.5"),
                    copiedShares.get(0).getPercentage());
        }

        @Test
        @DisplayName("Should copy contact shares to projected items")
        void shouldCopyContactShares() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));
            InvoiceItem item = createInstallmentItem(
                    source, 50L, 1, 2);
            source.addItem(item);

            TrustedContact contact = TrustedContact.create(
                    owner, "Contact", "contact@test.com",
                    null, null);
            ReflectionTestUtils.setField(contact, "id", 5L);

            ItemShare contactShare = ItemShare.forContact(
                    contact, item,
                    new BigDecimal("0.5"), new BigDecimal("50.00"),
                    false);
            item.addShare(contactShare);

            Invoice futureInvoice = createInvoice(
                    2L, YearMonth.of(2025, 4));
            when(invoiceRepository.findByCreditCardAndMonth(
                    creditCard, YearMonth.of(2025, 4)))
                    .thenReturn(List.of(futureInvoice));
            when(itemShareRepository.save(any(ItemShare.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            projectionService.projectInstallments(source, owner);

            List<InvoiceItem> futureItems = futureInvoice.getItems();
            List<ItemShare> copiedShares = futureItems.get(0).getShares();
            assertEquals(1, copiedShares.size());
            assertNotNull(copiedShares.get(0).getTrustedContact());
            assertEquals(
                    contact, copiedShares.get(0).getTrustedContact());
        }

        @Test
        @DisplayName("Should project multiple items with different remaining installments")
        void shouldProjectMultipleItems() {
            Invoice source = createInvoice(1L, YearMonth.of(2025, 3));

            InvoiceItem item2of3 = createInstallmentItem(
                    source, 60L, 2, 3);
            source.addItem(item2of3);

            InvoiceItem item1of4 = createInstallmentItem(
                    source, 61L, 1, 4);
            source.addItem(item1of4);

            when(invoiceRepository.findByCreditCardAndMonth(
                    eq(creditCard), any(YearMonth.class)))
                    .thenReturn(List.of());
            when(invoiceRepository.save(any(Invoice.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            int count = projectionService.projectInstallments(
                    source, owner);

            // item2of3: 1 remaining (3/3)
            // item1of4: 3 remaining (2/4, 3/4, 4/4)
            assertEquals(4, count);
        }
    }

    @Nested
    @DisplayName("removeProjectedItems")
    class RemoveProjectedItems {

        @Test
        @DisplayName("Should remove only projected items")
        void shouldRemoveOnlyProjectedItems() {
            Invoice invoice = createInvoice(1L, YearMonth.of(2025, 4));

            InvoiceItem real = InvoiceItem.of(
                    invoice, "REAL ITEM", new BigDecimal("200.00"),
                    category, LocalDate.of(2025, 3, 10), 2, 3);
            ReflectionTestUtils.setField(real, "id", 70L);
            invoice.addItem(real);

            InvoiceItem projected = InvoiceItem.projected(
                    invoice, "PROJECTED ITEM", new BigDecimal("100.00"),
                    category, LocalDate.of(2025, 1, 15), 3, 3, 50L);
            ReflectionTestUtils.setField(projected, "id", 71L);
            invoice.addItem(projected);

            int removed = projectionService.removeProjectedItems(invoice);

            assertEquals(1, removed);
            assertEquals(1, invoice.getItems().size());
            assertFalse(invoice.getItems().get(0).isProjected());
            verify(invoiceRepository).save(invoice);
        }

        @Test
        @DisplayName("Should do nothing when no projected items exist")
        void shouldDoNothingWhenNoProjectedItems() {
            Invoice invoice = createInvoice(1L, YearMonth.of(2025, 4));

            InvoiceItem real = InvoiceItem.of(
                    invoice, "REAL ITEM", new BigDecimal("200.00"),
                    category, LocalDate.of(2025, 3, 10));
            ReflectionTestUtils.setField(real, "id", 70L);
            invoice.addItem(real);

            int removed = projectionService.removeProjectedItems(invoice);

            assertEquals(0, removed);
            verify(invoiceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("InvoiceItem projected factory")
    class InvoiceItemProjectedFactory {

        @Test
        @DisplayName("Should create a projected item with correct fields")
        void shouldCreateProjectedItem() {
            Invoice invoice = createInvoice(1L, YearMonth.of(2025, 4));
            InvoiceItem projected = InvoiceItem.projected(
                    invoice, "TEST PROJECTED",
                    new BigDecimal("150.00"), category,
                    LocalDate.of(2025, 1, 5), 3, 6, 42L);

            assertTrue(projected.isProjected());
            assertEquals(42L, projected.getSourceItemId());
            assertEquals("TEST PROJECTED", projected.getDescription());
            assertEquals(new BigDecimal("150.00"), projected.getAmount());
            assertEquals(category, projected.getCategory());
            assertEquals(3, projected.getInstallments());
            assertEquals(6, projected.getTotalInstallments());
            assertNotNull(projected.getCreatedAt());
        }

        @Test
        @DisplayName("Regular items should not be projected")
        void regularItemsShouldNotBeProjected() {
            Invoice invoice = createInvoice(1L, YearMonth.of(2025, 4));
            InvoiceItem regular = InvoiceItem.of(
                    invoice, "REGULAR",
                    new BigDecimal("50.00"), category,
                    LocalDate.of(2025, 4, 1));

            assertFalse(regular.isProjected());
        }
    }
}
