package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.BankService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.dto.creditcard.BankCreateRequest;
import com.fintrack.dto.creditcard.BankCreateResponse;
import com.fintrack.dto.creditcard.BankListResponse;
import com.fintrack.dto.creditcard.BankDetailResponse;
import com.fintrack.dto.creditcard.BankResponse;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing banks.
 * Provides endpoints for bank operations.
 */
@RestController
@RequestMapping("/api/banks")
public class BankController {

    /** The bank service. */
    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    /**
     * Creates a new bank.
     *
     * @param request the bank creation request.
     * @return a response with the created bank information.
     */
    @PostMapping
    public ResponseEntity<BankCreateResponse> createBank(@Valid @RequestBody BankCreateRequest request) {
        Bank savedBank = bankService.create(request.code(), request.name());

        BankCreateResponse response = new BankCreateResponse(
            "Bank created successfully",
            savedBank.getId(),
            savedBank.getCode(),
            savedBank.getName()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all banks.
     *
     * @return a response with all banks.
     */
    @GetMapping
    public ResponseEntity<BankListResponse> getAllBanks() {
        List<Bank> banks = bankService.findAll();

        List<BankResponse> bankResponses = banks.stream()
            .map(bank -> new BankResponse(bank.getId(), bank.getCode(), bank.getName()))
            .toList();

        BankListResponse response = new BankListResponse(
            "Banks retrieved successfully",
            bankResponses,
            bankResponses.size()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets a specific bank by ID.
     *
     * @param id the bank ID.
     * @return a response with the bank information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BankDetailResponse> getBank(@PathVariable Long id) {
        return bankService.findById(id)
            .map(bank -> {
                BankResponse bankResponse = new BankResponse(bank.getId(), bank.getCode(), bank.getName());
                BankDetailResponse response = new BankDetailResponse("Bank retrieved successfully", bankResponse);
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
} 