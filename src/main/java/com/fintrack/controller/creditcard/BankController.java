package com.fintrack.controller.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * REST controller for managing banks.
 * Provides endpoints for bank operations.
 */
@RestController
@RequestMapping("/api/banks")
public class BankController {

    private final BankJpaRepository bankRepository;

    public BankController(BankJpaRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    /**
     * Creates a new bank.
     *
     * @param request the bank creation request.
     * @return a response with the created bank information.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBank(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String name = request.get("name");

        if (code == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code and name are required"));
        }

        // Check if bank already exists
        if (bankRepository.existsByCode(code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bank with this code already exists"));
        }

        Bank bank = Bank.of(code, name);
        Bank savedBank = bankRepository.save(bank);

        return ResponseEntity.ok(Map.of(
            "message", "Bank created successfully",
            "id", savedBank.getId(),
            "code", savedBank.getCode(),
            "name", savedBank.getName()
        ));
    }

    /**
     * Gets all banks.
     *
     * @return a response with all banks.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBanks() {
        List<Bank> banks = bankRepository.findAll();

        List<Map<String, Object>> bankDtos = new ArrayList<>();
        for (Bank bank : banks) {
            bankDtos.add(Map.of(
                "id", bank.getId(),
                "code", bank.getCode(),
                "name", bank.getName()
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Banks retrieved successfully",
            "banks", bankDtos,
            "count", bankDtos.size()
        ));
    }

    /**
     * Gets a specific bank by ID.
     *
     * @param id the bank ID.
     * @return a response with the bank information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBank(@PathVariable Long id) {
        return bankRepository.findById(id)
            .map(bank -> ResponseEntity.ok(Map.of(
                "message", "Bank retrieved successfully",
                "bank", Map.of(
                    "id", bank.getId(),
                    "code", bank.getCode(),
                    "name", bank.getName()
                )
            )))
            .orElse(ResponseEntity.notFound().build());
    }
} 