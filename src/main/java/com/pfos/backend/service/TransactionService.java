package com.pfos.backend.service;

import com.pfos.backend.dto.Category;
import com.pfos.backend.dto.PaymentMethod;
import com.pfos.backend.dto.Transaction;
import com.pfos.backend.dto.TransactionType;
import com.pfos.backend.dto.request.CreateTransactionRequest;
import com.pfos.backend.exception.BusinessValidationException;
import com.pfos.backend.exception.TransactionNotFoundException;
import com.pfos.backend.repo.CategoryRepository;
import com.pfos.backend.repo.PaymentMethodRepository;
import com.pfos.backend.repo.TransactionRepository;
import com.pfos.backend.repo.TransactionTypeRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

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

    public Transaction create(
            @NotNull @Valid CreateTransactionRequest request) {

        Transaction transaction = new Transaction();
        applyEditableFields(transaction, request);

        Instant now = Instant.now();
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        return transactionRepository.insert(transaction);
    }

    public Transaction getById(String id) {
        return findRequired(id);
    }

    public Page<Transaction> getHistory(int page, int size){
        if (page < 0) {
            throw new BusinessValidationException(
                    "page",
                    "Page must be zero or greater."
            );
        }

        if (size < 1 || size > 100) {
            throw new BusinessValidationException(
                    "size",
                    "Size must be between 1 and 100."
            );
        }

        Sort sort = Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        return transactionRepository.findByDeletedAtIsNull(
                PageRequest.of(page, size, sort)
        );
    }

    public Transaction update(
            String id,
            @NotNull @Valid CreateTransactionRequest request) {

        Transaction transaction = findRequired(id);

        applyEditableFields(transaction, request);
        transaction.setUpdatedAt(Instant.now());

        return transactionRepository.save(transaction);
    }

    public void delete(String id) {
        Transaction transaction = findRequired(id);

        Instant now = Instant.now();
        transaction.setDeletedAt(now);
        transaction.setUpdatedAt(now);

        transactionRepository.save(transaction);
    }

    private Transaction findRequired(String id) {
        return transactionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(TransactionNotFoundException::new);
    }

    private void applyEditableFields(
            Transaction transaction,
            CreateTransactionRequest request) {

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessValidationException(
                        "categoryId",
                        "Selected category does not exist."
                ));

        boolean retainingCategory = Objects.equals(
                transaction.getCategoryId(),
                category.getId()
        );

        if (!Boolean.TRUE.equals(category.getIsActive())
                && !retainingCategory) {
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

        boolean retainingPaymentMethod = Objects.equals(
                transaction.getPaymentMethodId(),
                paymentMethod.getId()
        );

        if (!Boolean.TRUE.equals(paymentMethod.getIsActive())
                && !retainingPaymentMethod) {
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

        transaction.setTransactionDate(request.transactionDate().toString());
        transaction.setDescription(request.description());
        transaction.setCategoryId(category.getId());
        transaction.setTransactionTypeId(transactionType.getId());
        transaction.setPaymentMethodId(paymentMethod.getId());

        transaction.setAmount(
                request.amount().setScale(2, RoundingMode.UNNECESSARY)
        );

        transaction.setNotes(request.notes());
    }

}