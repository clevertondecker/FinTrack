package com.fintrack.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA converter for YearMonth type.
 * Converts YearMonth to String and vice versa for database storage.
 */
@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    private static final Logger logger = LoggerFactory.getLogger(YearMonthConverter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public String convertToDatabaseColumn(YearMonth yearMonth) {
        logger.info("YearMonthConverter.convertToDatabaseColumn called with: {}", yearMonth);
        logger.info("YearMonthConverter.convertToDatabaseColumn - yearMonth type: {}", 
                   yearMonth != null ? yearMonth.getClass().getName() : "NULL");
        
        if (yearMonth == null) {
            logger.warn("YearMonth is null, using current month as fallback");
            String fallback = YearMonth.now().format(FORMATTER);
            logger.info("Fallback value: {}", fallback);
            return fallback;
        }
        
        try {
            String result = yearMonth.format(FORMATTER);
            logger.info("Converted YearMonth to database column: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error converting YearMonth to database column: {}", e.getMessage(), e);
            String fallback = YearMonth.now().format(FORMATTER);
            logger.info("Using fallback value: {}", fallback);
            return fallback;
        }
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        logger.info("YearMonthConverter.convertToEntityAttribute called with: {}", dbData);
        
        if (dbData == null || dbData.trim().isEmpty()) {
            logger.warn("Null or empty YearMonth in database, using current month as fallback");
            return YearMonth.now();
        }
        
        try {
            YearMonth result = YearMonth.parse(dbData.trim(), FORMATTER);
            logger.info("Converted database column to YearMonth: {}", result);
            return result;
        } catch (DateTimeParseException e) {
            logger.warn("Invalid YearMonth format in database: {}. Error: {}. Using current month as fallback.", dbData, e.getMessage());
            return YearMonth.now();
        } catch (Exception e) {
            logger.warn("Unexpected error parsing YearMonth from database: {}. Error: {}. Using current month as fallback.", dbData, e.getMessage());
            return YearMonth.now();
        }
    }
} 