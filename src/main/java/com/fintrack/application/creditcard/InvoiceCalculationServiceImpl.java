package com.fintrack.application.creditcard;

import com.fintrack.domain.contact.TrustedContact;
import com.fintrack.domain.creditcard.InvoiceCalculationService;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.ParticipantShare;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of InvoiceCalculationService.
 * Provides business logic for calculating invoice amounts and user shares.
 */
@Service
@Transactional
public class InvoiceCalculationServiceImpl implements InvoiceCalculationService {

    /** The expense sharing service. */
    private final ExpenseSharingServiceImpl expenseSharingService;

    /**
     * Constructs a new InvoiceCalculationServiceImpl.
     *
     * @param expenseSharingService the expense sharing service. Must not be null.
     */
    public InvoiceCalculationServiceImpl(final ExpenseSharingServiceImpl expenseSharingService) {
        this.expenseSharingService = expenseSharingService;
    }

    @Override
    public BigDecimal calculateUserShare(Invoice invoice, User user) {
        BigDecimal userShare = BigDecimal.ZERO;
        
        // Calculate user's share for each item in the invoice
        for (InvoiceItem item : invoice.getItems()) {
            userShare = userShare.add(calculateUserShareForItem(item, user));
        }
        
        return userShare;
    }

    @Override
    public BigDecimal calculateTotalForUser(User user, YearMonth month) {
        List<ItemShare> shares = expenseSharingService.getSharesForUser(user, month);
        
        return shares.stream()
                .map(ItemShare::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<User, BigDecimal> calculateSharesForItem(InvoiceItem item) {
        Map<User, BigDecimal> shares = new HashMap<>();

        for (ItemShare share : item.getShares()) {
            if (share.getUser() != null) {
                shares.put(share.getUser(), share.getAmount());
            }
        }

        return shares;
    }

    @Override
    public BigDecimal calculateTotalSharedAmount(Invoice invoice) {
        return invoice.getItems().stream()
                .map(InvoiceItem::getSharedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateUnsharedAmount(Invoice invoice) {
        return invoice.getItems().stream()
                .map(InvoiceItem::getUnsharedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateSharedPercentage(Invoice invoice) {
        BigDecimal totalAmount = invoice.getTotalAmount();
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sharedAmount = calculateTotalSharedAmount(invoice);
        return sharedAmount.divide(totalAmount, 4, RoundingMode.HALF_UP);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Groups shares by participant email so that a person who appears as both
     * a system User and a TrustedContact is counted once. Only trusted contacts
     * owned by {@code owner} are included; contacts owned by other users are
     * excluded for security.</p>
     */
    @Override
    public List<ParticipantShare> calculateOtherParticipantShares(
            final Invoice invoice, final User owner) {

        Map<String, BigDecimal> totalsByEmail = new HashMap<>();
        Map<String, String> nameByEmail = new HashMap<>();

        for (InvoiceItem item : invoice.getItems()) {
            for (ItemShare share : item.getShares()) {
                String email = null;
                String name = null;

                if (share.isContactShare()) {
                    TrustedContact contact = share.getTrustedContact();
                    if (!contact.getOwner().getId().equals(owner.getId())) {
                        continue;
                    }
                    email = contact.getEmail();
                    name = contact.getName();
                } else if (share.getUser() != null
                        && !share.getUser().getId().equals(owner.getId())) {
                    User u = share.getUser();
                    email = u.getEmail().getEmail();
                    name = u.getName();
                }

                if (email != null) {
                    totalsByEmail.merge(email, share.getAmount(), BigDecimal::add);
                    nameByEmail.putIfAbsent(email, name);
                }
            }

            addCardAssigneeUnsharedAmount(item, owner, totalsByEmail, nameByEmail);
        }

        List<ParticipantShare> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totalsByEmail.entrySet()) {
            String email = entry.getKey();
            result.add(new ParticipantShare(
                    nameByEmail.get(email), email, entry.getValue()));
        }
        return result;
    }

    @Override
    public BigDecimal calculateUserShareForItem(InvoiceItem item, User user) {
        for (ItemShare share : item.getShares()) {
            if (user.equals(share.getUser())) {
                return share.getAmount();
            }
        }

        BigDecimal unsharedAmount = item.getUnsharedAmount();
        CreditCard card = item.getInvoice().getCreditCard();
        User assignedUser = card.getAssignedUser();

        // An assigned system user is responsible for every portion that was not
        // explicitly divided. A trusted contact is accounted for in the participant
        // summary, so neither the owner nor another system user receives that amount.
        if (assignedUser != null) {
            return assignedUser.equals(user) ? unsharedAmount : BigDecimal.ZERO;
        }
        if (hasValidAssignedContact(card)) {
            return BigDecimal.ZERO;
        }

        return card.getOwner().equals(user) ? unsharedAmount : BigDecimal.ZERO;
    }

    /**
     * Adds the portion not explicitly shared to the card assignee. The assignee is
     * resolved from the persisted card configuration rather than from invoice text.
     */
    private void addCardAssigneeUnsharedAmount(
            final InvoiceItem item,
            final User owner,
            final Map<String, BigDecimal> totalsByEmail,
            final Map<String, String> nameByEmail) {
        BigDecimal unsharedAmount = item.getUnsharedAmount();
        if (unsharedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        CreditCard card = item.getInvoice().getCreditCard();
        TrustedContact contact = card.getAssignedContact();
        if (hasValidAssignedContact(card) && contact.getOwner().equals(owner)) {
            addParticipantAmount(totalsByEmail, nameByEmail,
                contact.getEmail(), contact.getName(), unsharedAmount);
            return;
        }

        User assignedUser = card.getAssignedUser();
        if (assignedUser != null && !assignedUser.getId().equals(owner.getId())) {
            addParticipantAmount(totalsByEmail, nameByEmail,
                assignedUser.getEmail().getEmail(), assignedUser.getName(), unsharedAmount);
        }
    }

    private void addParticipantAmount(
            final Map<String, BigDecimal> totalsByEmail,
            final Map<String, String> nameByEmail,
            final String email,
            final String name,
            final BigDecimal amount) {
        totalsByEmail.merge(email, amount, BigDecimal::add);
        nameByEmail.putIfAbsent(email, name);
    }

    private boolean hasValidAssignedContact(final CreditCard card) {
        TrustedContact contact = card.getAssignedContact();
        return contact != null && contact.getOwner().equals(card.getOwner());
    }
}
