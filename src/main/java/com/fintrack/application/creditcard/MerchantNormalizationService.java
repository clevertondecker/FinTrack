package com.fintrack.application.creditcard;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for normalizing merchant descriptions from credit card invoices.
 *
 * <p>Transforms raw transaction descriptions like:
 * <ul>
 *   <li>"PAG*JoseDaSilva SAO PAULO BR" → "JOSEDASILVA"</li>
 *   <li>"UBER *TRIP 4029357733" → "UBER"</li>
 *   <li>"MERCADOLIVRE*MERC DO JOAO" → "MERCADOLIVRE"</li>
 * </ul>
 *
 * <p>This normalization enables consistent matching of the same merchant
 * across different transactions despite minor variations in the description.
 */
@Service
public class MerchantNormalizationService {

    /** Maximum length for normalized merchant key. */
    private static final int MAX_KEY_LENGTH = 50;

    /** Minimum length for a valid merchant key. */
    private static final int MIN_KEY_LENGTH = 2;

    /** Pattern to match accented characters for removal. */
    private static final Pattern ACCENT_PATTERN = Pattern.compile("\\p{M}");

    /** Pattern to match multiple whitespaces. */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /** Pattern to match long numbers (IDs, phone numbers, etc.). */
    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile("\\d{6,}");

    /** Pattern to match short numbers with surrounding context. */
    private static final Pattern SHORT_NUMBER_PATTERN = Pattern.compile("\\b\\d{1,5}\\b");

    /** Pattern to match special characters except alphanumeric and spaces. */
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^A-Z0-9\\s]");

    /** Common prefixes used by payment processors. */
    private static final Set<String> PAYMENT_PREFIXES = Set.of(
        "PAG", "PGTO", "PAGTO", "PG", "PIX"
    );

    /** Common suffixes and noise tokens to remove. */
    private static final Set<String> NOISE_TOKENS = Set.of(
        // Location tokens
        "BR", "BRASIL", "BRAZIL",
        "SAO", "PAULO", "SP", "RJ", "MG", "RS", "PR", "SC", "BA", "PE", "CE",
        "RIO", "JANEIRO", "BELO", "HORIZONTE", "PORTO", "ALEGRE", "CURITIBA",
        "FLORIANOPOLIS", "SALVADOR", "RECIFE", "FORTALEZA", "BRASILIA",

        // Legal entity suffixes
        "LTDA", "ME", "EPP", "EIRELI", "SA", "SS", "FILIAL",

        // Common business suffixes
        "COM", "NET", "ORG", "IO", "APP",
        "LOJA", "STORE", "SHOP",

        // Transaction types
        "COMPRA", "PURCHASE", "PAGAMENTO", "PAYMENT",
        "DEBITO", "CREDITO", "DEBIT", "CREDIT",
        "PARCELA", "PARC", "INSTALLMENT",

        // Service indicators
        "TRIP", "RIDE", "EATS", "DELIVERY", "ENTREGA",

        // Misc noise
        "DE", "DO", "DA", "DOS", "DAS", "E", "THE", "AND", "OF"
    );

    /** Known merchant patterns for better normalization. */
    private static final Set<String> KNOWN_MERCHANTS = Set.of(
        "UBER", "IFOOD", "RAPPI", "NETFLIX", "SPOTIFY", "AMAZON", "MERCADOLIVRE",
        "MERCADOPAGO", "PICPAY", "NUBANK", "ITAU", "BRADESCO", "SANTANDER",
        "GOOGLE", "APPLE", "MICROSOFT", "STEAM", "PLAYSTATION", "XBOX",
        "SHELL", "IPIRANGA", "BR", "PETROBRAS", "ALE",
        "CARREFOUR", "EXTRA", "PAO", "ACUCAR", "ATACADAO", "ASSAI", "BIG",
        "DROGASIL", "DROGARIA", "PACHECO", "PANVEL", "RAIA",
        "RENNER", "RIACHUELO", "CEA", "MARISA", "HERING",
        "MCDONALDS", "BURGER", "KING", "SUBWAY", "STARBUCKS", "OUTBACK",
        "CLARO", "VIVO", "TIM", "OI", "NET", "SKY",
        "ENEL", "CPFL", "LIGHT", "CEMIG", "COPEL", "SABESP", "SANEPAR"
    );

    /**
     * Normalizes a transaction description to a merchant key.
     *
     * @param description the raw transaction description. Can be null or empty.
     * @return the normalized merchant key, or null if normalization fails.
     */
    public String normalize(final String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String result = description;

        // Step 1: Uppercase and trim
        result = result.toUpperCase().trim();

        // Step 2: Remove accents
        result = removeAccents(result);

        // Step 3: Remove payment processor prefixes (PAG*, PGTO*, etc.)
        result = removePaymentPrefixes(result);

        // Step 4: Remove long numbers (IDs, phone numbers)
        result = LONG_NUMBER_PATTERN.matcher(result).replaceAll(" ");

        // Step 5: Remove special characters
        result = SPECIAL_CHARS_PATTERN.matcher(result).replaceAll(" ");

        // Step 6: Tokenize and filter
        result = filterTokens(result);

        // Step 7: Try to match known merchants
        String knownMerchant = matchKnownMerchant(result);
        if (knownMerchant != null) {
            return knownMerchant;
        }

        // Step 8: Take first significant tokens
        result = extractPrimaryTokens(result);

        // Step 9: Final cleanup and length check
        result = finalCleanup(result);

        if (result == null || result.length() < MIN_KEY_LENGTH) {
            return null;
        }

        return result;
    }

    /**
     * Removes accents from a string.
     *
     * @param input the input string. Must not be null.
     * @return the string without accents. Never null.
     */
    private String removeAccents(final String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return ACCENT_PATTERN.matcher(normalized).replaceAll("");
    }

    /**
     * Removes payment processor prefixes.
     *
     * @param input the input string. Must not be null.
     * @return the string without payment prefixes. Never null.
     */
    private String removePaymentPrefixes(final String input) {
        String result = input;

        // Remove prefix patterns like "PAG*", "PGTO*", etc.
        for (String prefix : PAYMENT_PREFIXES) {
            if (result.startsWith(prefix + "*") || result.startsWith(prefix + " ")) {
                result = result.substring(prefix.length() + 1).trim();
                break;
            }
            if (result.startsWith(prefix)) {
                // Check if followed by another word
                String remaining = result.substring(prefix.length());
                if (!remaining.isEmpty() && !Character.isLetterOrDigit(remaining.charAt(0))) {
                    result = remaining.substring(1).trim();
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Filters out noise tokens from the description.
     *
     * @param input the input string. Must not be null.
     * @return the filtered string. Never null.
     */
    private String filterTokens(final String input) {
        String[] tokens = WHITESPACE_PATTERN.split(input.trim());
        StringBuilder result = new StringBuilder();

        for (String token : tokens) {
            // Skip empty tokens
            if (token.isEmpty()) {
                continue;
            }

            // Skip noise tokens
            if (NOISE_TOKENS.contains(token)) {
                continue;
            }

            // Skip pure numbers (short ones)
            if (token.matches("\\d+")) {
                continue;
            }

            // Skip very short tokens (likely noise)
            if (token.length() < 2) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(token);
        }

        return result.toString();
    }

    /**
     * Tries to match a known merchant from the description.
     *
     * @param input the filtered description. Must not be null.
     * @return the known merchant name if found, null otherwise.
     */
    private String matchKnownMerchant(final String input) {
        // Check for exact match first
        for (String merchant : KNOWN_MERCHANTS) {
            if (input.equals(merchant) || input.startsWith(merchant + " ")) {
                return merchant;
            }
        }

        // Check for contains (for cases like "IFOODRESTAURANTE" → "IFOOD")
        for (String merchant : KNOWN_MERCHANTS) {
            if (input.startsWith(merchant)) {
                return merchant;
            }
        }

        return null;
    }

    /**
     * Extracts the primary (first significant) tokens from the description.
     *
     * @param input the filtered description. Must not be null.
     * @return the primary tokens. Never null.
     */
    private String extractPrimaryTokens(final String input) {
        String[] tokens = WHITESPACE_PATTERN.split(input.trim());

        if (tokens.length == 0) {
            return "";
        }

        // For single token, return it
        if (tokens.length == 1) {
            return tokens[0];
        }

        // For multiple tokens, take first 1-2 significant ones
        StringBuilder result = new StringBuilder();
        int count = 0;
        int maxTokens = 2;

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }

            if (count >= maxTokens) {
                break;
            }

            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(token);
            count++;
        }

        return result.toString();
    }

    /**
     * Performs final cleanup on the normalized key.
     *
     * @param input the input string. Can be null.
     * @return the cleaned up string, or null if invalid.
     */
    private String finalCleanup(final String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        // Remove remaining short numbers
        String result = SHORT_NUMBER_PATTERN.matcher(input).replaceAll(" ");

        // Collapse whitespace and trim
        result = WHITESPACE_PATTERN.matcher(result).replaceAll(" ").trim();

        // Remove spaces for final key (compact form)
        result = result.replace(" ", "");

        // Truncate if too long
        if (result.length() > MAX_KEY_LENGTH) {
            result = result.substring(0, MAX_KEY_LENGTH);
        }

        return result.isEmpty() ? null : result;
    }
}
