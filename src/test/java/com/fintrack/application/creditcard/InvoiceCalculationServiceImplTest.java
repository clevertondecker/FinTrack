package com.fintrack.application.creditcard;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.creditcard.ParticipantShare;

/**
 * Unit tests for {@link InvoiceCalculationServiceImpl#calculateOtherParticipantShares(Invoice, User)}.
 *
 * <p>Tests cover contact share filtering by owner, user share filtering by owner exclusion,
 * email-based grouping, and amount summation across multiple items.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceCalculationServiceImpl - calculateOtherParticipantShares")
class InvoiceCalculationServiceImplTest {

    @Mock
    private ExpenseSharingServiceImpl expenseSharingService;

    private InvoiceCalculationServiceImpl service;

    private User owner;
    private User otherUser;
    private Bank bank;
    private CreditCard creditCard;

    @BeforeEach
    void setUp() throws Exception {
        service = new InvoiceCalculationServiceImpl(expenseSharingService);

        owner = User.createLocalUser("Alice Owner", "alice@example.com", "password123", Set.of(Role.USER));
        setId(owner, User.class, 1L);

        otherUser = User.createLocalUser("Bob Other", "bob@example.com", "password456", Set.of(Role.USER));
        setId(otherUser, User.class, 2L);

        bank = Bank.of("NU", "Nubank");
        creditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), owner, bank);
    }

    // ---- Helper methods ----

    private static void setId(Object entity, Class<?> clazz, Long id) throws Exception {
        java.lang.reflect.Field idField = clazz.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private Invoice createInvoice() {
        return Invoice.of(creditCard, YearMonth.of(2024, 11), LocalDate.of(2024, 11, 10));
    }

    private InvoiceItem createItem(Invoice invoice, String description, BigDecimal amount) {
        return InvoiceItem.of(invoice, description, amount, null, LocalDate.of(2024, 10, 15));
    }

    private TrustedContact createContact(User contactOwner, String name, String email) throws Exception {
        TrustedContact contact = TrustedContact.create(contactOwner, name, email, null, null);
        setId(contact, TrustedContact.class, null);
        return contact;
    }

    // ---- Test classes ----

    @Nested
    @DisplayName("Empty / no-share scenarios")
    class EmptyScenarios {

        @Test
        @DisplayName("Should return empty list when invoice has no items")
        void shouldReturnEmptyListWhenInvoiceHasNoItems() {
            // Arrange
            Invoice invoice = createInvoice();

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when items have no shares")
        void shouldReturnEmptyListWhenItemsHaveNoShares() {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item = createItem(invoice, "Groceries", new BigDecimal("100.00"));
            invoice.addItem(item);

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Trusted contact share filtering")
    class TrustedContactShareFiltering {

        @Test
        @DisplayName("Should include trusted contact shares owned by the user")
        void shouldIncludeTrustedContactSharesOwnedByTheUser() throws Exception {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item = createItem(invoice, "Dinner", new BigDecimal("200.00"));
            invoice.addItem(item);

            TrustedContact contact = createContact(owner, "Charlie Contact", "charlie@example.com");
            setId(contact, TrustedContact.class, 10L);

            ItemShare contactShare = ItemShare.forContact(
                    contact, item, new BigDecimal("0.50"), new BigDecimal("100.00"), false);
            item.addShare(contactShare);

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).hasSize(1);
            ParticipantShare summary = result.get(0);
            assertThat(summary.name()).isEqualTo("Charlie Contact");
            assertThat(summary.email()).isEqualTo("charlie@example.com");
            assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should exclude trusted contact shares not owned by the user")
        void shouldExcludeTrustedContactSharesNotOwnedByTheUser() throws Exception {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item = createItem(invoice, "Dinner", new BigDecimal("200.00"));
            invoice.addItem(item);

            // Contact owned by a different user (otherUser), not the owner
            TrustedContact contactOwnedByOther = createContact(otherUser, "Dave Contact", "dave@example.com");
            setId(contactOwnedByOther, TrustedContact.class, 11L);

            ItemShare contactShare = ItemShare.forContact(
                    contactOwnedByOther, item, new BigDecimal("0.50"), new BigDecimal("100.00"), false);
            item.addShare(contactShare);

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("User share filtering")
    class UserShareFiltering {

        @Test
        @DisplayName("Should include other user shares when user_id differs from owner")
        void shouldIncludeOtherUserSharesWhenUserIdDiffersFromOwner() {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item = createItem(invoice, "Shared Lunch", new BigDecimal("120.00"));
            invoice.addItem(item);

            ItemShare userShare = ItemShare.of(
                    otherUser, item, new BigDecimal("0.50"), new BigDecimal("60.00"), false);
            item.addShare(userShare);

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).hasSize(1);
            ParticipantShare summary = result.get(0);
            assertThat(summary.name()).isEqualTo("Bob Other");
            assertThat(summary.email()).isEqualTo("bob@example.com");
            assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
        }

        @Test
        @DisplayName("Should exclude owner's own user shares")
        void shouldExcludeOwnersOwnUserShares() {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item = createItem(invoice, "Personal Item", new BigDecimal("100.00"));
            invoice.addItem(item);

            // Owner's own share -- should be excluded
            ItemShare ownerShare = ItemShare.of(
                    owner, item, new BigDecimal("0.50"), new BigDecimal("50.00"), true);
            item.addShare(ownerShare);

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Email-based grouping")
    class EmailBasedGrouping {

        @Test
        @DisplayName("Should group by email when same person is both User and TrustedContact")
        void shouldGroupByEmailWhenSamePersonIsBothUserAndTrustedContact() throws Exception {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item1 = createItem(invoice, "Item A", new BigDecimal("200.00"));
            invoice.addItem(item1);
            InvoiceItem item2 = createItem(invoice, "Item B", new BigDecimal("200.00"));
            invoice.addItem(item2);

            // Bob as a system user share on item1
            ItemShare userShare = ItemShare.of(
                    otherUser, item1, new BigDecimal("0.50"), new BigDecimal("100.00"), false);
            item1.addShare(userShare);

            // Bob as a trusted contact (same email) on item2
            TrustedContact bobContact = createContact(owner, "Bob Contact", "bob@example.com");
            setId(bobContact, TrustedContact.class, 20L);
            ItemShare contactShare = ItemShare.forContact(
                    bobContact, item2, new BigDecimal("0.25"), new BigDecimal("50.00"), false);
            item2.addShare(contactShare);

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert -- same email should be grouped into a single entry
            assertThat(result).hasSize(1);
            ParticipantShare summary = result.get(0);
            assertThat(summary.email()).isEqualTo("bob@example.com");
            assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should keep different email participants as separate entries")
        void shouldKeepDifferentEmailParticipantsAsSeparateEntries() throws Exception {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item = createItem(invoice, "Shared Dinner", new BigDecimal("300.00"));
            invoice.addItem(item);

            // otherUser share
            ItemShare userShare = ItemShare.of(
                    otherUser, item, new BigDecimal("0.33"), new BigDecimal("100.00"), false);
            item.addShare(userShare);

            // trusted contact with different email
            TrustedContact contact = createContact(owner, "Eve Contact", "eve@example.com");
            setId(contact, TrustedContact.class, 30L);
            ItemShare contactShare = ItemShare.forContact(
                    contact, item, new BigDecimal("0.33"), new BigDecimal("100.00"), false);
            item.addShare(contactShare);

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(ParticipantShare::email)
                    .containsExactlyInAnyOrder("bob@example.com", "eve@example.com");
        }
    }

    @Nested
    @DisplayName("Amount summation across multiple items")
    class AmountSummation {

        @Test
        @DisplayName("Should sum amounts across multiple items for same participant")
        void shouldSumAmountsAcrossMultipleItemsForSameParticipant() {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item1 = createItem(invoice, "Item 1", new BigDecimal("200.00"));
            invoice.addItem(item1);
            InvoiceItem item2 = createItem(invoice, "Item 2", new BigDecimal("300.00"));
            invoice.addItem(item2);
            InvoiceItem item3 = createItem(invoice, "Item 3", new BigDecimal("100.00"));
            invoice.addItem(item3);

            // otherUser shares across all three items
            item1.addShare(ItemShare.of(otherUser, item1, new BigDecimal("0.50"), new BigDecimal("100.00"), false));
            item2.addShare(ItemShare.of(otherUser, item2, new BigDecimal("0.50"), new BigDecimal("150.00"), false));
            item3.addShare(ItemShare.of(otherUser, item3, new BigDecimal("0.50"), new BigDecimal("50.00"), false));

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).hasSize(1);
            ParticipantShare summary = result.get(0);
            assertThat(summary.email()).isEqualTo("bob@example.com");
            assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Should sum contact share amounts across multiple items for same contact")
        void shouldSumContactShareAmountsAcrossMultipleItemsForSameContact() throws Exception {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item1 = createItem(invoice, "Item 1", new BigDecimal("200.00"));
            invoice.addItem(item1);
            InvoiceItem item2 = createItem(invoice, "Item 2", new BigDecimal("400.00"));
            invoice.addItem(item2);

            TrustedContact contact = createContact(owner, "Charlie Contact", "charlie@example.com");
            setId(contact, TrustedContact.class, 10L);

            item1.addShare(ItemShare.forContact(
                    contact, item1, new BigDecimal("0.50"), new BigDecimal("100.00"), false));
            item2.addShare(ItemShare.forContact(
                    contact, item2, new BigDecimal("0.25"), new BigDecimal("100.00"), false));

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert
            assertThat(result).hasSize(1);
            ParticipantShare summary = result.get(0);
            assertThat(summary.name()).isEqualTo("Charlie Contact");
            assertThat(summary.email()).isEqualTo("charlie@example.com");
            assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        }
    }

    @Nested
    @DisplayName("Mixed scenarios")
    class MixedScenarios {

        @Test
        @DisplayName("Should handle mix of contact shares, user shares, and owner shares correctly")
        void shouldHandleMixOfShareTypesCorrectly() throws Exception {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item = createItem(invoice, "Big Dinner", new BigDecimal("400.00"));
            invoice.addItem(item);

            // Owner's own share (should be excluded)
            item.addShare(ItemShare.of(owner, item, new BigDecimal("0.25"), new BigDecimal("100.00"), true));

            // Other user's share (should be included)
            item.addShare(ItemShare.of(otherUser, item, new BigDecimal("0.25"), new BigDecimal("100.00"), false));

            // Contact owned by owner (should be included)
            TrustedContact ownedContact = createContact(owner, "Eve", "eve@example.com");
            setId(ownedContact, TrustedContact.class, 40L);
            item.addShare(ItemShare.forContact(
                    ownedContact, item, new BigDecimal("0.25"), new BigDecimal("100.00"), false));

            // Contact owned by otherUser (should be excluded)
            TrustedContact foreignContact = createContact(otherUser, "Frank", "frank@example.com");
            setId(foreignContact, TrustedContact.class, 41L);
            item.addShare(ItemShare.forContact(
                    foreignContact, item, new BigDecimal("0.25"), new BigDecimal("100.00"), false));

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert -- only Bob (other user) and Eve (owner's contact) should be included
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(ParticipantShare::email)
                    .containsExactlyInAnyOrder("bob@example.com", "eve@example.com");
            assertThat(result).allSatisfy(s ->
                    assertThat(s.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Should use first encountered name when grouping by email")
        void shouldUseFirstEncounteredNameWhenGroupingByEmail() throws Exception {
            // Arrange
            Invoice invoice = createInvoice();
            InvoiceItem item1 = createItem(invoice, "Item 1", new BigDecimal("200.00"));
            invoice.addItem(item1);
            InvoiceItem item2 = createItem(invoice, "Item 2", new BigDecimal("200.00"));
            invoice.addItem(item2);

            // First encounter: otherUser (name "Bob Other") on item1
            item1.addShare(ItemShare.of(otherUser, item1, new BigDecimal("0.50"), new BigDecimal("100.00"), false));

            // Second encounter: contact with same email but different display name on item2
            TrustedContact bobContact = createContact(owner, "Bobby", "bob@example.com");
            setId(bobContact, TrustedContact.class, 50L);
            item2.addShare(ItemShare.forContact(
                    bobContact, item2, new BigDecimal("0.50"), new BigDecimal("100.00"), false));

            // Act
            List<ParticipantShare> result = service.calculateOtherParticipantShares(invoice, owner);

            // Assert -- grouped by email, name is from the first encounter (putIfAbsent)
            assertThat(result).hasSize(1);
            ParticipantShare summary = result.get(0);
            assertThat(summary.email()).isEqualTo("bob@example.com");
            assertThat(summary.name()).isEqualTo("Bob Other");
            assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        }
    }
}
