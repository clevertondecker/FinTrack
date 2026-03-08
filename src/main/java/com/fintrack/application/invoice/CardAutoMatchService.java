package com.fintrack.application.invoice;

import com.fintrack.domain.creditcard.CardType;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.DetectedCardMapping;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedCardSection;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedInvoiceItem;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Matches detected card sections from a parsed PDF against
 * registered credit cards using last-four-digits lookup and
 * parent/child hierarchy disambiguation.
 */
@Service
public class CardAutoMatchService {

    private static final Logger logger = LoggerFactory.getLogger(CardAutoMatchService.class);

    private final CreditCardJpaRepository creditCardRepository;

    public CardAutoMatchService(CreditCardJpaRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    /**
     * Builds auto-match mappings for every card section in parsed data.
     */
    public List<DetectedCardMapping> buildDetectedCardMappings(
            ParsedInvoiceData parsedData, User user) {

        List<ParsedCardSection> sections = parsedData.cardSections();

        if (sections == null || sections.isEmpty()) {
            if (parsedData.cardNumber() != null) {
                return List.of(matchSingleCard(parsedData, user));
            }
            if (parsedData.items() != null && !parsedData.items().isEmpty()) {
                return List.of(new DetectedCardMapping(
                        "????", "Unknown Card",
                        null, null, false, false,
                        List.of(), parsedData.items(), parsedData.totalAmount()));
            }
            return List.of();
        }

        logger.info("Auto-matching {} card sections for user {} (id={})",
                sections.size(), user.getEmail(), user.getId());

        List<DetectedCardMapping> mappings = new ArrayList<>();
        boolean isFirst = true;
        for (ParsedCardSection section : sections) {
            List<CreditCard> candidates = creditCardRepository
                    .findByOwnerAndLastFourDigitsAndActiveTrue(user, section.cardLastFourDigits());
            logger.info("Card section '{}' (****{}): found {} candidates in DB",
                    section.cardDisplayName(), section.cardLastFourDigits(), candidates.size());
            candidates.forEach(c -> logger.info("  Candidate: id={}, name='{}', type={}, parentId={}",
                    c.getId(), c.getName(), c.getCardType(),
                    c.getParentCard() != null ? c.getParentCard().getId() : "null"));
            mappings.add(resolveMatch(
                    section.cardLastFourDigits(), section.cardDisplayName(),
                    section.items(), section.subtotal(), candidates, isFirst));
            isFirst = false;
        }
        return mappings;
    }

    /**
     * Groups items by detected card last-four-digits.
     */
    public Map<String, List<ParsedInvoiceItem>> buildItemsByCardMap(
            ParsedInvoiceData parsedData) {

        List<ParsedCardSection> sections = parsedData.cardSections();
        if (sections != null && !sections.isEmpty()) {
            return sections.stream()
                    .collect(Collectors.toMap(
                            ParsedCardSection::cardLastFourDigits,
                            ParsedCardSection::items,
                            (a, b) -> {
                                List<ParsedInvoiceItem> merged = new ArrayList<>(a);
                                merged.addAll(b);
                                return merged;
                            }));
        }
        if (parsedData.cardNumber() != null && parsedData.items() != null) {
            return Map.of(parsedData.cardNumber(), parsedData.items());
        }
        return Map.of();
    }

    private DetectedCardMapping matchSingleCard(ParsedInvoiceData data, User user) {
        List<CreditCard> candidates = creditCardRepository
                .findByOwnerAndLastFourDigitsAndActiveTrue(user, data.cardNumber());
        return resolveMatch(
                data.cardNumber(), data.creditCardName(),
                data.items(), data.totalAmount(), candidates, true);
    }

    private DetectedCardMapping resolveMatch(
            String lastFour, String displayName,
            List<ParsedInvoiceItem> items, BigDecimal subtotal,
            List<CreditCard> candidates, boolean isFirstSection) {

        List<Long> ids = candidates.stream().map(CreditCard::getId).toList();

        if (candidates.size() == 1) {
            CreditCard c = candidates.get(0);
            return new DetectedCardMapping(lastFour, displayName,
                    c.getId(), c.getName(), true, false, ids, items, subtotal);
        }

        if (candidates.size() > 1) {
            CreditCard picked = disambiguate(candidates, isFirstSection);
            if (picked != null) {
                return new DetectedCardMapping(lastFour, displayName,
                        picked.getId(), picked.getName(), true, false, ids, items, subtotal);
            }
            return new DetectedCardMapping(lastFour, displayName,
                    null, null, false, true, ids, items, subtotal);
        }

        return new DetectedCardMapping(lastFour, displayName,
                null, null, false, false, ids, items, subtotal);
    }

    /**
     * Disambiguates cards with the same last 4 digits using card type and
     * parent-child hierarchy. The first section in a PDF typically belongs
     * to the titular (PHYSICAL) card.
     */
    private CreditCard disambiguate(List<CreditCard> candidates, boolean isFirst) {
        List<CreditCard> physicals = candidates.stream()
                .filter(c -> c.getCardType() == CardType.PHYSICAL).toList();

        if (isFirst && physicals.size() == 1) {
            return physicals.get(0);
        }

        List<CreditCard> nonPhysicals = candidates.stream()
                .filter(c -> c.getCardType() != CardType.PHYSICAL).toList();

        if (!isFirst && nonPhysicals.size() == 1) {
            return nonPhysicals.get(0);
        }

        List<CreditCard> parents = candidates.stream()
                .filter(c -> c.getParentCard() == null).toList();
        List<CreditCard> children = candidates.stream()
                .filter(c -> c.getParentCard() != null).toList();

        if (isFirst && parents.size() == 1) {
            return parents.get(0);
        }
        if (!isFirst && children.size() == 1) {
            return children.get(0);
        }
        return null;
    }
}
