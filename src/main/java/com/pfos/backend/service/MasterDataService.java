package com.pfos.backend.service;

import com.pfos.backend.dto.response.CategoryResponse;
import com.pfos.backend.dto.response.PaymentMethodResponse;
import com.pfos.backend.dto.response.TransactionTypeResponse;
import com.pfos.backend.repo.CategoryRepository;
import com.pfos.backend.repo.PaymentMethodRepository;
import com.pfos.backend.repo.TransactionTypeRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MasterDataService {

    private final CategoryRepository categoryRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public MasterDataService(
            CategoryRepository categoryRepository,
            TransactionTypeRepository transactionTypeRepository,
            PaymentMethodRepository paymentMethodRepository) {

        this.categoryRepository = categoryRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getSortOrder(),
                        category.getIsActive()
                ))
                .toList();
    }

    public List<TransactionTypeResponse> getTransactionTypes() {
        return transactionTypeRepository.findAll(Sort.by("name"))
                .stream()
                .map(type -> new TransactionTypeResponse(
                        type.getId(),
                        type.getName()
                ))
                .toList();
    }

    public List<PaymentMethodResponse> getPaymentMethods() {
        return paymentMethodRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(method -> new PaymentMethodResponse(
                        method.getId(),
                        method.getName(),
                        method.getIsActive()
                ))
                .toList();
    }
}