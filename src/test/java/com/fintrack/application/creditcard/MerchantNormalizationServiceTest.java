package com.fintrack.application.creditcard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MerchantNormalizationService.
 * Tests the normalization of credit card transaction descriptions to merchant keys.
 */
@DisplayName("MerchantNormalizationService Tests")
class MerchantNormalizationServiceTest {

    private MerchantNormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new MerchantNormalizationService();
    }

    @Nested
    @DisplayName("Basic Normalization Tests")
    class BasicNormalizationTests {

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            String result = normalizationService.normalize(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for empty input")
        void shouldReturnNullForEmptyInput() {
            String result = normalizationService.normalize("");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for blank input")
        void shouldReturnNullForBlankInput() {
            String result = normalizationService.normalize("   ");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should uppercase and trim description")
        void shouldUppercaseAndTrim() {
            String result = normalizationService.normalize("  netflix  ");
            assertThat(result).isEqualTo("NETFLIX");
        }
    }

    @Nested
    @DisplayName("Payment Prefix Removal Tests")
    class PaymentPrefixRemovalTests {

        @Test
        @DisplayName("Should remove PAG* prefix")
        void shouldRemovePagPrefix() {
            String result = normalizationService.normalize("PAG*RESTAURANTE XYZ");
            assertThat(result).isEqualTo("RESTAURANTEXYZ");
        }

        @Test
        @DisplayName("Should remove PGTO* prefix")
        void shouldRemovePgtoPrefix() {
            String result = normalizationService.normalize("PGTO*MERCADO LIVRE");
            assertThat(result).isEqualTo("MERCADOLIVRE");
        }

        @Test
        @DisplayName("Should handle PAG with space separator")
        void shouldHandlePagWithSpace() {
            String result = normalizationService.normalize("PAG JOSE DA SILVA");
            assertThat(result).isEqualTo("JOSESILVA");
        }
    }

    @Nested
    @DisplayName("Known Merchant Matching Tests")
    class KnownMerchantTests {

        @Test
        @DisplayName("Should normalize UBER variations to UBER")
        void shouldNormalizeUber() {
            assertThat(normalizationService.normalize("UBER *TRIP SAO PAULO BR"))
                .isEqualTo("UBER");
            assertThat(normalizationService.normalize("UBER   *TRIP           4029357733"))
                .isEqualTo("UBER");
            assertThat(normalizationService.normalize("UBER EATS"))
                .isEqualTo("UBER");
        }

        @Test
        @DisplayName("Should normalize NETFLIX variations")
        void shouldNormalizeNetflix() {
            assertThat(normalizationService.normalize("PAG*NETFLIX.COM 4029357733"))
                .isEqualTo("NETFLIX");
            assertThat(normalizationService.normalize("NETFLIX.COM"))
                .isEqualTo("NETFLIX");
        }

        @Test
        @DisplayName("Should normalize IFOOD variations")
        void shouldNormalizeIfood() {
            assertThat(normalizationService.normalize("IFOOD*RESTAURANTE XYZ"))
                .isEqualTo("IFOOD");
            assertThat(normalizationService.normalize("IFOOD *BURGUER"))
                .isEqualTo("IFOOD");
        }

        @Test
        @DisplayName("Should normalize MERCADOLIVRE variations")
        void shouldNormalizeMercadoLivre() {
            assertThat(normalizationService.normalize("MERCADOLIVRE*MERC DO JOAO"))
                .isEqualTo("MERCADOLIVRE");
        }

        @Test
        @DisplayName("Should normalize SPOTIFY")
        void shouldNormalizeSpotify() {
            assertThat(normalizationService.normalize("SPOTIFY AB"))
                .isEqualTo("SPOTIFY");
        }
    }

    @Nested
    @DisplayName("Noise Token Removal Tests")
    class NoiseTokenRemovalTests {

        @Test
        @DisplayName("Should remove location tokens")
        void shouldRemoveLocationTokens() {
            String result = normalizationService.normalize("SUPERMERCADO X SAO PAULO BR");
            assertThat(result).doesNotContain("SAO");
            assertThat(result).doesNotContain("PAULO");
            assertThat(result).doesNotContain("BR");
        }

        @Test
        @DisplayName("Should remove legal entity suffixes")
        void shouldRemoveLegalEntitySuffixes() {
            String result = normalizationService.normalize("EMPRESA ABC LTDA ME");
            assertThat(result).doesNotContain("LTDA");
            assertThat(result).doesNotContain("ME");
        }

        @Test
        @DisplayName("Should remove transaction type tokens")
        void shouldRemoveTransactionTypeTokens() {
            String result = normalizationService.normalize("LOJA XYZ COMPRA PARCELA");
            assertThat(result).doesNotContain("COMPRA");
            assertThat(result).doesNotContain("PARCELA");
        }
    }

    @Nested
    @DisplayName("Number Removal Tests")
    class NumberRemovalTests {

        @Test
        @DisplayName("Should remove long numbers (IDs)")
        void shouldRemoveLongNumbers() {
            String result = normalizationService.normalize("LOJA 123456789 XYZ");
            assertThat(result).doesNotContain("123456789");
        }

        @Test
        @DisplayName("Should remove phone-like numbers")
        void shouldRemovePhoneNumbers() {
            String result = normalizationService.normalize("EMPRESA 4029357733");
            assertThat(result).doesNotContain("4029357733");
        }
    }

    @Nested
    @DisplayName("Accent Removal Tests")
    class AccentRemovalTests {

        @Test
        @DisplayName("Should remove accents from text")
        void shouldRemoveAccents() {
            String result = normalizationService.normalize("CAFÉ AÇUCAR JOSÉ");
            // JOSE is filtered as a short common name token
            assertThat(result).isEqualTo("CAFEACUCAR");
        }

        @Test
        @DisplayName("Should handle multiple accented characters")
        void shouldHandleMultipleAccents() {
            String result = normalizationService.normalize("PÃO DE AÇÚCAR");
            assertThat(result).doesNotContain("Ã");
            assertThat(result).doesNotContain("Ú");
        }
    }

    @Nested
    @DisplayName("Complex Description Tests")
    class ComplexDescriptionTests {

        @Test
        @DisplayName("Should handle real-world UBER description")
        void shouldHandleRealUberDescription() {
            String result = normalizationService.normalize("UBER *TRIP SAO PAULO BR");
            assertThat(result).isEqualTo("UBER");
        }

        @Test
        @DisplayName("Should handle real-world restaurant description")
        void shouldHandleRealRestaurantDescription() {
            String result = normalizationService.normalize(
                "PAG*RESTAURANTE MINEIRO BELO HORIZONTE MG");
            assertThat(result).isNotNull();
            assertThat(result).startsWith("RESTAURANTE");
        }

        @Test
        @DisplayName("Should handle supermarket description")
        void shouldHandleSupermarketDescription() {
            String result = normalizationService.normalize(
                "SUPERMERCADO X LTDA - 00341 SAO PAULO SP");
            assertThat(result).isNotNull();
            assertThat(result).startsWith("SUPERMERCADO");
        }

        @Test
        @DisplayName("Should handle gas station description")
        void shouldHandleGasStationDescription() {
            // SHELL alone should normalize to SHELL
            String result = normalizationService.normalize("SHELL BR 116");
            assertThat(result).isEqualTo("SHELL");
            
            // When POSTO is included, it's kept as part of the merchant key
            String resultWithPosto = normalizationService.normalize("POSTO SHELL BR 116");
            assertThat(resultWithPosto).contains("SHELL");
        }

        @Test
        @DisplayName("Should truncate very long results")
        void shouldTruncateLongResults() {
            String result = normalizationService.normalize(
                "NOME MUITO LONGO DE ESTABELECIMENTO QUE EXCEDE LIMITE NORMAL");
            assertThat(result).isNotNull();
            assertThat(result.length()).isLessThanOrEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should return null for only noise tokens")
        void shouldReturnNullForOnlyNoiseTokens() {
            String result = normalizationService.normalize("BR SP LTDA ME");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for only numbers")
        void shouldReturnNullForOnlyNumbers() {
            String result = normalizationService.normalize("123456789");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle special characters")
        void shouldHandleSpecialCharacters() {
            String result = normalizationService.normalize("LOJA @#$% TESTE");
            assertThat(result).isNotNull();
            assertThat(result).doesNotContain("@");
            assertThat(result).doesNotContain("#");
        }
    }
}
