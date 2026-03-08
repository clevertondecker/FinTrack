package com.fintrack.infrastructure.parsing;

import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedCardSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PdfInvoiceParser card section detection.
 */
class PdfInvoiceParserCardSectionTest {

    private PdfInvoiceParser parser;

    @BeforeEach
    void setUp() {
        parser = new PdfInvoiceParser();
    }

    @Test
    void extractInvoiceData_withMultipleCardSections_shouldGroupItemsByCard() {
        String text = """
                Banco: Santander
                Vencimento: 10/11/2024
                Total: R$ 200,00
                
                CARTÃO FINAL 1234
                05/10 UBER *TRIP 50,00
                06/10 NETFLIX.COM 39,90
                
                CARTÃO FINAL 5678
                07/10 AMAZON PRIME 14,90
                08/10 SPOTIFY 19,90
                """;

        ParsedInvoiceData result = parser.extractInvoiceData(text);

        assertThat(result.cardSections()).hasSize(2);

        ParsedCardSection first = result.cardSections().get(0);
        assertThat(first.cardLastFourDigits()).isEqualTo("1234");
        assertThat(first.items()).hasSize(2);
        assertThat(first.items().get(0).description()).contains("UBER");

        ParsedCardSection second = result.cardSections().get(1);
        assertThat(second.cardLastFourDigits()).isEqualTo("5678");
        assertThat(second.items()).hasSize(2);
        assertThat(second.items().get(0).description()).contains("AMAZON");
    }

    @Test
    void extractInvoiceData_withMultipleCardSections_shouldPopulateFlatItemsList() {
        String text = """
                Vencimento: 10/11/2024
                
                CARTÃO FINAL 1234
                05/10 UBER *TRIP 50,00
                
                CARTÃO FINAL 5678
                07/10 AMAZON PRIME 14,90
                """;

        ParsedInvoiceData result = parser.extractInvoiceData(text);

        assertThat(result.items()).hasSize(2);
        assertThat(result.allItems()).hasSize(2);
    }

    @Test
    void extractInvoiceData_withSingleCard_shouldNotCreateSections() {
        String text = """
                Vencimento: 10/11/2024
                **** 1234
                05/10 UBER *TRIP 50,00
                06/10 NETFLIX.COM 39,90
                """;

        ParsedInvoiceData result = parser.extractInvoiceData(text);

        assertThat(result.cardNumber()).isEqualTo("1234");
        assertThat(result.items()).hasSize(2);
        assertThat(result.cardSections()).hasSize(1);
        assertThat(result.cardSections().get(0).cardLastFourDigits()).isEqualTo("1234");
    }

    @Test
    void extractInvoiceData_withNoCardHeaders_shouldReturnEmptySections() {
        String text = """
                Vencimento: 10/11/2024
                05/10 UBER *TRIP 50,00
                """;

        ParsedInvoiceData result = parser.extractInvoiceData(text);

        assertThat(result.items()).hasSize(1);
        assertThat(result.cardSections()).isEmpty();
    }

    @Test
    void extractInvoiceData_withDifferentHeaderFormats_shouldDetectAll() {
        String text = """
                Vencimento: 10/11/2024
                
                CARTÃO ****1234
                05/10 UBER *TRIP 50,00
                
                CARTÃO DE CRÉDITO ****5678
                07/10 AMAZON PRIME 14,90
                """;

        ParsedInvoiceData result = parser.extractInvoiceData(text);

        assertThat(result.cardSections()).hasSize(2);
        assertThat(result.cardSections().get(0).cardLastFourDigits()).isEqualTo("1234");
        assertThat(result.cardSections().get(1).cardLastFourDigits()).isEqualTo("5678");
    }

    @Test
    void extractInvoiceData_withCardSections_shouldCalculateSubtotals() {
        String text = """
                Vencimento: 10/11/2024
                
                CARTÃO FINAL 1234
                05/10 UBER *TRIP 50,00
                06/10 NETFLIX 39,90
                
                CARTÃO FINAL 5678
                07/10 AMAZON 100,00
                """;

        ParsedInvoiceData result = parser.extractInvoiceData(text);

        ParsedCardSection first = result.cardSections().get(0);
        assertThat(first.subtotal()).isEqualByComparingTo("89.90");

        ParsedCardSection second = result.cardSections().get(1);
        assertThat(second.subtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void allItems_withCardSections_shouldReturnUnionOfAllSectionItems() {
        String text = """
                Vencimento: 10/11/2024
                
                CARTÃO FINAL 1234
                05/10 UBER *TRIP 50,00
                
                CARTÃO FINAL 5678
                07/10 AMAZON 14,90
                08/10 SPOTIFY 19,90
                """;

        ParsedInvoiceData result = parser.extractInvoiceData(text);

        assertThat(result.allItems()).hasSize(3);
    }
}
