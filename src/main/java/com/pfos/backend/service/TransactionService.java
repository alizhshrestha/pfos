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

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.pfos.backend.exception.InvalidRequestException;

import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Validated
public class TransactionService {

    private static final String INCOME_CATEGORY_ID = "cat_income";
    private static final String CREDIT_TYPE_ID = "type_credit";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final MongoTemplate mongoTemplate;


    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            TransactionTypeRepository transactionTypeRepository,
            PaymentMethodRepository paymentMethodRepository, MongoTemplate mongoTemplate) {

        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.mongoTemplate = mongoTemplate;
    }

    private Query activeTransactionQuery(String id){
        return Query.query(
                Criteria.where("id").is(id)
                        .and("deletedAt").is(null)
        );
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

    public Page<Transaction> getHistory(
            int page,
            int size,
            String from,
            String to,
            String categoryId,
            String transactionTypeId,
            String paymentMethodId,
            String search) {

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

        LocalDate fromDate = parseFilterDate(from, "from");
        LocalDate toDate = parseFilterDate(to, "to");

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new BusinessValidationException(
                    "from",
                    "From date must be on or before to date."
            );
        }

        String searchText = normalizeFilter(search);

        if (searchText != null && searchText.length() > 500) {
            throw new BusinessValidationException(
                    "search",
                    "Search must not exceed 500 characters."
            );
        }

        // Every history query excludes soft-deleted transactions.
        Query query = Query.query(
                Criteria.where("deletedAt").is(null)
        );

        if (fromDate != null || toDate != null) {
            Criteria dateCriteria = Criteria.where("transactionDate");

            if (fromDate != null) {
                dateCriteria.gte(fromDate.toString());
            }

            if (toDate != null) {
                dateCriteria.lte(toDate.toString());
            }

            query.addCriteria(dateCriteria);
        }

        addExactFilter(query, "categoryId", categoryId);
        addExactFilter(query, "transactionTypeId", transactionTypeId);
        addExactFilter(query, "paymentMethodId", paymentMethodId);

        if (searchText != null) {
            query.addCriteria(
                    Criteria.where("description")
                            .regex(Pattern.quote(searchText), "i")
            );
        }

        // Count before adding pagination.
        long total = mongoTemplate.count(query, Transaction.class);

        Sort sort = Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        PageRequest pageable = PageRequest.of(page, size, sort);

        // Copy the criteria into a separate query for the requested page.
        Query pageQuery = Query.of(query).with(pageable);

        List<Transaction> transactions = mongoTemplate.find(
                pageQuery,
                Transaction.class
        );

        return new PageImpl<>(transactions, pageable, total);
    }

    public Transaction update(
            String id,
            @NotNull @Valid CreateTransactionRequest request) {

        Transaction transaction = findRequired(id);

        // Keep the existing validation and editable-field mapping.
        applyEditableFields(transaction, request);

        Update changes = new Update()
                .set("transactionDate", transaction.getTransactionDate())
                .set("description", transaction.getDescription())
                .set("categoryId", transaction.getCategoryId())
                .set("transactionTypeId", transaction.getTransactionTypeId())
                .set("paymentMethodId", transaction.getPaymentMethodId())
                .set("amount", transaction.getAmount())
                .set("notes", transaction.getNotes())
                .set("updatedAt", Instant.now());

        Transaction updated = mongoTemplate.findAndModify(
                activeTransactionQuery(id),
                changes,
                FindAndModifyOptions.options()
                        .returnNew(true)
                        .upsert(false),
                Transaction.class
        );

        if (updated == null) {
            throw new TransactionNotFoundException();
        }

        return updated;
    }

    public void delete(String id) {
        Instant now = Instant.now();

        Update changes = new Update()
                .set("deletedAt", now)
                .set("updatedAt", now);

        var result = mongoTemplate.updateFirst(
                activeTransactionQuery(id),
                changes,
                Transaction.class
        );

        if (result.getMatchedCount() == 0) {
            throw new TransactionNotFoundException();
        }
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


    private LocalDate parseFilterDate(String value, String parameterName) {
        if (value == null) {
            return null;
        }

        // Reject blank values, surrounding spaces, and incorrect formats.
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new InvalidRequestException(
                    parameterName + " must be a valid date in YYYY-MM-DD format."
            );
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new InvalidRequestException(
                    parameterName + " must be a valid date in YYYY-MM-DD format."
            );
        }
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        return normalized.isEmpty() ? null : normalized;
    }

    private void addExactFilter(Query query, String field, String value) {
        String normalized = normalizeFilter(value);

        if (normalized != null) {
            query.addCriteria(
                    Criteria.where(field).is(normalized)
            );
        }
    }

}