package com.pfos.backend.service;

import com.pfos.backend.dto.Category;
import com.pfos.backend.dto.PaymentMethod;
import com.pfos.backend.dto.Transaction;
import com.pfos.backend.dto.TransactionType;
import com.pfos.backend.dto.request.CreateTransactionRequest;
import com.pfos.backend.exception.BusinessValidationException;
import com.pfos.backend.repo.CategoryRepository;
import com.pfos.backend.repo.PaymentMethodRepository;
import com.pfos.backend.repo.TransactionRepository;
import com.pfos.backend.repo.TransactionTypeRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.RoundingMode;
import java.time.Instant;

@Service
@Validated
public class TransactionService {

    private static final String INCOME_CATEGORY_ID = "cat_income";
    private static final String CREDIT_TYPE_ID = "type_credit";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            TransactionTypeRepository transactionTypeRepository,
            PaymentMethodRepository paymentMethodRepository) {

        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public Transaction create(@NotNull @Valid CreateTransactionRequest request) {

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessValidationException(
                        "categoryId",
                        "Selected category does not exist."
                ));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new BusinessValidationException(
                    "categoryId",
                    "Selected category is inactive."
            );
        }

        TransactionType transactionType = transactionTypeRepository
                .findById(request.transactionTypeId())
                .orElseThrow(() -> new BusinessValidationException(
                        "transactionTypeId",
                        "Selected transaction type does not exist."
                ));

        PaymentMethod paymentMethod = paymentMethodRepository
                .findById(request.paymentMethodId())
                .orElseThrow(() -> new BusinessValidationException(
                        "paymentMethodId",
                        "Selected payment method does not exist."
                ));

        if (!Boolean.TRUE.equals(paymentMethod.getIsActive())) {
            throw new BusinessValidationException(
                    "paymentMethodId",
                    "Selected payment method is inactive."
            );
        }

        //TODO: replace this with actual income category id and credit type id
        if (INCOME_CATEGORY_ID.equals(category.getId())
                && !CREDIT_TYPE_ID.equals(transactionType.getId())) {

            throw new BusinessValidationException(
                    "transactionTypeId",
                    "Income transactions must use Credit."
            );
        }

        Instant now = Instant.now();

        Transaction transaction = new Transaction();
        transaction.setTransactionDate(request.transactionDate().toString());
        transaction.setDescription(request.description());
        transaction.setCategoryId(category.getId());
        transaction.setTransactionTypeId(transactionType.getId());
        transaction.setPaymentMethodId(paymentMethod.getId());

        transaction.setAmount(
                request.amount().setScale(2, RoundingMode.UNNECESSARY)
        );

        transaction.setNotes(request.notes());
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        return transactionRepository.insert(transaction);
    }
}