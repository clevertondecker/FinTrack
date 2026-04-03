package com.fintrack.infrastructure.parsing;

import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedCardSection;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedInvoiceItem;
import org.apache.commons.lang3.Validate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing invoice data from PDF files.
 */
@Service
public class PdfInvoiceParser {

    private static final Logger logger = LoggerFactory.getLogger(PdfInvoiceParser.class);

    // Regex patterns for extracting data
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\*{4}\\s*(\\d{4})");
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
        "(?:vencimento|due date|vence em)\\s*:?\\s*(\\d{2})/(\\d{2})/(\\d{4})",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "(?:total|valor total|amount)\\s*:?\\s*R\\$\\s*([0-9.,]+)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern BANK_PATTERN = Pattern.compile(
        "(?:banco|bank)\\s*:?\\s*([A-Za-zÀ-ú]{3,20})",
        Pattern.CASE_INSENSITIVE);

    private static final List<String> KNOWN_BANKS = List.of(
        "Santander", "Nubank", "Itaú", "Itau", "Bradesco", "Banco do Brasil",
        "Caixa", "Inter", "C6 Bank", "C6Bank", "BTG", "Original", "Pan",
        "Sicredi", "Sicoob", "Banrisul", "Safra", "Votorantim", "Next",
        "Neon", "PicPay", "Mercado Pago", "XP", "Modal", "Daycoval"
    );
    /**
     * Pattern to capture international transactions with values in reais and dollars.
     * Format: Date Description Foreign_Currency_Value CURRENCY_NAME Reais_Value Dollar_Value
     * Example: "06/07 COT(WEB) 2.310,00 PESO URUGU 330,67 57,55"
     * Groups: 1=Date, 2=Description, 3=Foreign_Currency_Value, 4=Reais_Value, 5=Dollar_Value
     */
    private static final Pattern INTERNATIONAL_ITEM_PATTERN = Pattern.compile(
        "^.*?(\\d{2}/\\d{2})\\s+(.+?)\\s+([\\d.,]+)\\s+[A-Z\\s]+\\s+"
            + "([\\d.,]+)\\s+([\\d.,]+)$"
    );

    /**
     * Pattern to capture IOF (Imposto sobre Operações Financeiras) charges.
     * Format: IOF DESPESA NO EXTERIOR amount
     * Example: "IOF DESPESA NO EXTERIOR 13,54"
     * Groups: 1=Amount
     */
    private static final Pattern IOF_PATTERN = Pattern.compile(
        "^IOF\\s+DESPESA\\s+NO\\s+EXTERIOR\\s+([\\d.,]+)$"
    );
    
    // Padrão para itens normais (apenas valor em reais)
    private static final Pattern SANTANDER_ITEM_PATTERN = Pattern.compile(
        "^.*?(\\d{2}/\\d{2})\\s+(.+?)\\s+(-?[\\d.,]+)$"
    );

    // Padrão para itens com parcelamento (formato: LIBERTY DUTY FREE 02/04 123,45)
    private static final Pattern PARCELED_ITEM_PATTERN = Pattern.compile(
        "^(.+?)\\s+(\\d{2}/\\d{2})\\s+(-?[\\d.,]+)$"
    );

    /**
     * Detects card section headers in consolidated invoices.
     * Matches patterns like "CARTÃO FINAL 1234", "CARTÃO ****1234", "CARD **** 5678".
     */
    private static final Pattern CARD_SECTION_HEADER_PATTERN = Pattern.compile(
        "(?:CART[ÃA]O|CARD)\\s*(?:FINAL|DE\\s+CR[ÉE]DITO)?\\s*\\*{0,4}\\s*(\\d{4})",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Detects Santander-style card section headers.
     * Matches patterns like "@ CLEVERTON DECKER - 4258 XXXX XXXX 8511"
     * or "SABRINA - 4258 XXXX XXXX 5050".
     * Group 1 = cardholder name, Group 2 = last 4 digits.
     */
    private static final Pattern SANTANDER_CARD_HEADER_PATTERN = Pattern.compile(
        "^@?\\s*(.+?)\\s+-\\s+\\d{4}\\s+[Xx]{4}\\s+[Xx]{4}\\s+(\\d{4})$"
    );

    private static final List<String> IGNORE_KEYWORDS = List.of(
        "tarifa", "juros", "saldo", "autorização", "parcela", "CET", "multas",
        "fatura", "total", "rotativo", "saque", "remuneratórios", "cancelar",
        "central", "atendimento", "seguro", "prestamista", "valor parcelado"
    );

    /**
     * Parses a PDF file and extracts invoice data.
     *
     * @param filePath the path to the PDF file. Cannot be null or blank.
     * @return the parsed invoice data. Never null.
     * @throws IOException if there's an error reading the PDF file.
     */
    public ParsedInvoiceData parsePdf(String filePath) throws IOException {
        Validate.notBlank(filePath, "File path must not be blank.");

        logger.info("Starting PDF parsing for file: {}", filePath);

        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            logger.debug("Extracted text from PDF: {}", text.substring(0, Math.min(500, text.length())));

            return extractInvoiceData(text);
        } catch (IOException e) {
            logger.error("Error parsing PDF file: {}", filePath, e);
            throw e;
        }
    }

    /**
     * Extracts invoice data from the extracted text, detecting per-card sections
     * when the PDF contains multiple cards (consolidated invoices).
     *
     * @param text the text extracted from the PDF. Cannot be null.
     * @return the parsed invoice data. Never null.
     */
    public ParsedInvoiceData extractInvoiceData(String text) {
        Validate.notNull(text, "Text must not be null.");

        String creditCardName = extractCreditCardName(text);
        String cardNumber = extractCardNumber(text);
        LocalDate dueDate = extractDueDate(text);
        BigDecimal totalAmount = extractTotalAmount(text);
        String bankName = extractBankName(text);
        YearMonth invoiceMonth = extractInvoiceMonth(text);

        List<ParsedCardSection> cardSections = extractCardSections(text);

        List<ParsedInvoiceItem> items;
        if (!cardSections.isEmpty()) {
            items = new ArrayList<>();
            for (ParsedCardSection section : cardSections) {
                items.addAll(section.items());
            }
            logger.info("Detected {} card sections with {} total items",
                    cardSections.size(), items.size());
        } else {
            items = extractItemsFromLines(text.split("\n"));
            if (cardNumber != null && !items.isEmpty()) {
                BigDecimal subtotal = items.stream()
                        .map(ParsedInvoiceItem::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                cardSections = List.of(new ParsedCardSection(
                        cardNumber, creditCardName, items, subtotal));
            }
        }

        double confidence = calculateConfidence(text, totalAmount, dueDate, items);

        return new ParsedInvoiceData(
            creditCardName,
            cardNumber,
            dueDate,
            totalAmount,
            items,
            bankName,
            invoiceMonth,
            confidence,
            cardSections
        );
    }

    /**
     * Splits the PDF text into per-card sections using card header patterns,
     * then parses items within each section.
     *
     * @param text the full PDF text. Cannot be null.
     * @return list of card sections. Empty if no card headers detected.
     */
    private List<ParsedCardSection> extractCardSections(String text) {
        String[] lines = text.split("\n");
        Map<String, List<String>> sectionLines = new LinkedHashMap<>();
        Map<String, String> sectionDisplayNames = new LinkedHashMap<>();
        String currentCard = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            Matcher headerMatcher = CARD_SECTION_HEADER_PATTERN.matcher(line);
            if (headerMatcher.find()) {
                currentCard = headerMatcher.group(1);
                sectionLines.putIfAbsent(currentCard, new ArrayList<>());
                sectionDisplayNames.putIfAbsent(currentCard, line);
                continue;
            }

            Matcher santanderMatcher = SANTANDER_CARD_HEADER_PATTERN.matcher(line);
            if (santanderMatcher.find()) {
                currentCard = santanderMatcher.group(2);
                String displayName = santanderMatcher.group(1).trim() + " - **** " + currentCard;
                sectionLines.putIfAbsent(currentCard, new ArrayList<>());
                sectionDisplayNames.putIfAbsent(currentCard, displayName);
                logger.info("Detected Santander card section: {} (****{})", displayName, currentCard);
                continue;
            }

            if (currentCard != null) {
                sectionLines.get(currentCard).add(line);
            }
        }

        if (sectionLines.size() < 2) {
            return List.of();
        }

        List<ParsedCardSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : sectionLines.entrySet()) {
            String digits = entry.getKey();
            List<ParsedInvoiceItem> items =
                    extractItemsFromLines(entry.getValue().toArray(new String[0]));
            BigDecimal subtotal = items.stream()
                    .map(ParsedInvoiceItem::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sections.add(new ParsedCardSection(
                    digits, sectionDisplayNames.get(digits), items, subtotal));
        }
        return sections;
    }

    /**
     * Extracts the credit card name from the text.
     *
     * @param text the text to search in. Cannot be null.
     * @return the credit card name, or null if not found.
     */
    private String extractCreditCardName(String text) {
        // Look for common credit card name patterns
        String[] patterns = {
            "cartão\\s*:?\\s*([A-Za-z\\s]+)",
            "card\\s*:?\\s*([A-Za-z\\s]+)",
            "([A-Za-z\\s]+)\\s*cartão"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                if (name.length() > 2) {
                    return name;
                }
            }
        }

        return null;
    }

    /**
     * Extracts the last four digits of the card number.
     *
     * @param text the text to search in. Cannot be null.
     * @return the last four digits, or null if not found.
     */
    private String extractCardNumber(String text) {
        Matcher matcher = CARD_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extracts the due date from the text.
     *
     * @param text the text to search in. Cannot be null.
     * @return the due date, or null if not found.
     */
    private LocalDate extractDueDate(String text) {
        Matcher matcher = DUE_DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            } catch (NumberFormatException | DateTimeParseException e) {
                logger.warn("Error parsing due date: {}", matcher.group(0), e);
            }
        }
        return null;
    }

    /**
     * Extracts the total amount from the text.
     *
     * @param text the text to search in. Cannot be null.
     * @return the total amount, or null if not found.
     */
    private BigDecimal extractTotalAmount(String text) {
        Matcher matcher = TOTAL_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return parseBrazilianAmount(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Error parsing total amount: {}", matcher.group(0), e);
            }
        }
        return null;
    }

    /**
     * Extracts the bank name from the text.
     * Tries known bank names first, then falls back to a regex pattern.
     *
     * @param text the text to search in. Cannot be null.
     * @return the bank name, or null if not found.
     */
    private String extractBankName(String text) {
        String upperText = text.toUpperCase();
        for (String bank : KNOWN_BANKS) {
            if (upperText.contains(bank.toUpperCase())) {
                return bank;
            }
        }

        Matcher matcher = BANK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extracts the invoice month from the text.
     *
     * @param text the text to search in. Cannot be null.
     * @return the invoice month, or null if not found.
     */
    private YearMonth extractInvoiceMonth(String text) {
        // Look for month/year patterns
        Pattern monthPattern = Pattern.compile("(\\d{2})/(\\d{4})");
        Matcher matcher = monthPattern.matcher(text);
        if (matcher.find()) {
            try {
                int month = Integer.parseInt(matcher.group(1));
                int year = Integer.parseInt(matcher.group(2));
                return YearMonth.of(year, month);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing invoice month: {}", matcher.group(0), e);
            }
        }
        return null;
    }

    private boolean shouldSkipLine(String line) {
        String lower = line.toLowerCase();
        for (String keyword : IGNORE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private LocalDate parseDate(String dateStr) {
        int currentYear = Year.now().getValue();
        try {
            LocalDate date = LocalDate.parse(dateStr + "/" + currentYear,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            // If the parsed date is more than 2 months in the future,
            // it likely belongs to the previous year (e.g. Dec purchase parsed in Jan)
            if (date.isAfter(LocalDate.now().plusMonths(2))) {
                date = date.minusYears(1);
            }
            return date;
        } catch (DateTimeParseException e) {
            logger.warn("Error parsing date '{}' with year {}", dateStr, currentYear);
            return null;
        }
    }

    private BigDecimal parseBrazilianAmount(String amountStr) {
        String normalized = amountStr.replace(".", "").replace(",", ".");
        return new BigDecimal(normalized);
    }

    /**
     * Extracts individual items from an array of text lines.
     *
     * @param lines the lines to parse. Cannot be null.
     * @return a list of parsed items. Never null, may be empty.
     */
    private List<ParsedInvoiceItem> extractItemsFromLines(String[] lines) {
        List<ParsedInvoiceItem> items = new ArrayList<>();
        int processed = 0;
        int ignored = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || shouldSkipLine(line)) {
                continue;
            }

            ParsedInvoiceItem item = tryParseLine(line);
            if (item != null) {
                items.add(item);
                processed++;
            } else {
                ignored++;
                logger.debug("No pattern matched: {}", line);
            }
        }

        logger.info("Extracted {} items (processed={}, ignored={})",
                items.size(), processed, ignored);
        return items;
    }

    private ParsedInvoiceItem tryParseLine(String line) {
        ParsedInvoiceItem item = tryParseIof(line);
        if (item != null) {
            return item;
        }
        item = tryParseInternational(line);
        if (item != null) {
            return item;
        }
        item = tryParseParceled(line);
        if (item != null) {
            return item;
        }
        return tryParseStandard(line);
    }

    private ParsedInvoiceItem tryParseIof(String line) {
        Matcher m = IOF_PATTERN.matcher(line);
        if (!m.find()) {
            return null;
        }
        try {
            BigDecimal amount = parseBrazilianAmount(m.group(1));
            return new ParsedInvoiceItem(
                "IOF DESPESA NO EXTERIOR", amount,
                LocalDate.now(), null, 1, 1, 0.9);
        } catch (Exception e) {
            logger.warn("Error processing IOF line: {}", line, e);
            return null;
        }
    }

    private ParsedInvoiceItem tryParseInternational(String line) {
        Matcher m = INTERNATIONAL_ITEM_PATTERN.matcher(line);
        if (!m.find()) {
            return null;
        }
        try {
            LocalDate date = parseDate(m.group(1));
            String description = m.group(2).trim();
            BigDecimal amount = parseBrazilianAmount(m.group(4));
            return new ParsedInvoiceItem(
                description, amount, date, null, 1, 1, 0.9);
        } catch (Exception e) {
            logger.warn("Error processing international item: {}", line, e);
            return null;
        }
    }

    private static final Pattern INSTALLMENT_SUFFIX =
            Pattern.compile("^(.+?)\\s+(\\d{1,2})/(\\d{1,2})$");

    private ParsedInvoiceItem tryParseParceled(String line) {
        Matcher m = PARCELED_ITEM_PATTERN.matcher(line);
        if (!m.find()) {
            return null;
        }
        try {
            String[] parts = m.group(2).split("/");
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            if (second >= first && first >= 1 && second >= 2 && second <= 48) {
                String description = m.group(1).trim();
                if (SANTANDER_ITEM_PATTERN.matcher(line).find()) {
                    return null;
                }
                BigDecimal amount = parseBrazilianAmount(m.group(3));
                return new ParsedInvoiceItem(
                    description, amount, LocalDate.now(), null,
                    first, second, 0.9);
            }
        } catch (Exception e) {
            logger.warn("Error processing parceled item: {}", line, e);
        }
        return null;
    }

    private ParsedInvoiceItem tryParseStandard(String line) {
        Matcher m = SANTANDER_ITEM_PATTERN.matcher(line);
        if (!m.find()) {
            return null;
        }
        try {
            LocalDate date = parseDate(m.group(1));
            String description = m.group(2).trim();
            BigDecimal amount = parseBrazilianAmount(m.group(3));

            int installments = 1;
            int totalInstallments = 1;
            Matcher im = INSTALLMENT_SUFFIX.matcher(description);
            if (im.matches()) {
                int current = Integer.parseInt(im.group(2));
                int total = Integer.parseInt(im.group(3));
                if (total >= 2 && total <= 48
                        && current >= 1 && current <= total) {
                    description = im.group(1).trim();
                    installments = current;
                    totalInstallments = total;
                }
            }

            return new ParsedInvoiceItem(
                description, amount, date, null,
                installments, totalInstallments, 0.9);
        } catch (Exception e) {
            logger.warn("Error processing standard item: {}", line, e);
            return null;
        }
    }

    /**
     * Calculates the confidence level of the parsing.
     *
     * @param text the original text. Cannot be null.
     * @param totalAmount the extracted total amount. Can be null.
     * @param dueDate the extracted due date. Can be null.
     * @param items the extracted items. Cannot be null.
     * @return a confidence value between 0.0 and 1.0.
     */
    private double calculateConfidence(
            String text, BigDecimal totalAmount, LocalDate dueDate,
            List<ParsedInvoiceItem> items) {
        double confidence = 0.0;

        // Base confidence from text quality
        if (text.length() > 100) {
            confidence += 0.2;
        }
        if (text.length() > 500) {
            confidence += 0.1;
        }

        // Confidence from extracted data
        if (totalAmount != null) {
            confidence += 0.3;
        }
        if (dueDate != null) {
            confidence += 0.2;
        }
        if (!items.isEmpty()) {
            confidence += 0.2;
        }

        // Additional confidence for multiple items
        if (items.size() > 1) {
            confidence += 0.1;
        }

        return Math.min(confidence, 1.0);
    }
} 