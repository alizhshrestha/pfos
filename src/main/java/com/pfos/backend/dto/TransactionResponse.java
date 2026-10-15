package com.pfos.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        @JsonProperty("_id") String id,
        LocalDate transactionDate,
        String description,
        String categoryId,
        String transactionTypeId,
        String paymentMethodId,
        BigDecimal amount,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                LocalDate.parse(transaction.getTransactionDate()),
                transaction.getDescription(),
                transaction.getCategoryId(),
                transaction.getTransactionTypeId(),
                transaction.getPaymentMethodId(),
                transaction.getAmount(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}