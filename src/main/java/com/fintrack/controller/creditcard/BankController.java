package com.fintrack.controller.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.infrastructure.persistence.creditcard.BankJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

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

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bank created successfully");
        response.put("id", savedBank.getId());
        response.put("code", savedBank.getCode());
        response.put("name", savedBank.getName());

        return ResponseEntity.ok(response);
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
            Map<String, Object> bankDto = new HashMap<>();
            bankDto.put("id", bank.getId());
            bankDto.put("code", bank.getCode());
            bankDto.put("name", bank.getName());
            bankDtos.add(bankDto);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Banks retrieved successfully");
        response.put("banks", bankDtos);
        response.put("count", bankDtos.size());

        return ResponseEntity.ok(response);
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
            .map(bank -> {
                Map<String, Object> bankInfo = new HashMap<>();
                bankInfo.put("id", bank.getId());
                bankInfo.put("code", bank.getCode());
                bankInfo.put("name", bank.getName());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Bank retrieved successfully");
                response.put("bank", bankInfo);

                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
} 