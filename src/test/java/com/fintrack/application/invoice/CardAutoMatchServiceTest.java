package com.fintrack.application.invoice;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.DetectedCardMapping;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedCardSection;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedInvoiceItem;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardAutoMatchServiceTest {

    @Mock
    private CreditCardJpaRepository creditCardRepository;

    @InjectMocks
    private CardAutoMatchService service;

    private User user;
    private Bank bank;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user = User.createLocalUser("Test", "test@example.com", "password", roles);
        bank = Bank.of("001", "Test Bank");
    }

    @Test
    void buildDetectedCardMappings_singleMatch_shouldAutoMatch() {
        CreditCard card = CreditCard.of("My Card", "1234", BigDecimal.valueOf(5000), user, bank);
        ReflectionTestUtils.setField(card, "id", 1L);

        ParsedInvoiceData data = new ParsedInvoiceData(
                null, "1234", LocalDate.now(), BigDecimal.TEN,
                List.of(item("UBER", "50.00")),
                null, YearMonth.now(), 0.9,
                List.of(new ParsedCardSection("1234", "CARTÃO 1234",
                        List.of(item("UBER", "50.00")), BigDecimal.TEN)));

        when(creditCardRepository.findByOwnerAndLastFourDigitsAndActiveTrue(user, "1234"))
                .thenReturn(List.of(card));

        List<DetectedCardMapping> result = service.buildDetectedCardMappings(data, user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).autoMatched()).isTrue();
        assertThat(result.get(0).matchedCreditCardId()).isEqualTo(1L);
        assertThat(result.get(0).ambiguous()).isFalse();
    }

    @Test
    void buildDetectedCardMappings_noMatch_shouldFlagUnmatched() {
        ParsedInvoiceData data = new ParsedInvoiceData(
                null, "9999", LocalDate.now(), BigDecimal.TEN,
                List.of(item("UBER", "50.00")),
                null, YearMonth.now(), 0.9,
                List.of(new ParsedCardSection("9999", "CARTÃO 9999",
                        List.of(item("UBER", "50.00")), BigDecimal.TEN)));

        when(creditCardRepository.findByOwnerAndLastFourDigitsAndActiveTrue(user, "9999"))
                .thenReturn(List.of());

        List<DetectedCardMapping> result = service.buildDetectedCardMappings(data, user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).autoMatched()).isFalse();
        assertThat(result.get(0).ambiguous()).isFalse();
        assertThat(result.get(0).matchedCreditCardId()).isNull();
    }

    @Test
    void buildDetectedCardMappings_disambiguateByHierarchy_firstSectionGetsParent() {
        CreditCard parent = CreditCard.of("Parent Card", "1234", BigDecimal.valueOf(10000), user, bank);
        ReflectionTestUtils.setField(parent, "id", 1L);

        CreditCard child = CreditCard.of("Child Card", "1234", BigDecimal.valueOf(3000), user, bank);
        ReflectionTestUtils.setField(child, "id", 2L);
        ReflectionTestUtils.setField(child, "parentCard", parent);

        ParsedInvoiceData data = new ParsedInvoiceData(
                null, null, LocalDate.now(), BigDecimal.valueOf(150),
                List.of(),
                null, YearMonth.now(), 0.9,
                List.of(
                        new ParsedCardSection("1234", "CARTÃO 1234 TITULAR",
                                List.of(item("UBER", "100.00")), new BigDecimal("100.00")),
                        new ParsedCardSection("1234", "CARTÃO 1234 ADICIONAL",
                                List.of(item("NETFLIX", "50.00")), new BigDecimal("50.00"))
                ));

        when(creditCardRepository.findByOwnerAndLastFourDigitsAndActiveTrue(user, "1234"))
                .thenReturn(List.of(parent, child));

        List<DetectedCardMapping> result = service.buildDetectedCardMappings(data, user);

        // First section -> parent card
        assertThat(result.get(0).matchedCreditCardId()).isEqualTo(1L);
        assertThat(result.get(0).autoMatched()).isTrue();

        // Second section -> child card
        assertThat(result.get(1).matchedCreditCardId()).isEqualTo(2L);
        assertThat(result.get(1).autoMatched()).isTrue();
    }

    @Test
    void buildDetectedCardMappings_ambiguousWithoutHierarchy_shouldFlagAmbiguous() {
        CreditCard card1 = CreditCard.of("Card A", "1234", BigDecimal.valueOf(5000), user, bank);
        ReflectionTestUtils.setField(card1, "id", 1L);
        CreditCard card2 = CreditCard.of("Card B", "1234", BigDecimal.valueOf(5000), user, bank);
        ReflectionTestUtils.setField(card2, "id", 2L);

        ParsedInvoiceData data = new ParsedInvoiceData(
                null, null, LocalDate.now(), BigDecimal.TEN,
                List.of(),
                null, YearMonth.now(), 0.9,
                List.of(new ParsedCardSection("1234", "CARTÃO 1234",
                        List.of(item("UBER", "10.00")), BigDecimal.TEN),
                        new ParsedCardSection("1234", "CARTÃO 1234 OUTRO",
                                List.of(item("NETFLIX", "10.00")), BigDecimal.TEN)));

        when(creditCardRepository.findByOwnerAndLastFourDigitsAndActiveTrue(user, "1234"))
                .thenReturn(List.of(card1, card2));

        List<DetectedCardMapping> result = service.buildDetectedCardMappings(data, user);

        // First section: both are parents -> cannot disambiguate first by hierarchy IF there's more than one parent
        // Actually since both have no parentCard, they're both "parents", so first should match if only 1 parent.
        // But here we have 2 parents and isFirstSection=true, parents.size() == 2, so no match.
        assertThat(result.get(0).ambiguous()).isTrue();
        assertThat(result.get(0).matchedCreditCardId()).isNull();
        assertThat(result.get(0).candidateCardIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void buildItemsByCardMap_withSections_shouldGroupItems() {
        ParsedInvoiceData data = new ParsedInvoiceData(
                null, null, LocalDate.now(), BigDecimal.valueOf(100),
                List.of(),
                null, YearMonth.now(), 0.9,
                List.of(
                        new ParsedCardSection("1234", "A",
                                List.of(item("UBER", "50.00")), new BigDecimal("50.00")),
                        new ParsedCardSection("5678", "B",
                                List.of(item("AMAZON", "50.00")), new BigDecimal("50.00"))
                ));

        Map<String, List<ParsedInvoiceItem>> result = service.buildItemsByCardMap(data);

        assertThat(result).hasSize(2);
        assertThat(result.get("1234")).hasSize(1);
        assertThat(result.get("5678")).hasSize(1);
    }

    @Test
    void buildDetectedCardMappings_noSectionsWithCardNumber_shouldMatchAsSingle() {
        CreditCard card = CreditCard.of("Card", "1234", BigDecimal.valueOf(5000), user, bank);
        ReflectionTestUtils.setField(card, "id", 1L);

        ParsedInvoiceData data = new ParsedInvoiceData(
                "Card", "1234", LocalDate.now(), BigDecimal.TEN,
                List.of(item("UBER", "10.00")),
                null, YearMonth.now(), 0.9);

        when(creditCardRepository.findByOwnerAndLastFourDigitsAndActiveTrue(user, "1234"))
                .thenReturn(List.of(card));

        List<DetectedCardMapping> result = service.buildDetectedCardMappings(data, user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).autoMatched()).isTrue();
        assertThat(result.get(0).matchedCreditCardId()).isEqualTo(1L);
    }

    private ParsedInvoiceItem item(String desc, String amount) {
        return new ParsedInvoiceItem(desc, new BigDecimal(amount), LocalDate.now(), null, 1, 1, 0.9);
    }
}
