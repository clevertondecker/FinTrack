package com.fintrack.service.invoice;

import com.fintrack.dto.invoice.ParsedInvoiceData;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing invoice data from PDF files.
 */
@Service
public class PdfInvoiceParser {

    private static final Logger logger = LoggerFactory.getLogger(PdfInvoiceParser.class);

    // Regex patterns for extracting data
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("R\\$\\s*([0-9.,]+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\*{4}\\s*(\\d{4})");
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile("(?:vencimento|due date|vence em)\\s*:?\\s*(\\d{2})/(\\d{2})/(\\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTAL_PATTERN = Pattern.compile("(?:total|valor total|amount)\\s*:?\\s*R\\$\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANK_PATTERN = Pattern.compile("(?:banco|bank)\\s*:?\\s*([A-Za-z\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SANTANDER_ITEM_PATTERN = Pattern.compile(
        "^.*?(\\d{2}/\\d{2})\\s+(.+?)(?:\\s+(\\d{2}/\\d{2,4}))?\\s+([\\d.,]+)$"
    );

    private static final List<String> IGNORE_KEYWORDS = List.of(
        "tarifa", "juros", "saldo", "autorização", "parcela", "CET", "multas", "fatura", "total", "rotativo", "saque", "remuneratórios", "cancelar", "central", "atendimento", "seguro", "prestamista", "valor parcelado"
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
     * Extracts invoice data from the extracted text.
     *
     * @param text the text extracted from the PDF. Cannot be null.
     * @return the parsed invoice data. Never null.
     */
    private ParsedInvoiceData extractInvoiceData(String text) {
        Validate.notNull(text, "Text must not be null.");

        String creditCardName = extractCreditCardName(text);
        String cardNumber = extractCardNumber(text);
        LocalDate dueDate = extractDueDate(text);
        BigDecimal totalAmount = extractTotalAmount(text);
        String bankName = extractBankName(text);
        YearMonth invoiceMonth = extractInvoiceMonth(text);
        List<ParsedInvoiceItem> items = extractItems(text);

        // Calculate confidence based on extracted data quality
        double confidence = calculateConfidence(text, totalAmount, dueDate, items);

        return new ParsedInvoiceData(
            creditCardName,
            cardNumber,
            dueDate,
            totalAmount,
            items,
            bankName,
            invoiceMonth,
            confidence
        );
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
                String amountStr = matcher.group(1).replace(".", "").replace(",", ".");
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing total amount: {}", matcher.group(0), e);
            }
        }
        return null;
    }

    /**
     * Extracts the bank name from the text.
     *
     * @param text the text to search in. Cannot be null.
     * @return the bank name, or null if not found.
     */
    private String extractBankName(String text) {
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

    private boolean isValidItemLine(String line) {
        String lower = line.toLowerCase();
        for (String keyword : IGNORE_KEYWORDS) {
            if (lower.contains(keyword)) return false;
        }
        // Ignorar linhas muito curtas ou muito longas
        if (lower.length() < 4 || lower.length() > 80) return false;
        return true;
    }

    /**
     * Extracts individual items from the text.
     * This is a simplified implementation - in a real scenario, this would be much more complex.
     *
     * @param text the text to search in. Cannot be null.
     * @return a list of parsed items. Never null, may be empty.
     */
    private List<ParsedInvoiceItem> extractItems(String text) {
        List<ParsedInvoiceItem> items = new ArrayList<>();
        logger.info("Extracting items from text ({} characters)", text.length());

        String[] lines = text.split("\n");
        logger.info("Text has {} lines", lines.length);

        int currentYear = Year.now().getValue();
        for (String line : lines) {
            line = line.trim();
            Matcher matcher = SANTANDER_ITEM_PATTERN.matcher(line);
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String parcela = matcher.group(3); // pode ser null
                String amountStr = matcher.group(4).replace(".", "").replace(",", ".");
                try {
                    BigDecimal amount = new BigDecimal(amountStr);
                    LocalDate purchaseDate = LocalDate.parse(dateStr + "/" + currentYear, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    Integer installments = 1;
                    Integer totalInstallments = 1;
                    if (parcela != null && parcela.matches("\\d{2}/\\d{2,4}")) {
                        String[] parts = parcela.split("/");
                        try {
                            installments = Integer.parseInt(parts[0]);
                            totalInstallments = Integer.parseInt(parts[1]);
                        } catch (Exception e) {
                            logger.warn("Erro ao processar parcela: {}", parcela, e);
                        }
                    }
                    ParsedInvoiceItem item = new ParsedInvoiceItem(
                        description, amount, purchaseDate, null, installments, totalInstallments, 0.9
                    );
                    items.add(item);
                    logger.debug("Found item: '{}' - {} - {} - R$ {}", dateStr, description, parcela, amount);
                } catch (Exception e) {
                    logger.warn("Erro ao processar item: {}", line, e);
                }
            }
        }
        logger.info("Extracted {} items from PDF (Santander parser)", items.size());
        return items;
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
    private double calculateConfidence(String text, BigDecimal totalAmount, LocalDate dueDate, List<ParsedInvoiceItem> items) {
        double confidence = 0.0;

        // Base confidence from text quality
        if (text.length() > 100) confidence += 0.2;
        if (text.length() > 500) confidence += 0.1;

        // Confidence from extracted data
        if (totalAmount != null) confidence += 0.3;
        if (dueDate != null) confidence += 0.2;
        if (!items.isEmpty()) confidence += 0.2;

        // Additional confidence for multiple items
        if (items.size() > 1) confidence += 0.1;

        return Math.min(confidence, 1.0);
    }
} 