package com.fintrack.application.creditcard;

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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ExpenseSharingServiceImpl.class)
@ActiveProfiles("test")
@DisplayName("ExpenseSharingServiceImpl Integration Test")
class ExpenseSharingServiceImplTest {

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
        // Arrange
        var shares = List.of(
                new CreateItemShareRequest.UserShare(user1.getId(), new BigDecimal("0.7"), true),
                new CreateItemShareRequest.UserShare(user2.getId(), new BigDecimal("0.3"), false)
        );
        // Act
        expenseSharingService.createSharesFromUserIds(invoiceItem, shares);
        List<ItemShare> result = expenseSharingService.getSharesForItem(invoiceItem);
        // Assert
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
}