package com.fintrack.infrastructure.parsing;

import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedInvoiceItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PdfInvoiceParser line-level parsing logic.
 * Covers dual-currency (USD) transactions, standard items, parceled items, and edge cases.
 */
class PdfInvoiceParserLineParsingTest {

    private PdfInvoiceParser parser;

    @BeforeEach
    void setUp() {
        parser = new PdfInvoiceParser();
    }

    // --- Dual-currency (USD) transactions ---

    @Test
    void parseLine_withUsdTransaction_shouldUseBrlAmountNotUsd() {
        // UDEMY: BRL 346,86 comes before USD 65,47
        String text = invoiceWith("03/05 UDEMY SUBSCRIPTION 346,86 65,47");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).amount()).isEqualByComparingTo("346.86");
        assertThat(items.get(0).description()).doesNotContain("65,47");
    }

    @Test
    void parseLine_withUsdTransaction_descriptionShouldNotContainBrlAmount() {
        String text = invoiceWith("04/05 OPENAI *CHATGPT SUBSCR 42,78 8,09");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).amount()).isEqualByComparingTo("42.78");
        assertThat(items.get(0).description())
                .isEqualTo("OPENAI *CHATGPT SUBSCR")
                .doesNotContain("42,78")
                .doesNotContain("8,09");
    }

    @Test
    void parseLine_withMultipleUsdTransactions_shouldParseBothCorrectly() {
        String text = invoiceWith("""
                03/05 UDEMY SUBSCRIPTION 346,86 65,47
                04/05 OPENAI *CHATGPT SUBSCR 42,78 8,09
                """);

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(2);
        assertThat(items.get(0).amount()).isEqualByComparingTo("346.86");
        assertThat(items.get(1).amount()).isEqualByComparingTo("42.78");
    }

    // --- Standard domestic transactions not affected ---

    @Test
    void parseLine_withSingleAmountItem_shouldParseNormally() {
        String text = invoiceWith("05/05 NETFLIX.COM 57,80");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).amount()).isEqualByComparingTo("57.80");
        assertThat(items.get(0).description()).isEqualTo("NETFLIX.COM");
    }

    @Test
    void parseLine_withLargeAmountItem_shouldParseNormally() {
        String text = invoiceWith("01/05 MP *PICHAUINFORMATICA 6035,28");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).amount()).isEqualByComparingTo("6035.28");
    }

    @Test
    void parseLine_withNegativeAmount_shouldParseNormally() {
        String text = invoiceWith("05/04 MP*MERCADOLIVRE -0,07");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).amount()).isEqualByComparingTo("-0.07");
    }

    // --- Parceled items not affected ---

    @Test
    void parseLine_withParceledItem_shouldNotBeTreatedAsDualCurrency() {
        // "05/06" is installment notation (5th of 6), not a dual amount
        String text = invoiceWith("15/01 PG *GENERA GENERA 05/06 74,41");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).amount()).isEqualByComparingTo("74.41");
        assertThat(items.get(0).installments()).isEqualTo(5);
        assertThat(items.get(0).totalInstallments()).isEqualTo(6);
    }

    @Test
    void parseLine_withParceledItem12x_shouldNotBeTreatedAsDualCurrency() {
        String text = invoiceWith("28/01 YOU GYM GUARAMIRIM 04/12 159,90");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).amount()).isEqualByComparingTo("159.90");
        assertThat(items.get(0).installments()).isEqualTo(4);
        assertThat(items.get(0).totalInstallments()).isEqualTo(12);
    }

    // --- IOF lines not affected ---

    @Test
    void parseLine_withIofLine_shouldUseIofParser() {
        String text = invoiceWith("IOF DESPESA NO EXTERIOR 12,14");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).description()).isEqualTo("IOF DESPESA NO EXTERIOR");
        assertThat(items.get(0).amount()).isEqualByComparingTo("12.14");
    }

    // --- Edge cases for dual-currency guard (brlAmount > usdAmount) ---

    @Test
    void parseLine_withTwoAmountsWhereSecondIsLarger_shouldNotTreatAsDualCurrency() {
        // If second number is larger, it's not a USD transaction — fall through to standard
        // Standard parser will capture last number (300,00) as amount
        String text = invoiceWith("03/05 SOME PRODUCT 50,00 300,00");

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(1);
        // Standard parser captures last number
        assertThat(items.get(0).amount()).isEqualByComparingTo("300.00");
    }

    // --- Mixed invoice with both USD and domestic items ---

    @Test
    void parseInvoice_withMixedDomesticAndUsdItems_shouldParseAllCorrectly() {
        String text = invoiceWith("""
                05/05 NETFLIX.COM 57,80
                03/05 UDEMY SUBSCRIPTION 346,86 65,47
                IOF DESPESA NO EXTERIOR 12,14
                01/05 AWS BRAZIL 6,00
                04/05 OPENAI *CHATGPT SUBSCR 42,78 8,09
                IOF DESPESA NO EXTERIOR 1,50
                """);

        List<ParsedInvoiceItem> items = parser.extractInvoiceData(text).items();

        assertThat(items).hasSize(6);
        assertThat(items).anySatisfy(i -> {
            assertThat(i.description()).isEqualTo("UDEMY SUBSCRIPTION");
            assertThat(i.amount()).isEqualByComparingTo("346.86");
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.description()).isEqualTo("OPENAI *CHATGPT SUBSCR");
            assertThat(i.amount()).isEqualByComparingTo("42.78");
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.description()).isEqualTo("NETFLIX.COM");
            assertThat(i.amount()).isEqualByComparingTo("57.80");
        });
    }

    private String invoiceWith(String items) {
        return """
                Vencimento: 05/06/2026
                Total: R$ 100,00
                """ + items;
    }
}
