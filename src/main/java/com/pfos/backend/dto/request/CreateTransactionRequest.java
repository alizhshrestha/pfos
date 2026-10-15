package com.pfos.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

        @NotNull(message = "Transaction date is required.")
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "uuuu-MM-dd",
                lenient = OptBoolean.FALSE
        )
        LocalDate transactionDate,

        @NotBlank(message = "Description is required.")
        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String description,

        @NotBlank(message = "Category ID is required.")
        String categoryId,

        @NotBlank(message = "Transaction type ID is required.")
        String transactionTypeId,

        @NotBlank(message = "Payment method ID is required.")
        String paymentMethodId,

        @NotNull(message = "Amount is required.")
        @DecimalMin(
                value = "0",
                inclusive = false,
                message = "Amount must be greater than zero."
        )
        @Digits(
                integer = 12,
                fraction = 2,
                message = "Amount must have at most 12 integer digits "
                        + "and 2 decimal places."
        )
        BigDecimal amount,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters.")
        String notes

) {
    public CreateTransactionRequest {
        if (description != null) {
            description = description.strip();
        }

        if (notes != null) {
            notes = notes.strip();

            if (notes.isEmpty()) {
                notes = null;
            }
        }
    }
}