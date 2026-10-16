package com.pfos.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    private String id;

    // Always stored as YYYY-MM-DD.
    private String transactionDate;

    private String description;

    private String categoryId;
    private String transactionTypeId;
    private String paymentMethodId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private String notes;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

}