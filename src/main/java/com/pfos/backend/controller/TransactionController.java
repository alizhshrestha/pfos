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

import com.pfos.backend.dto.response.DeleteTransactionResponse;
import com.pfos.backend.dto.response.PageResponse;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

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


    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getById(
            @PathVariable("id") String id) {

        TransactionResponse response = TransactionResponse.from(
                transactionService.getById(id)
        );

        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> getHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "transactionTypeId", required = false)
            String transactionTypeId,
            @RequestParam(name = "paymentMethodId", required = false)
            String paymentMethodId,
            @RequestParam(name = "search", required = false) String search) {

        Page<TransactionResponse> result = transactionService
                .getHistory(
                        page,
                        size,
                        from,
                        to,
                        categoryId,
                        transactionTypeId,
                        paymentMethodId,
                        search
                )
                .map(TransactionResponse::from);

        PageResponse<TransactionResponse> response = new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );

        return ApiResponse.ok(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionResponse> update(
            @PathVariable("id") String id,
            @Valid @RequestBody CreateTransactionRequest request) {

        TransactionResponse response = TransactionResponse.from(
                transactionService.update(id, request)
        );

        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<DeleteTransactionResponse> delete(
            @PathVariable("id") String id) {

        transactionService.delete(id);

        return ApiResponse.ok(
                new DeleteTransactionResponse(id, true)
        );
    }



}