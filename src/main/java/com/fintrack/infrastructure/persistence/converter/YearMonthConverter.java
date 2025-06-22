package com.fintrack.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * JPA converter for YearMonth type.
 * Converts YearMonth to String and vice versa for database storage.
 */
@Converter
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public String convertToDatabaseColumn(YearMonth yearMonth) {
        if (yearMonth == null) {
            // Use current month as fallback instead of null
            return YearMonth.now().format(FORMATTER);
        }
        return yearMonth.format(FORMATTER);
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            // Use current month as fallback instead of null
            System.err.println("Warning: Null or empty YearMonth in database, using current month as fallback");
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(dbData.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            // Log the error and use current month as fallback
            System.err.println("Warning: Invalid YearMonth format in database: " + dbData + ". Error: " + e.getMessage() + ". Using current month as fallback.");
            return YearMonth.now();
        } catch (Exception e) {
            // Log any other unexpected errors and use current month as fallback
            System.err.println("Warning: Unexpected error parsing YearMonth from database: " + dbData + ". Error: " + e.getMessage() + ". Using current month as fallback.");
            return YearMonth.now();
        }
    }
} 