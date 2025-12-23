package com.fintrack.infrastructure.parsing;

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
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
        "(?:vencimento|due date|vence em)\\s*:?\\s*(\\d{2})/(\\d{2})/(\\d{4})",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "(?:total|valor total|amount)\\s*:?\\s*R\\$\\s*([0-9.,]+)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern BANK_PATTERN = Pattern.compile(
        "(?:banco|bank)\\s*:?\\s*([A-Za-z\\s]+)",
        Pattern.CASE_INSENSITIVE);
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

    private static final List<String> IGNORE_KEYWORDS = List.of(
        "tarifa", "juros", "saldo", "autorização", "parcela", "CET", "multas",
        "fatura", "total", "rotativo", "saque", "remuneratórios", "cancelar",
        "central", "atendimento", "seguro", "prestamista", "valor parcelado"
    );

    private static final List<String> IGNORE_NEGATIVE_KEYWORDS = List.of(
        "pagamento de fatura", "pagamento", "crédito", "demais créditos", "estorno de pagamento"
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
    public ParsedInvoiceData extractInvoiceData(String text) {
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
            return LocalDate.parse(dateStr + "/" + currentYear, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            logger.warn("Error parsing date '{}' with current year {}: {}", dateStr, currentYear, e);
            // Fallback to parsing with current year if the date is ambiguous
            try {
                return LocalDate.parse(dateStr + "/" + currentYear, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException fallbackE) {
                logger.warn("Error parsing date '{}' with current year {}: {}", dateStr, currentYear, fallbackE);
                return null; // Indicate failure
            }
        }
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
        String[] lines = text.split("\n");
        
        logger.info("Text has {} lines", lines.length);
        
        // Track ignored lines for debugging
        List<String> ignoredLines = new ArrayList<>();
        List<String> processedLines = new ArrayList<>();
        
        for (String line : lines) {
            line = line.trim();
            logger.debug("Processing line: '{}'", line);
            
            // Skip empty lines
            if (line.isEmpty()) {
                logger.debug("Skipping empty line");
                continue;
            }
            
            // Skip lines with keywords that should be ignored
            if (shouldSkipLine(line)) {
                logger.debug("Skipping line with keywords: '{}'", line);
                ignoredLines.add("IGNORED (keywords): " + line);
                continue;
            }
            
            // First try the pattern for international transactions (with reais and dollar values)
                           // Test IOF pattern first
               Matcher iofMatcher = IOF_PATTERN.matcher(line);
               logger.debug("Testing IOF pattern on line: '{}'", line);
               if (iofMatcher.find()) {
                   logger.debug("IOF pattern matched!");
                   String amountStr = iofMatcher.group(1).replace(".", "").replace(",", ".");
                   
                   logger.debug("IOF pattern matched: amount={}", amountStr);
                   
                   try {
                       BigDecimal amount = new BigDecimal(amountStr);
                       // Use today's date for IOF charges since they don't have specific dates
                       LocalDate date = LocalDate.now();
                       
                       items.add(new ParsedInvoiceItem("IOF DESPESA NO EXTERIOR", amount, date, null, 1, 1, 0.9));
                       logger.debug("Added IOF item: IOF DESPESA NO EXTERIOR - R$ {}", amount);
                       processedLines.add("IOF: " + line + " -> R$ " + amount);
                   } catch (Exception e) {
                       logger.warn("Erro ao processar IOF: {}", line, e);
                       ignoredLines.add("ERROR (IOF): " + line + " - " + e.getMessage());
                   }
                   continue;
               }

               Matcher internationalMatcher = INTERNATIONAL_ITEM_PATTERN.matcher(line);
               logger.debug("Testing international pattern on line: '{}'", line);
               if (internationalMatcher.find()) {
                   logger.debug("International pattern matched!");
                   String dateStr = internationalMatcher.group(1);
                   String description = internationalMatcher.group(2).trim();
                   String foreignAmountStr = internationalMatcher.group(3).replace(".", "").replace(",", ".");
                   String realAmountStr = internationalMatcher.group(4).replace(".", "").replace(",", ".");
                   String dollarAmountStr = internationalMatcher.group(5).replace(".", "").replace(",", ".");
                   
                   logger.debug("International pattern matched: date={}, desc={}, foreign={}, real={}, dollar={}", 
                       dateStr, description, foreignAmountStr, realAmountStr, dollarAmountStr);
                   
                   try {
                       // Use the reais value (priority) instead of the dollar value
                       // This fixes the bug where the system was incorrectly using the dollar value
                       BigDecimal amount = new BigDecimal(realAmountStr);
                       LocalDate date = parseDate(dateStr);
                       
                       items.add(new ParsedInvoiceItem(description, amount, date, null, 1, 1, 0.9));
                       logger.debug("Added international item: {} - R$ {}", description, amount);
                       processedLines.add("INTERNATIONAL: " + line + " -> R$ " + amount);
                   } catch (Exception e) {
                       logger.warn("Erro ao processar item internacional: {}", line, e);
                       ignoredLines.add("ERROR (international): " + line + " - " + e.getMessage());
                   }
                   continue;
               }
            
                           // Try parceled item pattern
               Matcher parceledMatcher = PARCELED_ITEM_PATTERN.matcher(line);
               if (parceledMatcher.find()) {
                   logger.debug("Parceled pattern matched!");
                   String description = parceledMatcher.group(1).trim();
                   String installmentStr = parceledMatcher.group(2);
                   String amountStr = parceledMatcher.group(3).replace(".", "").replace(",", ".");
                   
                   logger.debug("Parceled pattern matched: desc={}, installment={}, amount={}", 
                       description, installmentStr, amountStr);
                   
                   try {
                       BigDecimal amount = new BigDecimal(amountStr);
                       // Parse installment info (e.g., "02/04" -> current=2, total=4)
                       String[] parts = installmentStr.split("/");
                       int currentInstallment = Integer.parseInt(parts[0]);
                       int totalInstallments = Integer.parseInt(parts[1]);
                       
                       // Use today's date for parceled items since they don't have specific dates
                       LocalDate date = LocalDate.now();
                       
                       items.add(new ParsedInvoiceItem(
                           description, amount, date, null,
                           currentInstallment, totalInstallments, 0.9));
                       logger.debug(
                           "Added parceled item: {} - R$ {} (parcela {}/{})",
                           description, amount, currentInstallment, totalInstallments);
                       processedLines.add(
                           "PARCELED: " + line + " -> R$ " + amount
                               + " (parcela " + currentInstallment + "/" + totalInstallments + ")");
                   } catch (Exception e) {
                       logger.warn("Erro ao processar item parcelado: {}", line, e);
                       ignoredLines.add("ERROR (parceled): " + line + " - " + e.getMessage());
                   }
                   continue;
               }

               // If not a parceled transaction, try the normal pattern
               Matcher matcher = SANTANDER_ITEM_PATTERN.matcher(line);
               if (matcher.find()) {
                   logger.debug("Normal pattern matched!");
                   String dateStr = matcher.group(1);
                   String description = matcher.group(2).trim();
                   String amountStr = matcher.group(3).replace(".", "").replace(",", ".");
                   
                   logger.debug("Normal pattern matched: date={}, desc={}, amount={}", 
                       dateStr, description, amountStr);
                   
                   try {
                       BigDecimal amount = new BigDecimal(amountStr);
                       LocalDate date = parseDate(dateStr);
                       
                       items.add(new ParsedInvoiceItem(description, amount, date, null, 1, 1, 0.9));
                       logger.debug("Added normal item: {} - R$ {}", description, amount);
                       processedLines.add("NORMAL: " + line + " -> R$ " + amount);
                   } catch (Exception e) {
                       logger.warn("Erro ao processar item normal: {}", line, e);
                       ignoredLines.add("ERROR (normal): " + line + " - " + e.getMessage());
                   }
               } else {
                   logger.warn("No pattern matched for line: '{}'", line);
                   ignoredLines.add("NO PATTERN: " + line);
               }
        }
        
        // Log summary for debugging
        logger.info("Extracted {} items from PDF (Santander parser)", items.size());
        logger.info("Processed {} lines, ignored {} lines", processedLines.size(), ignoredLines.size());
        
        if (!ignoredLines.isEmpty()) {
            logger.warn("=== IGNORED LINES ===");
            for (String ignored : ignoredLines) {
                logger.warn(ignored);
            }
            logger.warn("=====================");
        }
        
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