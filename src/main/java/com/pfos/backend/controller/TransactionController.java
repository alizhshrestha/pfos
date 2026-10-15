package com.pfos.backend.controller;

import com.pfos.backend.dto.TransactionResponse;
import com.pfos.backend.dto.request.CreateTransactionRequest;
import com.pfos.backend.dto.response.ApiResponse;
import com.pfos.backend.service.TransactionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody CreateTransactionRequest request) {

        TransactionResponse response = TransactionResponse.from(
                transactionService.create(request)
        );

        URI location = URI.create("/api/transactions/" + response.id());

        return ResponseEntity.created(location)
                .body(ApiResponse.ok(response));
    }
}