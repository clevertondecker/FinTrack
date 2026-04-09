package com.fintrack.application.creditcard;

import com.fintrack.application.contact.TrustedContactService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.dto.creditcard.CreateItemShareRequest;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceItemJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import com.fintrack.domain.creditcard.ItemShareRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
@AutoConfigurationPackage(basePackageClasses = ItemShareRepository.class)
@EntityScan(basePackages = {
    "com.fintrack.domain.creditcard",
    "com.fintrack.domain.user",
    "com.fintrack.domain.contact"
})
@EnableJpaRepositories(basePackages = {
    "com.fintrack.domain.creditcard",
    "com.fintrack.infrastructure.persistence.creditcard",
    "com.fintrack.infrastructure.persistence.user",
    "com.fintrack.infrastructure.persistence.contact"
})
@Import({ ExpenseSharingServiceImpl.class, ExpenseSharingServiceImplTest.TestConfig.class })
@ActiveProfiles("test")
@DisplayName("ExpenseSharingServiceImpl Integration Test")
class ExpenseSharingServiceImplTest {

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @Bean
        TrustedContactService trustedContactService() {
            return mock(TrustedContactService.class);
        }
    }

    @Autowired
    private ExpenseSharingServiceImpl expenseSharingService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InvoiceItemJpaRepository invoiceItemRepository;
    @Autowired
    private CreditCardJpaRepository creditCardRepository;
    @Autowired
    private BankJpaRepository bankRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;

    private User user1;
    private User user2;
    private InvoiceItem invoiceItem;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.createLocalUser("User 1", "user1@example.com", "password", Set.of(Role.USER)));
        user2 = userRepository.save(User.createLocalUser("User 2", "user2@example.com", "password", Set.of(Role.USER)));

        Bank bank =
          bankRepository.save(Bank.of("NU", "Nubank"));
        CreditCard card =
          creditCardRepository.save(CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), user1, bank));

        Invoice invoice = Invoice.of(card, YearMonth.now(), LocalDate.now());
        invoice = invoiceRepository.save(invoice);
        invoiceItem = invoiceItemRepository.save(InvoiceItem.of(invoice, "Test Item",
            new BigDecimal("100.00"), null, LocalDate.now()));
    }

    @Test
    @DisplayName("Should persist and return shares correctly")
    void shouldPersistAndReturnSharesCorrectly() {
        var shares = List.of(
                new CreateItemShareRequest.UserShare(user1.getId(), null, new BigDecimal("0.7"), true),
                new CreateItemShareRequest.UserShare(user2.getId(), null, new BigDecimal("0.3"), false)
        );
        expenseSharingService.createSharesFromUserIds(invoiceItem, shares);
        List<ItemShare> result = expenseSharingService.getSharesForItem(invoiceItem);
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(share -> {
            if (share.getUser().getId().equals(user1.getId())) {
                assertThat(share.getPercentage()).isEqualByComparingTo(new BigDecimal("0.7"));
                assertThat(share.isResponsible()).isTrue();
            }
            if (share.getUser().getId().equals(user2.getId())) {
                assertThat(share.getPercentage()).isEqualByComparingTo(new BigDecimal("0.3"));
                assertThat(share.isResponsible()).isFalse();
            }
        });
    }

    private record ProjectedItemSet(InvoiceItem source, InvoiceItem projected1, InvoiceItem projected2) {}

    private ProjectedItemSet createInstallmentWithProjections() {
        Bank bank = bankRepository.findAll().get(0);
        CreditCard card = creditCardRepository.save(
                CreditCard.of("Propagation Card", "5678",
                        new BigDecimal("10000.00"), user1, bank));

        Invoice currentInvoice = invoiceRepository.save(
                Invoice.of(card, YearMonth.of(2026, 4), LocalDate.of(2026, 4, 15)));
        Invoice nextInvoice = invoiceRepository.save(
                Invoice.of(card, YearMonth.of(2026, 5), LocalDate.of(2026, 5, 15)));
        Invoice thirdInvoice = invoiceRepository.save(
                Invoice.of(card, YearMonth.of(2026, 6), LocalDate.of(2026, 6, 15)));

        InvoiceItem source = invoiceItemRepository.save(InvoiceItem.of(
                currentInvoice, "Hotel Booking", new BigDecimal("300.00"),
                null, LocalDate.of(2026, 3, 20), 1, 3));

        InvoiceItem proj1 = invoiceItemRepository.save(InvoiceItem.projected(
                nextInvoice, "Hotel Booking", new BigDecimal("300.00"),
                null, LocalDate.of(2026, 3, 20), 2, 3,
                source.getId()));

        InvoiceItem proj2 = invoiceItemRepository.save(InvoiceItem.projected(
                thirdInvoice, "Hotel Booking", new BigDecimal("300.00"),
                null, LocalDate.of(2026, 3, 20), 3, 3,
                source.getId()));

        return new ProjectedItemSet(source, proj1, proj2);
    }

    private List<CreateItemShareRequest.UserShare> fiftyFiftyUserSplit() {
        return List.of(
                new CreateItemShareRequest.UserShare(user1.getId(), null, new BigDecimal("0.5"), true),
                new CreateItemShareRequest.UserShare(user2.getId(), null, new BigDecimal("0.5"), false));
    }

    @Test
    @DisplayName("Creating shares on installment item propagates to all projected items")
    void shouldPropagateSharesOnCreation() {
        var items = createInstallmentWithProjections();

        expenseSharingService.createSharesFromRequest(items.source(), fiftyFiftyUserSplit(), user1);

        List<ItemShare> proj1Shares = expenseSharingService.getSharesForItem(items.projected1());
        List<ItemShare> proj2Shares = expenseSharingService.getSharesForItem(items.projected2());

        assertThat(proj1Shares).hasSize(2);
        assertThat(proj2Shares).hasSize(2);

        for (List<ItemShare> projShares : List.of(proj1Shares, proj2Shares)) {
            assertThat(projShares).anySatisfy(s -> {
                assertThat(s.getUser().getId()).isEqualTo(user1.getId());
                assertThat(s.getPercentage()).isEqualByComparingTo(new BigDecimal("0.5"));
                assertThat(s.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            });
            assertThat(projShares).anySatisfy(s -> {
                assertThat(s.getUser().getId()).isEqualTo(user2.getId());
                assertThat(s.getPercentage()).isEqualByComparingTo(new BigDecimal("0.5"));
                assertThat(s.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            });
        }
    }

    @Test
    @DisplayName("Removing shares from installment item clears shares on projected items")
    void shouldClearProjectedSharesOnRemoval() {
        var items = createInstallmentWithProjections();

        expenseSharingService.createSharesFromRequest(items.source(), fiftyFiftyUserSplit(), user1);
        assertThat(expenseSharingService.getSharesForItem(items.projected1())).hasSize(2);

        expenseSharingService.removeShares(items.source());
        expenseSharingService.propagateSharesToProjections(items.source());

        assertThat(expenseSharingService.getSharesForItem(items.projected1())).isEmpty();
        assertThat(expenseSharingService.getSharesForItem(items.projected2())).isEmpty();
    }

    @Test
    @DisplayName("Non-installment items do not trigger propagation")
    void shouldNotPropagateForNonInstallmentItems() {
        var shares = List.of(
                new CreateItemShareRequest.UserShare(
                        user1.getId(), null, new BigDecimal("1.0"), true));
        expenseSharingService.createSharesFromRequest(invoiceItem, shares, user1);

        assertThat(expenseSharingService.getSharesForItem(invoiceItem)).hasSize(1);
    }

    @Test
    @DisplayName("Installment items with no projections do not error")
    void shouldNotErrorWhenNoProjectionsExist() {
        Bank bank = bankRepository.findAll().get(0);
        CreditCard card = creditCardRepository.save(
                CreditCard.of("Itau Card", "7777",
                        new BigDecimal("8000.00"), user1, bank));
        Invoice inv = invoiceRepository.save(
                Invoice.of(card, YearMonth.of(2026, 7), LocalDate.of(2026, 7, 15)));
        InvoiceItem orphanInstallment = invoiceItemRepository.save(InvoiceItem.of(
                inv, "Appliance", new BigDecimal("200.00"),
                null, LocalDate.of(2026, 3, 10), 1, 6));

        var shares = List.of(
                new CreateItemShareRequest.UserShare(
                        user1.getId(), null, new BigDecimal("0.6"), true),
                new CreateItemShareRequest.UserShare(
                        user2.getId(), null, new BigDecimal("0.4"), false));

        expenseSharingService.createSharesFromRequest(orphanInstallment, shares, user1);

        List<ItemShare> result = expenseSharingService.getSharesForItem(orphanInstallment);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Updating shares on source replaces shares on projected items")
    void shouldReplaceProjectedSharesOnUpdate() {
        var items = createInstallmentWithProjections();

        expenseSharingService.createSharesFromRequest(items.source(), fiftyFiftyUserSplit(), user1);

        var updatedShares = List.of(
                new CreateItemShareRequest.UserShare(
                        user1.getId(), null, new BigDecimal("0.7"), true),
                new CreateItemShareRequest.UserShare(
                        user2.getId(), null, new BigDecimal("0.3"), false));
        expenseSharingService.createSharesFromRequest(items.source(), updatedShares, user1);

        List<ItemShare> proj1Shares = expenseSharingService.getSharesForItem(items.projected1());
        assertThat(proj1Shares).hasSize(2);
        assertThat(proj1Shares).anySatisfy(s -> {
            assertThat(s.getUser().getId()).isEqualTo(user1.getId());
            assertThat(s.getPercentage()).isEqualByComparingTo(new BigDecimal("0.7"));
            assertThat(s.getAmount()).isEqualByComparingTo(new BigDecimal("210.00"));
        });
        assertThat(proj1Shares).anySatisfy(s -> {
            assertThat(s.getUser().getId()).isEqualTo(user2.getId());
            assertThat(s.getPercentage()).isEqualByComparingTo(new BigDecimal("0.3"));
            assertThat(s.getAmount()).isEqualByComparingTo(new BigDecimal("90.00"));
        });
    }

    @Test
    @DisplayName("Projected shares start unpaid regardless of source payment status")
    void shouldNotPropagatePaymentStatus() {
        var items = createInstallmentWithProjections();

        var shares = List.of(
                new CreateItemShareRequest.UserShare(
                        user1.getId(), null, new BigDecimal("1.0"), true));
        expenseSharingService.createSharesFromRequest(items.source(), shares, user1);

        List<ItemShare> proj1Shares = expenseSharingService.getSharesForItem(items.projected1());
        assertThat(proj1Shares).hasSize(1);
        assertThat(proj1Shares.get(0).isPaid()).isFalse();
    }
}