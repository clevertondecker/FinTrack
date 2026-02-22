package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for bank operations.
 * Keeps controllers independent of infrastructure (fintrack-architecture).
 */
@Service
public class BankService {

    private static final String MSG_BANK_CODE_EXISTS = "Bank with this code already exists";

    private final BankJpaRepository bankRepository;

    public BankService(BankJpaRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    /**
     * Creates a new bank if the code is not already in use.
     *
     * @param code the bank code. Cannot be null or blank.
     * @param name the bank name. Cannot be null or blank.
     * @return the saved bank. Never null.
     * @throws IllegalArgumentException if a bank with the same code already exists.
     */
    @Transactional
    public Bank create(String code, String name) {
        if (bankRepository.existsByCode(code)) {
            throw new IllegalArgumentException(MSG_BANK_CODE_EXISTS);
        }
        Bank bank = Bank.of(code, name);
        return bankRepository.save(bank);
    }

    /**
     * Returns all banks.
     *
     * @return list of all banks. Never null.
     */
    @Transactional(readOnly = true)
    public List<Bank> findAll() {
        return bankRepository.findAll();
    }

    /**
     * Finds a bank by ID.
     *
     * @param id the bank ID.
     * @return the bank if found, empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<Bank> findById(Long id) {
        return bankRepository.findById(id);
    }
}
